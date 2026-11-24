package br.edu.gamificacaocursos.orientacao.api;

import jakarta.validation.constraints.Size;

public record PlanoRequestDTO(@Size(max = 300, message = "O objetivo deve ter no maximo 300 caracteres")
                              String objetivo) {
}
