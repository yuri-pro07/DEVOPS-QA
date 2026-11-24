package br.edu.gamificacaocursos.orientacao.application;

import br.edu.gamificacaocursos.competencia.api.AvaliacaoResponseDTO;
import br.edu.gamificacaocursos.competencia.application.CompetenciaService;
import br.edu.gamificacaocursos.competencia.domain.PerfilDeCompetencia;
import br.edu.gamificacaocursos.curso.api.CursoResponseDTO;
import br.edu.gamificacaocursos.curso.application.CursoService;
import br.edu.gamificacaocursos.orientacao.domain.Acompanhamento;
import br.edu.gamificacaocursos.orientacao.domain.EtapaDoPlano;
import br.edu.gamificacaocursos.orientacao.domain.PlanoDeEstudos;
import br.edu.gamificacaocursos.orientacao.domain.RegistroDeAgente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia.INICIANTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgenteDeAcompanhamentoTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 23);

    @Mock
    private CompetenciaService competencias;

    @Mock
    private CursoService cursos;

    private AgenteDeAcompanhamento agente;

    @BeforeEach
    void prepara() {
        agente = new AgenteDeAcompanhamento(competencias, cursos, new PoliticaDeAcessoDosAgentes());
    }

    private static PlanoDeEstudos plano() {
        return new PlanoDeEstudos(1L, INICIANTE, "java", "orientacao", "agente", List.of(1L, 2L),
                LocalDateTime.of(2026, 11, 23, 9, 0));
    }

    private void catalogo() {
        when(cursos.buscarPorId(1L)).thenReturn(new CursoResponseDTO(1L, "Fundamentos de Java", "d", "Dev", INICIANTE, 20));
        when(cursos.buscarPorId(2L)).thenReturn(new CursoResponseDTO(2L, "HTTP e APIs REST", "d", "Dev", INICIANTE, 12));
    }

    private void horasSemanais(int horas) {
        when(competencias.ultimaAvaliacao(1L)).thenReturn(Optional.of(new AvaliacaoResponseDTO(1L, 1L, INICIANTE, 0.9,
                Map.of(), "rl", LocalDateTime.now(), new PerfilDeCompetencia(6, 1, 6.0, horas, false, false))));
    }

    @Test
    @DisplayName("Cria uma etapa por curso com prazo pela carga horaria e pelas horas semanais da ultima avaliacao")
    void criaEtapasPelasHorasDoColaborador() {
        catalogo();
        horasSemanais(5);
        PlanoDeEstudos plano = plano();

        List<EtapaDoPlano> etapas = agente.criarEtapas(plano, INICIO);

        assertEquals(2, etapas.size());
        assertEquals("Fundamentos de Java", etapas.get(0).getTituloDoCurso());
        assertEquals(INICIO.plusWeeks(4), etapas.get(0).getPrazo());
        assertEquals(INICIO.plusWeeks(7), etapas.get(1).getPrazo());
        assertEquals(2, etapas.get(1).getOrdem());
        assertEquals(12, etapas.get(1).getCargaHoraria());
        assertEquals(2L, etapas.get(1).getCursoId());
        assertEquals(List.of("consultarDisponibilidade", "consultarCurso", "consultarCurso", "criarEtapas"),
                plano.getRegistros().stream().map(RegistroDeAgente::getFerramenta).toList());
        assertEquals("2 etapas com 5 h semanais", plano.getRegistros().get(3).getDetalhe());
    }

    @Test
    @DisplayName("Sem avaliacao ou com zero horas usa 4 horas semanais")
    void usaHorasPadrao() {
        catalogo();
        when(competencias.ultimaAvaliacao(1L)).thenReturn(Optional.empty());
        assertEquals(INICIO.plusWeeks(5), agente.criarEtapas(plano(), INICIO).get(0).getPrazo());

        horasSemanais(0);
        assertEquals(INICIO.plusWeeks(5), agente.criarEtapas(plano(), INICIO).get(0).getPrazo());
        assertEquals(1, AgenteDeAcompanhamento.semanas(2, 40));
    }

    @Test
    @DisplayName("O acompanhamento explica cada situacao do plano e avisa etapas atrasadas")
    void acompanha() {
        PlanoDeEstudos aguardando = plano();
        PlanoDeEstudos recusado = plano();
        recusado.recusar(LocalDateTime.now());
        PlanoDeEstudos emAndamento = plano();
        EtapaDoPlano primeira = new EtapaDoPlano(1, 1L, "Fundamentos de Java", 20, INICIO.minusDays(2));
        EtapaDoPlano segunda = new EtapaDoPlano(2, 2L, "HTTP e APIs REST", 12, INICIO.plusWeeks(3));
        ReflectionTestUtils.setField(primeira, "id", 10L);
        ReflectionTestUtils.setField(segunda, "id", 11L);
        emAndamento.autorizar(List.of(primeira, segunda), LocalDateTime.now());

        assertTrue(agente.acompanhar(aguardando, INICIO).situacao().contains("Nenhuma acao foi executada"));
        assertTrue(agente.acompanhar(recusado, INICIO).situacao().contains("Nenhuma etapa foi criada"));

        Acompanhamento atrasado = agente.acompanhar(emAndamento, INICIO);
        assertEquals(0, atrasado.percentualConcluido());
        assertEquals("Proxima etapa: Fundamentos de Java ate 2026-11-21.", atrasado.situacao());
        assertEquals(List.of("Etapa 1 (Fundamentos de Java) atrasada desde 2026-11-21. Replaneje com o gestor."),
                atrasado.alertas());

        emAndamento.concluirEtapa(10L, INICIO);
        Acompanhamento metade = agente.acompanhar(emAndamento, INICIO);
        assertEquals(50, metade.percentualConcluido());
        assertEquals(List.of(), metade.alertas());

        emAndamento.concluirEtapa(11L, INICIO);
        assertEquals(100, agente.acompanhar(emAndamento, INICIO).percentualConcluido());
    }
}
