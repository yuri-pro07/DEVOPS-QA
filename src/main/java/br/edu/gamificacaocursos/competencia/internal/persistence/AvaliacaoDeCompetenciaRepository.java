package br.edu.gamificacaocursos.competencia.internal.persistence;

import br.edu.gamificacaocursos.competencia.domain.AvaliacaoDeCompetencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvaliacaoDeCompetenciaRepository extends JpaRepository<AvaliacaoDeCompetencia, Long> {
    List<AvaliacaoDeCompetencia> findByColaboradorIdOrderByAvaliadoEmDescIdDesc(Long colaboradorId);
}
