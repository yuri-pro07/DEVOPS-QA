package br.edu.gamificacaocursos.ofensiva.api;

import jakarta.validation.constraints.NotBlank;

public class SequenciaDeEstudosRequestDTO {
    @NotBlank(message = "Aluno e obrigatorio")
    private String aluno;

    public SequenciaDeEstudosRequestDTO() {
    }

    public SequenciaDeEstudosRequestDTO(String aluno) {
        this.aluno = aluno;
    }

    public String getAluno() {
        return aluno;
    }

    public void setAluno(String aluno) {
        this.aluno = aluno;
    }
}
