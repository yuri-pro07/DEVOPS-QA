package br.edu.gamificacaocursos.treinamento.domain;

public record ResultadoDoTreinamento(String versao, Integer semente, Integer amostrasDeTreino, Integer amostrasDeTeste,
                                     Double acuracia, Double f1Macro) {
}
