package br.edu.gamificacaocursos.treinamento.internal.mensageria;

import br.edu.gamificacaocursos.treinamento.application.FilaDeTreinamentos;
import br.edu.gamificacaocursos.treinamento.domain.PedidoDeTreinamento;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitFilaDeTreinamentos implements FilaDeTreinamentos {
    private final RabbitTemplate rabbit;
    private final String fila;

    public RabbitFilaDeTreinamentos(RabbitTemplate rabbit, @Value("${treinamento.fila.solicitacoes}") String fila) {
        this.rabbit = rabbit;
        this.fila = fila;
    }

    @Override
    public void enviar(PedidoDeTreinamento pedido) {
        rabbit.convertAndSend("", fila, pedido);
    }
}
