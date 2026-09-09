package br.edu.exemplo.gamificacaocursos.domain;

import java.time.LocalDate;

public class SequenciaDeEstudos {

    static final int DIAS_PARA_RECOMPENSA = 7;
    static final int MOEDAS_POR_MARCO = 1;

    private final String aluno;
    private int diasConsecutivos;
    private LocalDate ultimoDiaEstudado;
    private int moedasConquistadas;

    public SequenciaDeEstudos(String aluno) {
        this.aluno = aluno;
        this.diasConsecutivos = 0;
        this.ultimoDiaEstudado = null;
        this.moedasConquistadas = 0;
    }

    public void registrarEstudo(LocalDate data) {
        throw new UnsupportedOperationException("registrarEstudo ainda nao implementado (TDD RED).");
    }

    public String getAluno() {
        return aluno;
    }

    public int getDiasConsecutivos() {
        return diasConsecutivos;
    }

    public LocalDate getUltimoDiaEstudado() {
        return ultimoDiaEstudado;
    }

    public int getMoedasConquistadas() {
        return moedasConquistadas;
    }
}
