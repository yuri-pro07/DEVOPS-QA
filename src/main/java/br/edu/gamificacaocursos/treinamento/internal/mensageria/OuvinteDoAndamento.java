package br.edu.gamificacaocursos.treinamento.internal.mensageria;

import br.edu.gamificacaocursos.treinamento.application.TreinamentoService;
import br.edu.gamificacaocursos.treinamento.domain.AndamentoDoTreinamento;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OuvinteDoAndamento {
    private final TreinamentoService service;

    public OuvinteDoAndamento(TreinamentoService service) {
        this.service = service;
    }

    @RabbitListener(queues = "${treinamento.fila.andamento}")
    public void receber(AndamentoDoTreinamento andamento) {
        service.registrarAndamento(andamento);
    }
}
