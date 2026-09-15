package br.edu.gamificacaocursos.repository;

import br.edu.gamificacaocursos.domain.SequenciaDeEstudos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Camada: REPOSITORY.
// Ao estender JpaRepository ganhamos prontos: findAll(), findById(...),
// save(...), deleteById(...). Nao precisamos escrever SQL.
public interface SequenciaDeEstudosRepository extends JpaRepository<SequenciaDeEstudos, Long> {

    // Spring Data monta a consulta a partir do nome do metodo.
    Optional<SequenciaDeEstudos> findByAluno(String aluno);

    boolean existsByAluno(String aluno);
}
