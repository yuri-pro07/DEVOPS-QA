package br.edu.gamificacaocursos.orientacao.domain;

import java.util.List;

public record PropostaDoModelo(List<Long> cursoIds, String orientacao) {
}
