package br.edu.gamificacaocursos.ranking.application;

import br.edu.gamificacaocursos.ranking.api.RankingItemDTO;
import br.edu.gamificacaocursos.ranking.api.RankingPaginaDTO;
import br.edu.gamificacaocursos.ranking.internal.persistence.LinhaDoRanking;
import br.edu.gamificacaocursos.ranking.internal.persistence.RankingRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private RankingRepository repository;

    private SimpleMeterRegistry metricas;
    private RankingService service;

    @BeforeEach
    void prepara() {
        metricas = new SimpleMeterRegistry();
        service = new RankingService(repository, metricas);
    }

    @Test
    @DisplayName("Numera as posicoes a partir do deslocamento da pagina pedida")
    void numeraPosicoesPelaPagina() {
        PageRequest terceiraPagina = PageRequest.of(2, 10);
        when(repository.buscarRanking(terceiraPagina)).thenReturn(new PageImpl<>(List.of(
                new LinhaDoRanking("Maria", 9, 30),
                new LinhaDoRanking("Joao", 9, 12)), terceiraPagina, 100_000));

        RankingPaginaDTO pagina = service.consultar(2, 10);

        assertEquals(21, pagina.itens().get(0).posicao());
        assertEquals(22, pagina.itens().get(1).posicao());
        assertEquals("Maria", pagina.itens().get(0).aluno());
        assertEquals(100_000, pagina.totalDeAlunos());
        assertEquals(10_000, pagina.totalDePaginas());
        verify(repository, times(1)).buscarRanking(terceiraPagina);
    }

    @Test
    @DisplayName("Cada consulta alimenta a metrica de tempo do ranking")
    void registraOTempoDaConsulta() {
        when(repository.buscarRanking(PageRequest.of(0, 20))).thenReturn(new PageImpl<>(List.of()));

        service.consultar(0, 20);
        service.consultar(0, 20);

        assertEquals(2, metricas.get("gamificacao.ranking.consulta").timer().count());
    }

    @Test
    @DisplayName("Pagina maior que o limite e recusada sem tocar no banco")
    void recusaPaginaGrandeDemais() {
        assertThrows(IllegalArgumentException.class, () -> service.consultar(0, 101));
        assertThrows(IllegalArgumentException.class, () -> service.consultar(0, 0));
        assertThrows(IllegalArgumentException.class, () -> service.consultar(-1, 20));

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("A posicao do aluno e a quantidade de alunos a frente mais um")
    void calculaPosicaoDoAluno() {
        when(repository.buscarPorAluno("Maria")).thenReturn(Optional.of(new LinhaDoRanking("Maria", 5, 7)));
        when(repository.contarAFrente(5, 7, "Maria")).thenReturn(41L);

        RankingItemDTO posicao = service.posicaoDe("Maria");

        assertEquals(42, posicao.posicao());
        assertEquals(5, posicao.moedas());
        verify(repository).contarAFrente(5, 7, "Maria");
    }

    @Test
    @DisplayName("Aluno inexistente gera erro e nao dispara a contagem")
    void alunoInexistenteNaoContaPosicao() {
        when(repository.buscarPorAluno("Fantasma")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.posicaoDe("Fantasma"));

        verify(repository, never()).contarAFrente(anyInt(), anyInt(), anyString());
    }
}
