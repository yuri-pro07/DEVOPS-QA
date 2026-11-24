package br.edu.gamificacaocursos.orientacao.internal.persistence;

import br.edu.gamificacaocursos.orientacao.domain.PlanoDeEstudos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanoDeEstudosRepository extends JpaRepository<PlanoDeEstudos, Long> {
    List<PlanoDeEstudos> findByColaboradorIdOrderByCriadoEmDescIdDesc(Long colaboradorId);
}
