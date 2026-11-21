package br.edu.gamificacaocursos.competencia.api;

import br.edu.gamificacaocursos.competencia.domain.PerfilDeCompetencia;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AvaliacaoRequestDTO(
        @NotNull(message = "Meses de experiencia e obrigatorio")
        @Min(value = 0, message = "Meses de experiencia nao pode ser negativo")
        @Max(value = 600, message = "Meses de experiencia acima do limite") Integer mesesExperiencia,
        @NotNull(message = "Cursos concluidos e obrigatorio")
        @Min(value = 0, message = "Cursos concluidos nao pode ser negativo")
        @Max(value = 100, message = "Cursos concluidos acima do limite") Integer cursosConcluidos,
        @NotNull(message = "Media das avaliacoes e obrigatoria")
        @DecimalMin(value = "0.0", message = "Media minima 0")
        @DecimalMax(value = "10.0", message = "Media maxima 10") Double mediaAvaliacoes,
        @NotNull(message = "Horas semanais e obrigatorio")
        @Min(value = 0, message = "Horas semanais nao pode ser negativo")
        @Max(value = 80, message = "Horas semanais acima do limite") Integer horasSemanais,
        @NotNull(message = "Informe se conhece REST") Boolean conheceRest,
        @NotNull(message = "Informe se conhece banco de dados") Boolean conheceBancoDeDados) {

    public PerfilDeCompetencia paraPerfil() {
        return new PerfilDeCompetencia(mesesExperiencia, cursosConcluidos, mediaAvaliacoes, horasSemanais,
                conheceRest, conheceBancoDeDados);
    }
}
