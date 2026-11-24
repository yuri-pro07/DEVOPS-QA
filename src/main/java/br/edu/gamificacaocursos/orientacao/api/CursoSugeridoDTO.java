package br.edu.gamificacaocursos.orientacao.api;

import br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia;

public record CursoSugeridoDTO(Long id, String titulo, NivelDeCompetencia nivel, int cargaHoraria) {
}
