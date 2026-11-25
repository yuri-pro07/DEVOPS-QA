package br.edu.gamificacaocursos.treinamento.internal.persistence;

import br.edu.gamificacaocursos.treinamento.domain.SolicitacaoDeTreinamento;
import br.edu.gamificacaocursos.treinamento.domain.StatusDoTreinamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface SolicitacaoDeTreinamentoRepository extends JpaRepository<SolicitacaoDeTreinamento, Long> {
    List<SolicitacaoDeTreinamento> findTop20ByOrderByCriadaEmDescIdDesc();

    List<SolicitacaoDeTreinamento> findTop50ByEnviadaEmIsNullAndCriadaEmBeforeOrderByIdAsc(LocalDateTime limite);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update SolicitacaoDeTreinamento s set s.enviadaEm = :agora, "
            + "s.status = case when s.status = :recebida then :naFila else s.status end "
            + "where s.id = :id and s.enviadaEm is null")
    int marcarComoEnviada(@Param("id") Long id, @Param("agora") LocalDateTime agora,
                          @Param("recebida") StatusDoTreinamento recebida, @Param("naFila") StatusDoTreinamento naFila);
}
