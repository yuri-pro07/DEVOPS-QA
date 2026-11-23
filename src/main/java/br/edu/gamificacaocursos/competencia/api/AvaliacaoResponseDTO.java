package br.edu.gamificacaocursos.competencia.api;

import br.edu.gamificacaocursos.competencia.domain.PerfilDeCompetencia;
import br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia;

import java.time.LocalDateTime;
import java.util.Map;

public record AvaliacaoResponseDTO(Long id, Long colaboradorId, NivelDeCompetencia nivel, double confianca,
                                   Map<String, Double> probabilidades, String modelo, LocalDateTime avaliadoEm,
                                   PerfilDeCompetencia perfil) {
}
