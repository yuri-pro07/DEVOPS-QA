package br.edu.gamificacaocursos.recomendacao.domain;

import br.edu.gamificacaocursos.recomendacao.api.CursoIndicadoDTO;
import br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia;

import java.util.List;

public record ResultadoDaPesquisa(NivelDeCompetencia nivel, String objetivo, List<CursoIndicadoDTO> cursos,
                                  List<TrechoDeDocumento> trechos) {
}
