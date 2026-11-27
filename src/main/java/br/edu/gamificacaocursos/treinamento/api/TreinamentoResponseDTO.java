package br.edu.gamificacaocursos.treinamento.api;

import br.edu.gamificacaocursos.treinamento.domain.ResultadoDoTreinamento;
import br.edu.gamificacaocursos.treinamento.domain.StatusDoTreinamento;

import java.time.LocalDateTime;
import java.util.List;

public record TreinamentoResponseDTO(Long id, String solicitante, String motivo, int amostras, Integer semente,
                                     StatusDoTreinamento status, int percentual, String situacao, boolean finalizado,
                                     int tentativas, LocalDateTime criadaEm, LocalDateTime enviadaEm,
                                     LocalDateTime atualizadaEm, LocalDateTime concluidaEm,
                                     ResultadoDoTreinamento modelo, List<EventoDoTreinamentoDTO> historico) {
}
