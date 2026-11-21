package br.edu.gamificacaocursos.competencia.application;

import br.edu.gamificacaocursos.competencia.domain.Classificacao;
import br.edu.gamificacaocursos.competencia.domain.PerfilDeCompetencia;

public interface ClassificadorDeCompetencia {
    Classificacao classificar(PerfilDeCompetencia perfil);
}
