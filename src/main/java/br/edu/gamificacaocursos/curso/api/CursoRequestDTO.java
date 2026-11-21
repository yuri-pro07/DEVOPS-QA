package br.edu.gamificacaocursos.curso.api;

import br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CursoRequestDTO(
        @NotBlank(message = "Titulo e obrigatorio") String titulo,
        @NotBlank(message = "Descricao e obrigatoria")
        @Size(max = 1000, message = "Descricao com no maximo 1000 caracteres") String descricao,
        @NotBlank(message = "Area e obrigatoria") String area,
        @NotNull(message = "Nivel e obrigatorio") NivelDeCompetencia nivel,
        @NotNull(message = "Carga horaria e obrigatoria")
        @Min(value = 1, message = "Carga horaria minima de 1 hora")
        @Max(value = 400, message = "Carga horaria maxima de 400 horas") Integer cargaHoraria) {
}
