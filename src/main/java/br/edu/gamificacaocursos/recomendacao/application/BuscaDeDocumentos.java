package br.edu.gamificacaocursos.recomendacao.application;

import br.edu.gamificacaocursos.recomendacao.domain.TrechoDeDocumento;

import java.util.List;

public interface BuscaDeDocumentos {
    List<TrechoDeDocumento> buscar(String consulta, int quantidade);
}
