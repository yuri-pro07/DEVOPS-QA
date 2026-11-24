package br.edu.gamificacaocursos.orientacao.application;

import br.edu.gamificacaocursos.curso.api.CursoResponseDTO;
import br.edu.gamificacaocursos.curso.application.CursoService;
import br.edu.gamificacaocursos.orientacao.domain.PropostaDeOrientacao;
import br.edu.gamificacaocursos.orientacao.domain.PropostaDoModelo;
import br.edu.gamificacaocursos.orientacao.domain.RegistroDeAgente;
import br.edu.gamificacaocursos.recomendacao.api.CursoIndicadoDTO;
import br.edu.gamificacaocursos.recomendacao.application.RecomendacaoService;
import br.edu.gamificacaocursos.recomendacao.domain.ResultadoDaPesquisa;
import br.edu.gamificacaocursos.recomendacao.domain.TrechoDeDocumento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia.INICIANTE;
import static br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia.INTERMEDIARIO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgenteOrientadorTest {

    private static final TrechoDeDocumento TRECHO = new TrechoDeDocumento("trilha-intermediaria#2", "Trilha",
            "trilha-intermediaria.md", "Cursos", "Spring Boot na Pratica e Mensageria com RabbitMQ", 0.5);
    private static final List<CursoIndicadoDTO> CANDIDATOS = List.of(
            new CursoIndicadoDTO(4L, "Spring Boot na Pratica", INTERMEDIARIO, 30, TRECHO.id()),
            new CursoIndicadoDTO(6L, "Mensageria com RabbitMQ", INTERMEDIARIO, 18, TRECHO.id()));

    @Mock
    private RecomendacaoService recomendacoes;

    @Mock
    private CursoService cursos;

    @Mock
    private PlanejadorComIa planejador;

    private AgenteOrientador agente;

    @BeforeEach
    void prepara() {
        agente = new AgenteOrientador(recomendacoes, cursos, new PoliticaDeAcessoDosAgentes(), planejador);
    }

    private void documentosIndicam(List<CursoIndicadoDTO> candidatos) {
        when(recomendacoes.nivelAtual(1L)).thenReturn(INTERMEDIARIO);
        when(recomendacoes.pesquisar(INTERMEDIARIO, "backend"))
                .thenReturn(new ResultadoDaPesquisa(INTERMEDIARIO, "backend", candidatos, List.of(TRECHO)));
    }

    @Test
    @DisplayName("Sem LLM o agente consulta o nivel, busca os documentos e propoe os cursos citados")
    void proposeSemLlm() {
        documentosIndicam(CANDIDATOS);
        when(planejador.planejar(anyString(), any())).thenReturn(Optional.empty());

        PropostaDeOrientacao proposta = agente.propor(1L, "backend");

        assertEquals(INTERMEDIARIO, proposta.nivel());
        assertEquals(List.of(4L, 6L), proposta.cursoIds());
        assertEquals(AgenteOrientador.MODO_SEM_LLM, proposta.modo());
        assertTrue(proposta.orientacao().contains("com foco em \"backend\": 1) Spring Boot na Pratica (30 h)"));
        assertTrue(proposta.orientacao().contains("Nenhuma acao foi executada"));
        assertEquals(List.of("consultarNivel", "buscarDocumentos"),
                proposta.registros().stream().map(RegistroDeAgente::getFerramenta).toList());
    }

    @Test
    @DisplayName("Com LLM aceita so cursos que as ferramentas encontraram, sem repetir")
    void aceitaPropostaDoLlmComGuardrail() {
        documentosIndicam(CANDIDATOS);
        when(planejador.planejar(anyString(), any()))
                .thenReturn(Optional.of(new PropostaDoModelo(List.of(6L, 99L, 6L), "  Comece por mensageria [x].  ")));
        when(planejador.descricao()).thenReturn("Spring AI com OpenAI gpt-4o-mini e tool calling");

        PropostaDeOrientacao proposta = agente.propor(1L, "backend");

        assertEquals(List.of(6L), proposta.cursoIds());
        assertEquals("Comece por mensageria [x].", proposta.orientacao());
        assertEquals("Spring AI com OpenAI gpt-4o-mini e tool calling", proposta.modo());
    }

    @Test
    @DisplayName("Proposta do LLM invalida e descartada e o agente usa o plano dos documentos")
    void descartaPropostaInvalida() {
        documentosIndicam(CANDIDATOS);
        when(planejador.planejar(anyString(), any())).thenReturn(
                Optional.of(new PropostaDoModelo(List.of(99L), "inventado")),
                Optional.of(new PropostaDoModelo(null, "sem ids")),
                Optional.of(new PropostaDoModelo(List.of(4L), " ")),
                Optional.of(new PropostaDoModelo(List.of(4L), null)));

        for (int tentativa = 0; tentativa < 4; tentativa++) {
            PropostaDeOrientacao proposta = agente.propor(1L, "backend");
            assertEquals(List.of(4L, 6L), proposta.cursoIds());
            assertEquals(AgenteOrientador.MODO_SEM_LLM, proposta.modo());
        }
    }

    @Test
    @DisplayName("Sem cursos nos documentos o agente nao chama o LLM e devolve proposta vazia")
    void semCursos() {
        when(recomendacoes.nivelAtual(1L)).thenReturn(null);
        when(recomendacoes.pesquisar(null, "")).thenReturn(new ResultadoDaPesquisa(null, "", List.of(), List.of()));

        PropostaDeOrientacao proposta = agente.propor(1L, "");

        assertNull(proposta.nivel());
        assertEquals(List.of(), proposta.cursoIds());
        assertEquals(AgenteOrientador.SEM_CURSOS, proposta.orientacao());
        verifyNoInteractions(planejador);
    }

    @Test
    @DisplayName("O contexto enviado ao LLM traz candidatos e trechos, e a orientacao padrao trata nivel nao avaliado")
    void contextoEOrientacao() {
        ResultadoDaPesquisa pesquisa = new ResultadoDaPesquisa(null, "", CANDIDATOS.subList(0, 1), List.of(TRECHO));

        String contexto = AgenteOrientador.contexto(pesquisa);
        String orientacao = AgenteOrientador.orientacaoPadrao(pesquisa);

        assertTrue(contexto.contains("Nivel do colaborador: ainda nao avaliado"));
        assertTrue(contexto.contains("Objetivo: nao informado"));
        assertTrue(contexto.contains("4 | Spring Boot na Pratica | INTERMEDIARIO | 30 h | trilha-intermediaria#2"));
        assertTrue(contexto.contains("[trilha-intermediaria#2] Spring Boot"));
        assertTrue(orientacao.startsWith("Plano sugerido pelo agente orientador para o nivel ainda nao avaliado: 1)"));
    }

    @Test
    @DisplayName("As ferramentas registram cada uso e guardam os cursos encontrados para o guardrail")
    void ferramentas() {
        when(recomendacoes.nivelAtual(1L)).thenReturn(INICIANTE);
        when(recomendacoes.pesquisar(null, "java"))
                .thenReturn(new ResultadoDaPesquisa(null, "java", CANDIDATOS, List.of(TRECHO)));
        CursoResponseDTO curso = new CursoResponseDTO(4L, "Spring Boot na Pratica", "d", "Dev", INTERMEDIARIO, 30);
        when(cursos.buscarPorId(4L)).thenReturn(curso);
        FerramentasDoOrientador ferramentas = new FerramentasDoOrientador(1L, recomendacoes, cursos,
                new PoliticaDeAcessoDosAgentes());

        assertEquals("INICIANTE", ferramentas.consultarNivel());
        ferramentas.buscarDocumentos("java", null);
        assertEquals(curso, ferramentas.consultarCurso(4L));

        assertEquals(List.of(4L, 6L), List.copyOf(ferramentas.cursosEncontrados().keySet()));
        assertEquals(Arrays.asList("consultarNivel", "buscarDocumentos", "consultarCurso"),
                ferramentas.registros().stream().map(RegistroDeAgente::getFerramenta).toList());
        assertEquals("objetivo: java", ferramentas.registros().get(1).getDetalhe());
    }
}
