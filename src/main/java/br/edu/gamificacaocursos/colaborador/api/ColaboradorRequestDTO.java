package br.edu.gamificacaocursos.colaborador.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ColaboradorRequestDTO(
        @NotBlank(message = "Nome e obrigatorio") String nome,
        @NotBlank(message = "Email e obrigatorio") @Email(message = "Email invalido") String email,
        @NotBlank(message = "Cargo e obrigatorio") String cargo,
        @NotBlank(message = "Area e obrigatoria") String area) {
}
