package br.edu.gamificacaocursos.treinamento.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDateTime;

@Embeddable
public class EventoDoTreinamento {
    static final int TAMANHO_DO_DETALHE = 500;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusDoTreinamento status;

    @Column(nullable = false, length = TAMANHO_DO_DETALHE)
    private String detalhe;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm;

    protected EventoDoTreinamento() {
    }

    public EventoDoTreinamento(StatusDoTreinamento status, String detalhe, LocalDateTime registradoEm) {
        String texto = detalhe == null || detalhe.isBlank() ? status.descricao() : detalhe.trim();
        this.status = status;
        this.detalhe = texto.length() > TAMANHO_DO_DETALHE ? texto.substring(0, TAMANHO_DO_DETALHE) : texto;
        this.registradoEm = registradoEm;
    }

    public StatusDoTreinamento getStatus() {
        return status;
    }

    public String getDetalhe() {
        return detalhe;
    }

    public LocalDateTime getRegistradoEm() {
        return registradoEm;
    }
}
