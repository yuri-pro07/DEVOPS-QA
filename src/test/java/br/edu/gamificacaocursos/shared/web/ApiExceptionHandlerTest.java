package br.edu.gamificacaocursos.shared.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    @DisplayName("Transicao de estado invalida vira 409 e ferramenta proibida para o agente vira 403")
    void conflitoEAcessoNegado() {
        ResponseEntity<Map<String, Object>> conflito = handler.handleConflito(new IllegalStateException("ja decidido"));
        ResponseEntity<Map<String, Object>> negado = handler.handleAcessoNegado(new AcessoNegadoException("proibido"));

        assertEquals(409, conflito.getStatusCode().value());
        assertEquals("ja decidido", conflito.getBody().get("erro"));
        assertEquals(403, negado.getStatusCode().value());
        assertEquals(403, negado.getBody().get("status"));
    }
}
