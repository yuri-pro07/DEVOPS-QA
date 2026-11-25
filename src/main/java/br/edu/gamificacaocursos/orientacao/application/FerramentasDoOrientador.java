package br.edu.gamificacaocursos.orientacao.application;

import br.edu.gamificacaocursos.curso.api.CursoResponseDTO;
import br.edu.gamificacaocursos.curso.application.CursoService;
import br.edu.gamificacaocursos.orientacao.domain.RegistroDeAgente;
import br.edu.gamificacaocursos.recomendacao.api.CursoIndicadoDTO;
import br.edu.gamificacaocursos.recomendacao.application.RecomendacaoService;
import br.edu.gamificacaocursos.recomendacao.domain.ResultadoDaPesquisa;
import br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static br.edu.gamificacaocursos.orientacao.application.PoliticaDeAcessoDosAgentes.AGENTE_ORIENTADOR;

public class FerramentasDoOrientador {
    static final String NAO_AVALIADO = "NAO_AVALIADO";

    private final Long colaboradorId;
    private final RecomendacaoService recomendacoes;
    private final CursoService cursos;
    private final PoliticaDeAcessoDosAgentes politica;
    private final List<RegistroDeAgente> registros = new ArrayList<>();
    private final Map<Long, CursoIndicadoDTO> cursosEncontrados = new LinkedHashMap<>();

    public FerramentasDoOrientador(Long colaboradorId, RecomendacaoService recomendacoes, CursoService cursos,
                                   PoliticaDeAcessoDosAgentes politica) {
        this.colaboradorId = colaboradorId;
        this.recomendacoes = recomendacoes;
        this.cursos = cursos;
        this.politica = politica;
    }

    @Tool(description = "Consulta o nivel de competencia mais recente do colaborador atendido. "
            + "Devolve INICIANTE, INTERMEDIARIO, AVANCADO ou NAO_AVALIADO.")
    public String consultarNivel() {
        usar("consultarNivel", "nivel do colaborador atendido");
        NivelDeCompetencia nivel = recomendacoes.nivelAtual(colaboradorId);
        return nivel == null ? NAO_AVALIADO : nivel.name();
    }

    @Tool(description = "Busca nos documentos institucionais os cursos indicados para um objetivo de estudo.")
    public ResultadoDaPesquisa buscarDocumentos(
            @ToolParam(description = "Objetivo de estudo em poucas palavras") String objetivo,
            @ToolParam(description = "Nivel INICIANTE, INTERMEDIARIO ou AVANCADO", required = false)
            NivelDeCompetencia nivel) {
        usar("buscarDocumentos", "objetivo: " + objetivo);
        ResultadoDaPesquisa pesquisa = recomendacoes.pesquisar(nivel, objetivo);
        pesquisa.cursos().forEach(curso -> cursosEncontrados.putIfAbsent(curso.id(), curso));
        return pesquisa;
    }

    @Tool(description = "Consulta titulo, descricao, nivel e carga horaria de um curso do catalogo pelo id.")
    public CursoResponseDTO consultarCurso(@ToolParam(description = "Id do curso") Long cursoId) {
        usar("consultarCurso", "curso " + cursoId);
        return cursos.buscarPorId(cursoId);
    }

    public List<RegistroDeAgente> registros() {
        return List.copyOf(registros);
    }

    public Map<Long, CursoIndicadoDTO> cursosEncontrados() {
        return Collections.unmodifiableMap(cursosEncontrados);
    }

    private void usar(String ferramenta, String detalhe) {
        politica.verificar(AGENTE_ORIENTADOR, ferramenta);
        registros.add(new RegistroDeAgente(AGENTE_ORIENTADOR, ferramenta, detalhe, LocalDateTime.now()));
    }
}
