package br.edu.gamificacaocursos.colaborador.internal.persistence;

import br.edu.gamificacaocursos.colaborador.domain.Colaborador;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColaboradorRepository extends JpaRepository<Colaborador, Long> {
    boolean existsByEmail(String email);
}
