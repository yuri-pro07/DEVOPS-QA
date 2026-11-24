package br.edu.gamificacaocursos.orientacao.application;

import br.edu.gamificacaocursos.colaborador.application.ColaboradorService;
import br.edu.gamificacaocursos.curso.api.CursoResponseDTO;
import br.edu.gamificacaocursos.curso.application.CursoService;
import br.edu.gamificacaocursos.orientacao.api.PlanoResponseDTO;
import br.edu.gamificacaocursos.orientacao.domain.Acompanhamento;
import br.edu.gamificacaocursos.orientacao.domain.EtapaDoPlano;
import br.edu.gamificacaocursos.orientacao.domain.PlanoDeEstudos;
import br.edu.gamificacaocursos.orientacao.domain.PropostaDeOrientacao;
import br.edu.gamificacaocursos.orientacao.domain.RegistroDeAgente;
import br.edu.gamificacaocursos.orientacao.domain.StatusDoPlano;
import br.edu.gamificacaocursos.orientacao.internal.persistence.PlanoDeEstudosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia.INICIANTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrientacaoServiceTest {

    private static final Acompanhamento SITUACAO = new Acompanhamento(0, "situacao", List.of());

    @Mock
    private ColaboradorService colaboradores;

    @Mock
    private CursoService cursos;

    @Mock
    private AgenteOrientador orientador;

    @Mock
    private AgenteDeAcompanhamento acompanhamento;

    @Mock
    private PlanoDeEstudosRepository repository;

    private OrientacaoService service;

    @BeforeEach
    void prepara() {
        service = new OrientacaoService(colaboradores, cursos, orientador, acompanhamento, repository);
    }

    private static PlanoDeEstudos plano() {
        PlanoDeEstudos plano = new PlanoDeEstudos(1L, INICIANTE, "java", "orientacao", "agente", List.of(1L),
                LocalDateTime.now());
        ReflectionTestUtils.setField(plano, "id", 7L);
        return plano;
    }

    private void catalogo() {
        when(cursos.buscarPorId(1L)).thenReturn(new CursoResponseDTO(1L, "Fundamentos de Java", "d", "Dev", INICIANTE, 20));
        when(acompanhamento.acompanhar(any(), any())).thenReturn(SITUACAO);
    }

    @Test
    @DisplayName("O coordenador pede a proposta ao orientador e grava o plano aguardando autorizacao, com a auditoria")
    void propoe() {
        catalogo();
        RegistroDeAgente registro = new RegistroDeAgente("agente-orientador", "buscarDocumentos", "objetivo: java",
                LocalDateTime.now());
        when(orientador.propor(1L, "java")).thenReturn(new PropostaDeOrientacao(INICIANTE, List.of(1L),
                "orientacao", "agente deterministico, sem LLM", List.of(registro)));
        when(repository.save(any(PlanoDeEstudos.class))).thenAnswer(chamada -> chamada.getArgument(0));

        PlanoResponseDTO plano = service.propor(1L, "  java ");

        assertEquals(StatusDoPlano.AGUARDANDO_AUTORIZACAO, plano.status());
        assertEquals("Fundamentos de Java", plano.cursosSugeridos().get(0).titulo());
        assertEquals("buscarDocumentos", plano.registrosDosAgentes().get(0).ferramenta());
        assertEquals(SITUACAO, plano.acompanhamento());
        assertEquals(List.of(), plano.etapas());
        InOrder ordem = inOrder(colaboradores, orientador, repository);
        ordem.verify(colaboradores).buscarPorId(1L);
        ordem.verify(orientador).propor(1L, "java");
        ordem.verify(repository).save(any(PlanoDeEstudos.class));
    }

    @Test
    @DisplayName("Sem cursos indicados nao grava plano e devolve o motivo")
    void semCursos() {
        when(orientador.propor(1L, "")).thenReturn(new PropostaDeOrientacao(null, List.of(),
                AgenteOrientador.SEM_CURSOS, AgenteOrientador.MODO_SEM_LLM, List.of()));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.propor(1L, null));

        assertEquals(AgenteOrientador.SEM_CURSOS, erro.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Autorizar chama o agente de acompanhamento para criar as etapas; recusar nao cria nada")
    void decide() {
        catalogo();
        PlanoDeEstudos autorizado = plano();
        PlanoDeEstudos recusado = plano();
        when(repository.findById(7L)).thenReturn(Optional.of(autorizado), Optional.of(recusado));
        when(repository.saveAndFlush(any(PlanoDeEstudos.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(acompanhamento.criarEtapas(eq(autorizado), any(LocalDate.class))).thenReturn(
                List.of(new EtapaDoPlano(1, 1L, "Fundamentos de Java", 20, LocalDate.now().plusWeeks(5))));

        PlanoResponseDTO primeiro = service.decidir(7L, true);
        PlanoResponseDTO segundo = service.decidir(7L, false);

        assertEquals(StatusDoPlano.AUTORIZADO, primeiro.status());
        assertEquals("Fundamentos de Java", primeiro.etapas().get(0).tituloDoCurso());
        assertEquals(StatusDoPlano.RECUSADO, segundo.status());
        assertEquals(List.of(), segundo.etapas());
        verify(acompanhamento, never()).criarEtapas(eq(recusado), any());
    }

    @Test
    @DisplayName("Plano ja decidido nao aciona o agente de acompanhamento de novo")
    void naoDecideDuasVezes() {
        PlanoDeEstudos plano = plano();
        plano.recusar(LocalDateTime.now());
        when(repository.findById(7L)).thenReturn(Optional.of(plano));

        assertThrows(IllegalStateException.class, () -> service.decidir(7L, true));
        verifyNoInteractions(acompanhamento);
    }

    @Test
    @DisplayName("Concluir etapa, consultar e listar passam pelo repositorio; plano inexistente e erro 400")
    void concluiConsultaELista() {
        catalogo();
        PlanoDeEstudos plano = plano();
        EtapaDoPlano etapa = new EtapaDoPlano(1, 1L, "Fundamentos de Java", 20, LocalDate.now().plusWeeks(5));
        ReflectionTestUtils.setField(etapa, "id", 3L);
        plano.autorizar(List.of(etapa), LocalDateTime.now());
        when(repository.findById(7L)).thenReturn(Optional.of(plano));
        when(repository.saveAndFlush(plano)).thenReturn(plano);
        when(repository.findByColaboradorIdOrderByCriadoEmDescIdDesc(1L)).thenReturn(List.of(plano));
        when(repository.findById(8L)).thenReturn(Optional.empty());

        assertEquals(StatusDoPlano.CONCLUIDO, service.concluirEtapa(7L, 3L).status());
        assertEquals(7L, service.consultar(7L).id());
        assertEquals(1, service.listar(1L).size());
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.consultar(8L));
        assertEquals("Plano nao encontrado: 8", erro.getMessage());
        ArgumentCaptor<Long> colaborador = ArgumentCaptor.forClass(Long.class);
        verify(colaboradores).buscarPorId(colaborador.capture());
        assertEquals(1L, colaborador.getValue());
    }
}
