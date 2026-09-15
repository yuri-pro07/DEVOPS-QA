package br.edu.gamificacaocursos.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// DTO de entrada para registrar um dia de estudo.
public class RegistrarEstudoRequestDTO {

    @NotNull(message = "Data e obrigatoria")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate data;

    public RegistrarEstudoRequestDTO() {
    }

    public RegistrarEstudoRequestDTO(LocalDate data) {
        this.data = data;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }
}
