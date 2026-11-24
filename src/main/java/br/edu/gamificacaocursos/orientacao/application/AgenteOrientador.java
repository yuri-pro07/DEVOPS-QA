package br.edu.gamificacaocursos.orientacao.application;

import br.edu.gamificacaocursos.curso.application.CursoService;
import br.edu.gamificacaocursos.orientacao.domain.PropostaDeOrientacao;
import br.edu.gamificacaocursos.orientacao.domain.PropostaDoModelo;
import br.edu.gamificacaocursos.recomendacao.api.CursoIndicadoDTO;
import br.edu.gamificacaocursos.recomendacao.application.RecomendacaoService;
import br.edu.gamificacaocursos.recomendacao.domain.ResultadoDaPesquisa;
import br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AgenteOrientador {
    static final int MAXIMO_DE_CURSOS = 3;
    static final String MODO_SEM_LLM = "agente deterministico, sem LLM";
    static final String SEM_CURSOS = "Os documentos institucionais nao indicaram cursos para esse objetivo.";

    private final RecomendacaoService recomendacoes;
    private final CursoService cursos;
    private final PoliticaDeAcessoDosAgentes politica;
    private final PlanejadorComIa planejador;

    public AgenteOrientador(RecomendacaoService recomendacoes, CursoService cursos,
                            PoliticaDeAcessoDosAgentes politica, PlanejadorComIa planejador) {
        this.recomendacoes = recomendacoes;
        this.cursos = cursos;
        this.politica = politica;
        this.planejador = planejador;
    }

    public PropostaDeOrientacao propor(Long colaboradorId, String objetivo) {
        FerramentasDoOrientador ferramentas = new FerramentasDoOrientador(colaboradorId, recomendacoes, cursos, politica);
        String nivelInformado = ferramentas.consultarNivel();
        NivelDeCompetencia nivel = FerramentasDoOrientador.NAO_AVALIADO.equals(nivelInformado)
                ? null
                : NivelDeCompetencia.valueOf(nivelInformado);
        ResultadoDaPesquisa pesquisa = ferramentas.buscarDocumentos(objetivo, nivel);
        if (pesquisa.cursos().isEmpty()) {
            return new PropostaDeOrientacao(nivel, List.of(), SEM_CURSOS, MODO_SEM_LLM, ferramentas.registros());
        }
        Optional<PropostaDeOrientacao> doModelo = planejador.planejar(contexto(pesquisa), ferramentas)
                .flatMap(proposta -> validar(proposta, ferramentas))
                .map(proposta -> new PropostaDeOrientacao(nivel, proposta.cursoIds(), proposta.orientacao().trim(),
                        planejador.descricao(), ferramentas.registros()));
        return doModelo.orElseGet(() -> new PropostaDeOrientacao(nivel,
                pesquisa.cursos().stream().map(CursoIndicadoDTO::id).toList(), orientacaoPadrao(pesquisa),
                MODO_SEM_LLM, ferramentas.registros()));
    }

    static Optional<PropostaDoModelo> validar(PropostaDoModelo proposta, FerramentasDoOrientador ferramentas) {
        if (proposta.cursoIds() == null || proposta.orientacao() == null || proposta.orientacao().isBlank()) {
            return Optional.empty();
        }
        List<Long> permitidos = proposta.cursoIds().stream()
                .filter(ferramentas.cursosEncontrados()::containsKey)
                .distinct()
                .limit(MAXIMO_DE_CURSOS)
                .toList();
        return permitidos.isEmpty()
                ? Optional.empty()
                : Optional.of(new PropostaDoModelo(permitidos, proposta.orientacao()));
    }

    static String contexto(ResultadoDaPesquisa pesquisa) {
        return "Nivel do colaborador: " + rotulo(pesquisa.nivel()) + "\n"
                + "Objetivo: " + (pesquisa.objetivo().isEmpty() ? "nao informado" : pesquisa.objetivo()) + "\n"
                + "Cursos candidatos (id | titulo | nivel | carga horaria | trecho):\n"
                + pesquisa.cursos().stream()
                .map(c -> c.id() + " | " + c.titulo() + " | " + c.nivel() + " | " + c.cargaHoraria() + " h | " + c.fonte())
                .collect(Collectors.joining("\n")) + "\n"
                + "Trechos dos documentos:\n" + pesquisa.trechos().stream()
                .map(t -> "[" + t.id() + "] " + t.texto())
                .collect(Collectors.joining("\n"));
    }

    static String orientacaoPadrao(ResultadoDaPesquisa pesquisa) {
        StringBuilder texto = new StringBuilder("Plano sugerido pelo agente orientador para o nivel ")
                .append(rotulo(pesquisa.nivel()));
        if (!pesquisa.objetivo().isEmpty()) {
            texto.append(" com foco em \"").append(pesquisa.objetivo()).append("\"");
        }
        texto.append(": ");
        int posicao = 1;
        for (CursoIndicadoDTO curso : pesquisa.cursos()) {
            texto.append(posicao++).append(") ").append(curso.titulo()).append(" (").append(curso.cargaHoraria())
                    .append(" h) [").append(curso.fonte()).append("]; ");
        }
        return texto.append("A politica de formacao recomenda no maximo tres cursos ativos por vez. ")
                .append("Nenhuma acao foi executada: autorize o plano para que o agente de acompanhamento ")
                .append("crie as etapas com prazos.").toString();
    }

    private static String rotulo(NivelDeCompetencia nivel) {
        return nivel == null ? "ainda nao avaliado" : nivel.name();
    }
}
