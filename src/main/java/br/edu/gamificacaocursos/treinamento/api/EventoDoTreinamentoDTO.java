package br.edu.gamificacaocursos.treinamento.api;

import br.edu.gamificacaocursos.treinamento.domain.StatusDoTreinamento;

import java.time.LocalDateTime;

public record EventoDoTreinamentoDTO(StatusDoTreinamento status, String detalhe, LocalDateTime registradoEm) {
}
