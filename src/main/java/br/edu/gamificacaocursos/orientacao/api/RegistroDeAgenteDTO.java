package br.edu.gamificacaocursos.orientacao.api;

import java.time.LocalDateTime;

public record RegistroDeAgenteDTO(String agente, String ferramenta, String detalhe, LocalDateTime registradoEm) {
}
