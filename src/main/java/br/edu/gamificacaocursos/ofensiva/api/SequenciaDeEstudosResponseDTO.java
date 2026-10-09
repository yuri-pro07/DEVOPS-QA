package br.edu.gamificacaocursos.ofensiva.api;

import java.time.LocalDate;

public class SequenciaDeEstudosResponseDTO {
    private Long id;
    private String aluno;
    private int diasConsecutivos;
    private LocalDate ultimoDiaEstudado;
    private int moedasConquistadas;

    public SequenciaDeEstudosResponseDTO() {
    }

    public SequenciaDeEstudosResponseDTO(Long id, String aluno, int diasConsecutivos,
                                         LocalDate ultimoDiaEstudado, int moedasConquistadas) {
        this.id = id;
        this.aluno = aluno;
        this.diasConsecutivos = diasConsecutivos;
        this.ultimoDiaEstudado = ultimoDiaEstudado;
        this.moedasConquistadas = moedasConquistadas;
    }

    public Long getId() {
        return id;
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
