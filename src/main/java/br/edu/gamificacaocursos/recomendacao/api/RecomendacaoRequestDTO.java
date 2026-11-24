package br.edu.gamificacaocursos.recomendacao.api;

import jakarta.validation.constraints.Size;

public record RecomendacaoRequestDTO(@Size(max = 300, message = "O objetivo deve ter no maximo 300 caracteres")
                                     String objetivo) {
}
