package br.edu.gamificacaocursos.curso.api;

import br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia;

public record CursoResponseDTO(Long id, String titulo, String descricao, String area,
                               NivelDeCompetencia nivel, int cargaHoraria) {
}
