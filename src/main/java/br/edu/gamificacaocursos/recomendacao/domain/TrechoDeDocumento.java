package br.edu.gamificacaocursos.recomendacao.domain;

public record TrechoDeDocumento(String id, String documento, String fonte, String secao, String texto,
                                double relevancia) {
}
