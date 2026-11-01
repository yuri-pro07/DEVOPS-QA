package br.edu.gamificacaocursos.ranking.internal.persistence;

import br.edu.gamificacaocursos.ofensiva.domain.SequenciaDeEstudos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RankingRepository extends Repository<SequenciaDeEstudos, Long> {

    @Query(value = "select new br.edu.gamificacaocursos.ranking.internal.persistence.LinhaDoRanking("
            + "s.aluno, s.moedasConquistadas, s.diasConsecutivos) "
            + "from SequenciaDeEstudos s "
            + "order by s.moedasConquistadas desc, s.diasConsecutivos desc, s.aluno asc",
            countQuery = "select count(s) from SequenciaDeEstudos s")
    Page<LinhaDoRanking> buscarRanking(Pageable pagina);

    @Query("select new br.edu.gamificacaocursos.ranking.internal.persistence.LinhaDoRanking("
            + "s.aluno, s.moedasConquistadas, s.diasConsecutivos) "
            + "from SequenciaDeEstudos s where s.aluno = :aluno")
    Optional<LinhaDoRanking> buscarPorAluno(@Param("aluno") String aluno);

    @Query("select count(s) from SequenciaDeEstudos s "
            + "where s.moedasConquistadas > :moedas "
            + "or (s.moedasConquistadas = :moedas and s.diasConsecutivos > :dias) "
            + "or (s.moedasConquistadas = :moedas and s.diasConsecutivos = :dias and s.aluno < :aluno)")
    long contarAFrente(@Param("moedas") int moedas, @Param("dias") int dias, @Param("aluno") String aluno);
}
