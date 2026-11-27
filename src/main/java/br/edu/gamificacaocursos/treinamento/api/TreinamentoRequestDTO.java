package br.edu.gamificacaocursos.treinamento.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TreinamentoRequestDTO(
        @NotBlank(message = "Informe quem esta solicitando o treinamento") @Size(max = 100) String solicitante,
        @Size(max = 300) String motivo,
        @Min(value = 1000, message = "Use pelo menos 1000 amostras")
        @Max(value = 200000, message = "Use no maximo 200000 amostras") Integer amostras,
        @Min(0) Integer semente) {
}
