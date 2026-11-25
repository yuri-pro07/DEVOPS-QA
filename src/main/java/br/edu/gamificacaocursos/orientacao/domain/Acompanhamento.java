package br.edu.gamificacaocursos.orientacao.domain;

import java.util.List;

public record Acompanhamento(int percentualConcluido, String situacao, List<String> alertas) {
}
