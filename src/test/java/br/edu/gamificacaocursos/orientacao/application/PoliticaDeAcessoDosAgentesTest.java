package br.edu.gamificacaocursos.orientacao.application;

import br.edu.gamificacaocursos.shared.web.AcessoNegadoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static br.edu.gamificacaocursos.orientacao.application.PoliticaDeAcessoDosAgentes.AGENTE_DE_ACOMPANHAMENTO;
import static br.edu.gamificacaocursos.orientacao.application.PoliticaDeAcessoDosAgentes.AGENTE_ORIENTADOR;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PoliticaDeAcessoDosAgentesTest {

    private final PoliticaDeAcessoDosAgentes politica = new PoliticaDeAcessoDosAgentes();

    @Test
    @DisplayName("O orientador so consulta; criar etapas e exclusivo do agente de acompanhamento")
    void separaPapeis() {
        assertDoesNotThrow(() -> politica.verificar(AGENTE_ORIENTADOR, "buscarDocumentos"));
        assertDoesNotThrow(() -> politica.verificar(AGENTE_DE_ACOMPANHAMENTO, "criarEtapas"));

        AcessoNegadoException erro = assertThrows(AcessoNegadoException.class,
                () -> politica.verificar(AGENTE_ORIENTADOR, "criarEtapas"));
        assertEquals("O agente-orientador nao tem permissao para usar a ferramenta criarEtapas", erro.getMessage());
    }

    @Test
    @DisplayName("Agente desconhecido nao tem nenhuma ferramenta, e dados pessoais nao sao ferramenta de ninguem")
    void negaPorPadrao() {
        assertThrows(AcessoNegadoException.class, () -> politica.verificar("agente-qualquer", "consultarCurso"));
        assertThrows(AcessoNegadoException.class, () -> politica.verificar(AGENTE_ORIENTADOR, "consultarEmail"));
    }

    @Test
    @DisplayName("Publica as permissoes em ordem para auditoria")
    void publicaPermissoes() {
        Map<String, List<String>> permissoes = politica.permissoes();

        assertEquals(List.of(AGENTE_DE_ACOMPANHAMENTO, AGENTE_ORIENTADOR), List.copyOf(permissoes.keySet()));
        assertEquals(List.of("buscarDocumentos", "consultarCurso", "consultarNivel"), permissoes.get(AGENTE_ORIENTADOR));
    }
}
