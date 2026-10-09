package br.edu.gamificacaocursos.ofensiva.internal.persistence;

import br.edu.gamificacaocursos.ofensiva.domain.SequenciaDeEstudos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SequenciaDeEstudosRepository extends JpaRepository<SequenciaDeEstudos, Long> {
    Optional<SequenciaDeEstudos> findByAluno(String aluno);

    boolean existsByAluno(String aluno);
}
