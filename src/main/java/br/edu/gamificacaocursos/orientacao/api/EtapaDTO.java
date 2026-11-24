package br.edu.gamificacaocursos.orientacao.api;

import br.edu.gamificacaocursos.orientacao.domain.StatusDaEtapa;

import java.time.LocalDate;

public record EtapaDTO(Long id, int ordem, Long cursoId, String tituloDoCurso, int cargaHoraria, LocalDate prazo,
                       StatusDaEtapa status, LocalDate concluidaEm) {
}
