package br.edu.gamificacaocursos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Teste de fumaca da classe de bootstrap, acrescentado no BLUE para fechar a
 * cobertura sem deixar nenhuma classe em vermelho.
 */
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
