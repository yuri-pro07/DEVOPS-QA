package br.edu.gamificacaocursos.ofensiva.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

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
