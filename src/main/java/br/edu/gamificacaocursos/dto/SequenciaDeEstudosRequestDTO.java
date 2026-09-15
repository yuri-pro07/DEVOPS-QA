package br.edu.gamificacaocursos.dto;

import jakarta.validation.constraints.NotBlank;

// DTO de entrada para criar a ofensiva de estudos de um aluno.
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
