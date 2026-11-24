package br.edu.gamificacaocursos.orientacao.application;

import br.edu.gamificacaocursos.orientacao.domain.PropostaDoModelo;

import java.util.Optional;

public interface PlanejadorComIa {
    Optional<PropostaDoModelo> planejar(String contexto, FerramentasDoOrientador ferramentas);

    String descricao();
}
