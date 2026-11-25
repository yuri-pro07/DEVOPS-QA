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
import br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RecomendacaoService {
    static final int TRECHOS_POR_BUSCA = 4;
    static final int MAXIMO_DE_CURSOS = 3;
    static final String SEM_FONTES = "Nao foram encontrados documentos institucionais sobre esse objetivo. "
            + "Tente descrever o objetivo com outras palavras.";

    private final ColaboradorService colaboradores;
    private final CompetenciaService competencias;
    private final CursoService cursos;
    private final BuscaDeDocumentos documentos;
    private final RedatorDeRecomendacao redator;

    public RecomendacaoService(ColaboradorService colaboradores, CompetenciaService competencias,
                               CursoService cursos, BuscaDeDocumentos documentos, RedatorDeRecomendacao redator) {
        this.colaboradores = colaboradores;
        this.competencias = competencias;
        this.cursos = cursos;
        this.documentos = documentos;
        this.redator = redator;
    }

    public RecomendacaoResponseDTO recomendar(Long colaboradorId, String objetivo) {
        colaboradores.buscarPorId(colaboradorId);
        ResultadoDaPesquisa pesquisa = pesquisar(nivelAtual(colaboradorId), objetivo);
        RespostaRedigida resposta = pesquisa.trechos().isEmpty()
                ? new RespostaRedigida(SEM_FONTES, "sem fontes")
                : redator.redigir(pesquisa);
        return new RecomendacaoResponseDTO(colaboradorId, pesquisa.nivel(), pesquisa.objetivo(), resposta.texto(),
                resposta.modo(), pesquisa.cursos(), pesquisa.trechos());
    }

    public NivelDeCompetencia nivelAtual(Long colaboradorId) {
        return competencias.ultimaAvaliacao(colaboradorId).map(AvaliacaoResponseDTO::nivel).orElse(null);
    }

    public ResultadoDaPesquisa pesquisar(NivelDeCompetencia nivel, String objetivo) {
        String objetivoLimpo = objetivo == null ? "" : objetivo.trim();
        List<TrechoDeDocumento> trechos = documentos.buscar(consulta(nivel, objetivoLimpo), TRECHOS_POR_BUSCA);
        return new ResultadoDaPesquisa(nivel, objetivoLimpo, cursosCitados(trechos, nivel), trechos);
    }

    static String consulta(NivelDeCompetencia nivel, String objetivo) {
        String trilha = nivel == null
                ? "cursos trilha formacao"
                : "cursos trilha " + nivel.name().toLowerCase(Locale.ROOT);
        return (objetivo + " " + trilha).trim();
    }

    private List<CursoIndicadoDTO> cursosCitados(List<TrechoDeDocumento> trechos, NivelDeCompetencia nivel) {
        List<CursoResponseDTO> catalogo = cursos.listar(null, null);
        Map<Long, CursoIndicadoDTO> citados = new LinkedHashMap<>();
        Map<Long, Integer> citacoes = new HashMap<>();
        for (TrechoDeDocumento trecho : trechos) {
            String texto = normalizar(trecho.texto());
            catalogo.stream()
                    .filter(curso -> texto.contains(normalizar(curso.titulo())))
                    .sorted(Comparator.comparingInt(curso -> texto.indexOf(normalizar(curso.titulo()))))
                    .forEach(curso -> {
                        citacoes.merge(curso.id(), 1, Integer::sum);
                        citados.putIfAbsent(curso.id(), new CursoIndicadoDTO(curso.id(), curso.titulo(),
                                curso.nivel(), curso.cargaHoraria(), trecho.id()));
                    });
        }
        List<CursoIndicadoDTO> ordenados = new ArrayList<>(citados.values());
        ordenados.sort(Comparator.<CursoIndicadoDTO>comparingInt(curso -> curso.nivel() == nivel ? 0 : 1)
                .thenComparingInt(curso -> -citacoes.get(curso.id())));
        return ordenados.stream().limit(MAXIMO_DE_CURSOS).toList();
    }

    static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
