package br.edu.gamificacaocursos.orientacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;

@Embeddable
public class RegistroDeAgente {
    static final int TAMANHO_DO_DETALHE = 500;

    @Column(nullable = false, length = 60)
    private String agente;

    @Column(nullable = false, length = 60)
    private String ferramenta;

    @Column(nullable = false, length = TAMANHO_DO_DETALHE)
    private String detalhe;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm;

    protected RegistroDeAgente() {
    }

    public RegistroDeAgente(String agente, String ferramenta, String detalhe, LocalDateTime registradoEm) {
        this.agente = agente;
        this.ferramenta = ferramenta;
        this.detalhe = detalhe.length() > TAMANHO_DO_DETALHE ? detalhe.substring(0, TAMANHO_DO_DETALHE) : detalhe;
        this.registradoEm = registradoEm;
    }

    public String getAgente() {
        return agente;
    }

    public String getFerramenta() {
        return ferramenta;
    }

    public String getDetalhe() {
        return detalhe;
    }

    public LocalDateTime getRegistradoEm() {
        return registradoEm;
    }
}
