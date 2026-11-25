package br.edu.gamificacaocursos.treinamento.domain;

public record AndamentoDoTreinamento(Long solicitacaoId, StatusDoTreinamento status, String detalhe, Integer tentativa,
                                     ResultadoDoTreinamento resultado) {
}
