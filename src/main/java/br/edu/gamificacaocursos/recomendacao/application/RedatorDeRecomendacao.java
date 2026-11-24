package br.edu.gamificacaocursos.recomendacao.application;

import br.edu.gamificacaocursos.recomendacao.domain.RespostaRedigida;
import br.edu.gamificacaocursos.recomendacao.domain.ResultadoDaPesquisa;

public interface RedatorDeRecomendacao {
    RespostaRedigida redigir(ResultadoDaPesquisa pesquisa);
}
