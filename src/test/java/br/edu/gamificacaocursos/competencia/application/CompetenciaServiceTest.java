package br.edu.gamificacaocursos.competencia.application;

import br.edu.gamificacaocursos.colaborador.api.ColaboradorResponseDTO;
import br.edu.gamificacaocursos.colaborador.application.ColaboradorService;
import br.edu.gamificacaocursos.competencia.api.AvaliacaoResponseDTO;
import br.edu.gamificacaocursos.competencia.domain.AvaliacaoDeCompetencia;
import br.edu.gamificacaocursos.competencia.domain.Classificacao;
import br.edu.gamificacaocursos.competencia.domain.PerfilDeCompetencia;
import br.edu.gamificacaocursos.competencia.internal.persistence.AvaliacaoDeCompetenciaRepository;
import br.edu.gamificacaocursos.shared.web.ServicoExternoIndisponivelException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia.AVANCADO;
import static br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia.INTERMEDIARIO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompetenciaServiceTest {

    private static final PerfilDeCompetencia PERFIL = new PerfilDeCompetencia(24, 6, 8.0, 10, true, false);
    private static final Classificacao INTERMEDIARIA = new Classificacao(INTERMEDIARIO,
            Map.of("INICIANTE", 0.2, "INTERMEDIARIO", 0.7, "AVANCADO", 0.1), "regressao-logistica v1");

    @Mock
    private ColaboradorService colaboradores;

    @Mock
    private ClassificadorDeCompetencia classificador;

    @Mock
    private AvaliacaoDeCompetenciaRepository repository;

    private SimpleMeterRegistry metricas;
    private CompetenciaService service;

    @BeforeEach
    void prepara() {
        metricas = new SimpleMeterRegistry();
        service = new CompetenciaService(colaboradores, classificador, repository, metricas);
    }

    @Test
    @DisplayName("Confere o colaborador, classifica no ML e so depois grava a avaliacao")
    void avaliaNaOrdemCerta() {
        when(colaboradores.buscarPorId(1L)).thenReturn(new ColaboradorResponseDTO(1L, "Ana", "a@e.com", "Dev", "TI"));
        when(classificador.classificar(PERFIL)).thenReturn(INTERMEDIARIA);
        when(repository.save(any(AvaliacaoDeCompetencia.class))).thenAnswer(chamada -> chamada.getArgument(0));

        AvaliacaoResponseDTO avaliacao = service.avaliar(1L, PERFIL);

        assertEquals(INTERMEDIARIO, avaliacao.nivel());
        assertEquals(0.7, avaliacao.confianca());
        assertEquals(0.1, avaliacao.probabilidades().get("AVANCADO"));
        assertEquals(PERFIL, avaliacao.perfil());
        assertEquals(1L, avaliacao.colaboradorId());
        assertEquals("regressao-logistica v1", avaliacao.modelo());
        InOrder ordem = inOrder(colaboradores, classificador, repository);
        ordem.verify(colaboradores).buscarPorId(1L);
        ordem.verify(classificador).classificar(PERFIL);
        ordem.verify(repository).save(any(AvaliacaoDeCompetencia.class));
    }

    @Test
    @DisplayName("Colaborador inexistente nao chega ao ML nem ao banco")
    void colaboradorInexistente() {
        when(colaboradores.buscarPorId(9L)).thenThrow(new IllegalArgumentException("Colaborador nao encontrado: 9"));

        assertThrows(IllegalArgumentException.class, () -> service.avaliar(9L, PERFIL));

        verifyNoInteractions(classificador, repository);
    }

    @Test
    @DisplayName("Com o ML fora do ar nada e gravado e o tempo da tentativa entra na metrica")
    void mlIndisponivel() {
        when(classificador.classificar(PERFIL)).thenThrow(new ServicoExternoIndisponivelException("fora do ar"));

        assertThrows(ServicoExternoIndisponivelException.class, () -> service.avaliar(1L, PERFIL));

        verify(repository, never()).save(any());
        assertEquals(1, metricas.get("gamificacao.competencia.classificacao").timer().count());
    }

    @Test
    @DisplayName("O historico vem do repositorio na ordem da mais recente")
    void historico() {
        Classificacao avancada = new Classificacao(AVANCADO, Map.of("AVANCADO", 0.9), "regressao-logistica v1");
        when(repository.findByColaboradorIdOrderByAvaliadoEmDescIdDesc(1L)).thenReturn(List.of(
                new AvaliacaoDeCompetencia(1L, PERFIL, avancada, LocalDateTime.of(2026, 11, 20, 10, 0)),
                new AvaliacaoDeCompetencia(1L, PERFIL, INTERMEDIARIA, LocalDateTime.of(2026, 11, 1, 10, 0))));

        List<AvaliacaoResponseDTO> historico = service.historico(1L);

        assertEquals(AVANCADO, historico.get(0).nivel());
        assertEquals(0.0, historico.get(0).probabilidades().get("INICIANTE"));
        assertEquals(LocalDateTime.of(2026, 11, 1, 10, 0), historico.get(1).avaliadoEm());
        verify(colaboradores).buscarPorId(1L);
    }
}
