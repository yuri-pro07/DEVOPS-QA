package br.edu.gamificacaocursos.orientacao.internal.ia;

import br.edu.gamificacaocursos.curso.application.CursoService;
import br.edu.gamificacaocursos.orientacao.application.FerramentasDoOrientador;
import br.edu.gamificacaocursos.orientacao.application.PoliticaDeAcessoDosAgentes;
import br.edu.gamificacaocursos.orientacao.domain.PropostaDoModelo;
import br.edu.gamificacaocursos.recomendacao.application.RecomendacaoService;
import br.edu.gamificacaocursos.shared.ia.AssistenteDeIa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class PlanejadorComSpringAiTest {

    private ChatModel modelo;
    private FerramentasDoOrientador ferramentas;

    @BeforeEach
    void prepara() {
        modelo = mock(ChatModel.class);
        when(modelo.getDefaultOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        ferramentas = new FerramentasDoOrientador(1L, mock(RecomendacaoService.class), mock(CursoService.class),
                new PoliticaDeAcessoDosAgentes());
    }

    private PlanejadorComSpringAi planejador(String chave) {
        return new PlanejadorComSpringAi(new AssistenteDeIa(ChatClient.builder(modelo), chave, "gpt-4o-mini"));
    }

    @Test
    @DisplayName("Sem chave nao chama o modelo e o agente segue sem LLM")
    void semChave() {
        assertEquals(Optional.empty(), planejador("sem-chave").planejar("contexto", ferramentas));
        verify(modelo, never()).call(any(Prompt.class));
    }

    @Test
    @DisplayName("Com chave envia as instrucoes, oferece so as ferramentas de consulta e converte a resposta")
    void comChave() {
        when(modelo.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(
                new AssistantMessage("{\"cursoIds\":[6,4],\"orientacao\":\"Comece por mensageria.\"}")))));

        PlanejadorComSpringAi planejador = planejador("sk-teste");
        Optional<PropostaDoModelo> proposta = planejador.planejar("Nivel: INTERMEDIARIO", ferramentas);

        assertEquals(List.of(6L, 4L), proposta.orElseThrow().cursoIds());
        assertEquals("Comece por mensageria.", proposta.orElseThrow().orientacao());
        assertEquals("Spring AI com OpenAI gpt-4o-mini e tool calling", planejador.descricao());
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(modelo).call(prompt.capture());
        assertTrue(prompt.getValue().getContents().contains("Voce nao executa acoes"));
        ToolCallingChatOptions opcoes = assertInstanceOf(ToolCallingChatOptions.class, prompt.getValue().getOptions());
        assertEquals(List.of("buscarDocumentos", "consultarCurso", "consultarNivel"), opcoes.getToolCallbacks().stream()
                .map(ferramenta -> ferramenta.getToolDefinition().name()).sorted().toList());
    }

    @Test
    @DisplayName("Falha no modelo devolve vazio para o agente usar o plano dos documentos")
    void falha() {
        when(modelo.call(any(Prompt.class))).thenThrow(new IllegalStateException("timeout"));

        assertEquals(Optional.empty(), planejador("sk-teste").planejar("contexto", ferramentas));
    }
}
