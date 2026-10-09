package br.edu.gamificacaocursos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GamificacaoCursosApplicationTest {
    @Test
    @DisplayName("BLUE: o contexto do Spring Boot sobe sem erro")
    void aplicacaoInicializaSemErro() {
        assertDoesNotThrow(() -> GamificacaoCursosApplication.main(new String[]{}));
    }

    @Test
    @DisplayName("BLUE: a classe de bootstrap pode ser instanciada")
    void classeDeBootstrapPodeSerInstanciada() {
        assertNotNull(new GamificacaoCursosApplication());
    }
}
