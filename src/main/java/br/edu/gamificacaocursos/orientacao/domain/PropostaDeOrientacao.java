package br.edu.gamificacaocursos.orientacao.domain;

import br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia;

import java.util.List;

public record PropostaDeOrientacao(NivelDeCompetencia nivel, List<Long> cursoIds, String orientacao, String modo,
                                   List<RegistroDeAgente> registros) {
}
