package br.edu.gamificacaocursos.recomendacao.api;

import br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia;

public record CursoIndicadoDTO(Long id, String titulo, NivelDeCompetencia nivel, int cargaHoraria, String fonte) {
}
