package br.edu.gamificacaocursos.curso.internal.persistence;

import br.edu.gamificacaocursos.curso.domain.Curso;
import br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CursoRepository extends JpaRepository<Curso, Long> {
    boolean existsByTituloIgnoreCase(String titulo);

    @Query("select c from Curso c "
            + "where (:nivel is null or c.nivel = :nivel) "
            + "and (:area is null or lower(c.area) = lower(cast(:area as string))) "
            + "order by c.titulo")
    List<Curso> filtrar(@Param("nivel") NivelDeCompetencia nivel, @Param("area") String area);
}
