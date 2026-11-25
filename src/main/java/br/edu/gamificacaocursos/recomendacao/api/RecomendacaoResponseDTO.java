package br.edu.gamificacaocursos.recomendacao.api;

import br.edu.gamificacaocursos.recomendacao.domain.TrechoDeDocumento;
import br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia;

import java.util.List;

public record RecomendacaoResponseDTO(Long colaboradorId, NivelDeCompetencia nivel, String objetivo,
                                      String resposta, String modo, List<CursoIndicadoDTO> cursos,
                                      List<TrechoDeDocumento> fontes) {
}
