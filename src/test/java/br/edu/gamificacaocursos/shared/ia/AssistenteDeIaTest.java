package br.edu.gamificacaocursos.shared.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AssistenteDeIaTest {

    private final ChatClient.Builder builder = ChatClient.builder(mock(ChatModel.class));

    @Test
    @DisplayName("Sem chave da OpenAI a IA fica desligada e os modulos usam o modo sem LLM")
    void semChave() {
        assertFalse(new AssistenteDeIa(builder, "sem-chave", "gpt-4o-mini").habilitada());
        assertFalse(new AssistenteDeIa(builder, "  ", "gpt-4o-mini").habilitada());
        assertFalse(new AssistenteDeIa(builder, null, "gpt-4o-mini").habilitada());
    }

    @Test
    @DisplayName("Com chave configurada a IA fica ligada e informa o modelo usado")
    void comChave() {
        AssistenteDeIa ia = new AssistenteDeIa(builder, "sk-teste", "gpt-4o-mini");

        assertTrue(ia.habilitada());
        assertNotNull(ia.cliente());
        assertEquals("Spring AI com OpenAI gpt-4o-mini", ia.descricao());
    }
}
