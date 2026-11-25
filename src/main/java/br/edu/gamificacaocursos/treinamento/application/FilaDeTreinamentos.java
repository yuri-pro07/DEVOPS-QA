package br.edu.gamificacaocursos.treinamento.application;

import br.edu.gamificacaocursos.treinamento.domain.PedidoDeTreinamento;

public interface FilaDeTreinamentos {
    void enviar(PedidoDeTreinamento pedido);
}
