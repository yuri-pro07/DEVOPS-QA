package br.edu.gamificacaocursos.recomendacao.application;

import br.edu.gamificacaocursos.colaborador.application.ColaboradorService;
import br.edu.gamificacaocursos.competencia.api.AvaliacaoResponseDTO;
import br.edu.gamificacaocursos.competencia.application.CompetenciaService;
import br.edu.gamificacaocursos.curso.api.CursoResponseDTO;
import br.edu.gamificacaocursos.curso.application.CursoService;
import br.edu.gamificacaocursos.recomendacao.api.CursoIndicadoDTO;
import br.edu.gamificacaocursos.recomendacao.api.RecomendacaoResponseDTO;
import br.edu.gamificacaocursos.recomendacao.domain.RespostaRedigida;
import br.edu.gamificacaocursos.recomendacao.domain.ResultadoDaPesquisa;
import br.edu.gamificacaocursos.recomendacao.domain.TrechoDeDocumento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia.AVANCADO;
import static br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia.INICIANTE;
import static br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia.INTERMEDIARIO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecomendacaoServiceTest {

    private static final List<CursoResponseDTO> CATALOGO = List.of(
            new CursoResponseDTO(1L, "Fundamentos de Java", "d", "Desenvolvimento", INICIANTE, 20),
            new CursoResponseDTO(4L, "Spring Boot na Pratica", "d", "Desenvolvimento", INTERMEDIARIO, 30),
            new CursoResponseDTO(5L, "Testes Automatizados com JUnit e Mockito", "d", "Qualidade", INTERMEDIARIO, 24),
            new CursoResponseDTO(6L, "Mensageria com RabbitMQ", "d", "Arquitetura", INTERMEDIARIO, 18),
            new CursoResponseDTO(7L, "Arquitetura de Software e Monolito Modular", "d", "Arquitetura", AVANCADO, 32));

    @Mock
    private ColaboradorService colaboradores;

    @Mock
    private CompetenciaService competencias;

    @Mock
    private CursoService cursos;

    @Mock
    private BuscaDeDocumentos documentos;

    @Mock
    private RedatorDeRecomendacao redator;

    private RecomendacaoService service;

    @BeforeEach
    void prepara() {
        service = new RecomendacaoService(colaboradores, competencias, cursos, documentos, redator);
    }

    private static TrechoDeDocumento trecho(String id, String texto) {
        return new TrechoDeDocumento(id, "Documento", id + ".md", "Secao", texto, 0.5);
    }

    private void avaliadoComo(Long colaboradorId, br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia nivel) {
        AvaliacaoResponseDTO avaliacao = mock(AvaliacaoResponseDTO.class);
        when(avaliacao.nivel()).thenReturn(nivel);
        when(competencias.ultimaAvaliacao(colaboradorId)).thenReturn(Optional.of(avaliacao));
    }

    @Test
    @DisplayName("Busca os trechos pelo objetivo e nivel, cruza com o catalogo e manda o redator escrever")
    void recomendaComRag() {
        avaliadoComo(1L, INTERMEDIARIO);
        when(cursos.listar(null, null)).thenReturn(CATALOGO);
        when(documentos.buscar("mensageria cursos trilha intermediario", 4)).thenReturn(List.of(
                trecho("trilha-intermediaria#3", "O curso indicado e Mensageria com RabbitMQ."),
                trecho("trilha-intermediaria#2", "Spring Boot na Pratica, depois Testes Automatizados com JUnit e Mockito"
                        + " e por fim Mensageria com RabbitMQ.")));
        when(redator.redigir(any())).thenReturn(new RespostaRedigida("texto", "extrativo, sem LLM"));

        RecomendacaoResponseDTO resposta = service.recomendar(1L, "  mensageria ");

        assertEquals(INTERMEDIARIO, resposta.nivel());
        assertEquals("mensageria", resposta.objetivo());
        assertEquals("texto", resposta.resposta());
        assertEquals(List.of(6L, 4L, 5L), resposta.cursos().stream().map(CursoIndicadoDTO::id).toList());
        assertEquals("trilha-intermediaria#3", resposta.cursos().get(0).fonte());
        assertEquals(2, resposta.fontes().size());
        ArgumentCaptor<ResultadoDaPesquisa> pesquisa = ArgumentCaptor.forClass(ResultadoDaPesquisa.class);
        verify(redator).redigir(pesquisa.capture());
        assertEquals(3, pesquisa.getValue().cursos().size());
        verify(colaboradores).buscarPorId(1L);
    }

    @Test
    @DisplayName("Cursos do nivel do colaborador vem primeiro, sem repetir e no maximo tres")
    void priorizaONivelELimita() {
        when(cursos.listar(null, null)).thenReturn(CATALOGO);
        when(documentos.buscar(anyString(), anyInt())).thenReturn(List.of(
                trecho("a#1", "Fundamentos de Java e Arquitetura de Software e Monolito Modular"),
                trecho("b#1", "FUNDAMENTOS DE JAVA, Spring Boot na Prática e Mensageria com RabbitMQ")));

        ResultadoDaPesquisa pesquisa = service.pesquisar(INTERMEDIARIO, null);

        assertEquals(List.of(4L, 6L, 1L), pesquisa.cursos().stream().map(CursoIndicadoDTO::id).toList());
        assertEquals("a#1", pesquisa.cursos().get(2).fonte());
        assertEquals("", pesquisa.objetivo());
    }

    @Test
    @DisplayName("No mesmo nivel vem primeiro o curso citado em mais trechos e depois a ordem do texto")
    void ordenaPorCitacoesEPosicao() {
        when(cursos.listar(null, null)).thenReturn(CATALOGO);
        when(documentos.buscar(anyString(), anyInt())).thenReturn(List.of(
                trecho("a#1", "Testes Automatizados com JUnit e Mockito antes de Spring Boot na Pratica"),
                trecho("b#1", "Spring Boot na Pratica")));

        ResultadoDaPesquisa pesquisa = service.pesquisar(INTERMEDIARIO, "spring");

        assertEquals(List.of(4L, 5L), pesquisa.cursos().stream().map(CursoIndicadoDTO::id).toList());
        assertEquals("a#1", pesquisa.cursos().get(0).fonte());
    }

    @Test
    @DisplayName("Sem documentos encontrados nao chama o redator e explica o motivo")
    void semFontes() {
        when(competencias.ultimaAvaliacao(2L)).thenReturn(Optional.empty());
        when(cursos.listar(null, null)).thenReturn(CATALOGO);
        when(documentos.buscar("culinaria cursos trilha formacao", 4)).thenReturn(List.of());

        RecomendacaoResponseDTO resposta = service.recomendar(2L, "culinaria");

        assertNull(resposta.nivel());
        assertEquals(RecomendacaoService.SEM_FONTES, resposta.resposta());
        assertEquals("sem fontes", resposta.modo());
        assertEquals(List.of(), resposta.cursos());
        verifyNoInteractions(redator);
    }

    @Test
    @DisplayName("Colaborador inexistente interrompe antes de consultar os documentos")
    void colaboradorInexistente() {
        when(colaboradores.buscarPorId(9L)).thenThrow(new IllegalArgumentException("Colaborador nao encontrado: 9"));

        assertThrows(IllegalArgumentException.class, () -> service.recomendar(9L, "java"));
        verifyNoInteractions(documentos, redator);
    }

    @Test
    @DisplayName("Monta a consulta com o objetivo e a trilha do nivel e normaliza acentos")
    void consultaENormalizacao() {
        assertEquals("cursos trilha avancado", RecomendacaoService.consulta(AVANCADO, ""));
        assertEquals("dados cursos trilha formacao", RecomendacaoService.consulta(null, "dados"));
        assertEquals("pratica e inteligencia", RecomendacaoService.normalizar("Prática E Inteligência"));
    }
}
