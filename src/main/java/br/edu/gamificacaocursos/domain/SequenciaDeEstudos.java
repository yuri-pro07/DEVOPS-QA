package br.edu.gamificacaocursos.domain;

import java.time.LocalDate;

/**
 * Ofensiva de estudos do aluno: conta os dias consecutivos de estudo e premia a
 * regularidade quando um marco e atingido.
 */
public class SequenciaDeEstudos {

    static final int DIAS_PARA_RECOMPENSA = 7;
    static final int MOEDAS_POR_MARCO = 1;
    static final int PRIMEIRO_DIA_DA_SEQUENCIA = 1;

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

    /**
     * Registra que o aluno estudou na data informada: a sequencia avanca quando o
     * estudo ocorre no dia seguinte ao ultimo, e recomeca do zero caso contrario.
     */
    public void registrarEstudo(LocalDate data) {
        this.diasConsecutivos = ehDiaSeguinteAoUltimoEstudo(data)
                ? diasConsecutivos + 1
                : PRIMEIRO_DIA_DA_SEQUENCIA;
        this.ultimoDiaEstudado = data;

        premiarSeAtingiuMarco();
    }

    private boolean ehDiaSeguinteAoUltimoEstudo(LocalDate data) {
        return ultimoDiaEstudado != null && data.equals(ultimoDiaEstudado.plusDays(1));
    }

    private void premiarSeAtingiuMarco() {
        if (diasConsecutivos == DIAS_PARA_RECOMPENSA) {
            this.moedasConquistadas += MOEDAS_POR_MARCO;
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
