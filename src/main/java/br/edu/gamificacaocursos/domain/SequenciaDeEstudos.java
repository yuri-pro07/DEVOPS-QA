package br.edu.gamificacaocursos.domain;

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
        // TDD - GREEN: implementacao minima para os 3 cenarios de aceite da US1.
        if (ultimoDiaEstudado != null && data.equals(ultimoDiaEstudado.plusDays(1))) {
            diasConsecutivos = diasConsecutivos + 1;
        } else {
            diasConsecutivos = 1;
        }

        ultimoDiaEstudado = data;

        if (diasConsecutivos == DIAS_PARA_RECOMPENSA) {
            moedasConquistadas = moedasConquistadas + MOEDAS_POR_MARCO;
        }
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
