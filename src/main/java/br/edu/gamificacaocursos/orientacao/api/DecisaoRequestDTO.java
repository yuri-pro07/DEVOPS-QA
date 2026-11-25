package br.edu.gamificacaocursos.orientacao.api;

import jakarta.validation.constraints.NotNull;

public record DecisaoRequestDTO(@NotNull(message = "Informe se o plano foi autorizado") Boolean autorizado) {
}
