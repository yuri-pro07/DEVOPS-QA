package br.edu.gamificacaocursos.orientacao.api;

import br.edu.gamificacaocursos.orientacao.domain.Acompanhamento;
import br.edu.gamificacaocursos.orientacao.domain.StatusDoPlano;
import br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia;

import java.time.LocalDateTime;
import java.util.List;

public record PlanoResponseDTO(Long id, Long colaboradorId, NivelDeCompetencia nivel, String objetivo,
                               StatusDoPlano status, String orientacao, String modo,
                               List<CursoSugeridoDTO> cursosSugeridos, List<EtapaDTO> etapas,
                               Acompanhamento acompanhamento, List<RegistroDeAgenteDTO> registrosDosAgentes,
                               LocalDateTime criadoEm, LocalDateTime decididoEm) {
}
