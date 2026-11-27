package br.edu.gamificacaocursos.treinamento.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReenvioAgendado {
    private final TreinamentoService service;

    public ReenvioAgendado(TreinamentoService service) {
        this.service = service;
    }

    @Scheduled(initialDelayString = "${treinamento.reenvio.intervalo-ms}",
            fixedDelayString = "${treinamento.reenvio.intervalo-ms}")
    public void executar() {
        service.reenviarPendentes();
    }
}
