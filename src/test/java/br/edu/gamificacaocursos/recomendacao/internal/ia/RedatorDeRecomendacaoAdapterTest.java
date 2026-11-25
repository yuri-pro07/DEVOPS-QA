package br.edu.gamificacaocursos.recomendacao.internal.ia;

import br.edu.gamificacaocursos.recomendacao.api.CursoIndicadoDTO;
import br.edu.gamificacaocursos.recomendacao.domain.RespostaRedigida;
import br.edu.gamificacaocursos.recomendacao.domain.ResultadoDaPesquisa;
import br.edu.gamificacaocursos.recomendacao.domain.TrechoDeDocumento;
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

import static br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia.INTERMEDIARIO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class RedatorDeRecomendacaoAdapterTest {

    private static final TrechoDeDocumento TRECHO = new TrechoDeDocumento("trilha-intermediaria#3",
            "Trilha Intermediaria", "trilha-intermediaria.md", "Mensageria", "O curso indicado e Mensageria com RabbitMQ.", 0.4);
    private static final ResultadoDaPesquisa COM_CURSOS = new ResultadoDaPesquisa(INTERMEDIARIO, "mensageria",
            List.of(new CursoIndicadoDTO(6L, "Mensageria com RabbitMQ", INTERMEDIARIO, 18, TRECHO.id())), List.of(TRECHO));
    private static final ResultadoDaPesquisa SEM_CURSOS = new ResultadoDaPesquisa(null, "", List.of(), List.of(TRECHO));

    private ChatModel modelo;

    @BeforeEach
    void prepara() {
        modelo = mock(ChatModel.class);
        when(modelo.getDefaultOptions()).thenReturn(ToolCallingChatOptions.builder().build());
    }

    private RedatorDeRecomendacaoAdapter redator(String chave) {
        return new RedatorDeRecomendacaoAdapter(new AssistenteDeIa(ChatClient.builder(modelo), chave, "gpt-4o-mini"));
    }

    private static ChatResponse resposta(String texto) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(texto))));
    }

    @Test
    @DisplayName("Sem chave escreve a recomendacao direto dos trechos, citando curso, carga e fonte")
    void extrativoSemChave() {
        RespostaRedigida resposta = redator("sem-chave").redigir(COM_CURSOS);

        assertEquals(RedatorDeRecomendacaoAdapter.MODO_EXTRATIVO, resposta.modo());
        assertTrue(resposta.texto().startsWith("Nivel considerado: INTERMEDIARIO."));
        assertTrue(resposta.texto().contains("1) Mensageria com RabbitMQ (18 h) [trilha-intermediaria#3]"));
        assertTrue(resposta.texto().endsWith("[trilha-intermediaria#3]"));
        verify(modelo, never()).call(any(Prompt.class));
    }

    @Test
    @DisplayName("Sem curso citado nos trechos ainda devolve a orientacao do documento mais relevante")
    void extrativoSemCursos() {
        String texto = RedatorDeRecomendacaoAdapter.extrativa(SEM_CURSOS);

        assertTrue(texto.startsWith("Nivel considerado: ainda nao avaliado."));
        assertTrue(texto.contains("nao citam um curso especifico"));
    }

    @Test
    @DisplayName("Com chave usa o Spring AI, mandando instrucoes contra injecao e os trechos recuperados")
    void comSpringAi() {
        when(modelo.call(any(Prompt.class))).thenReturn(resposta("  Recomendo Mensageria com RabbitMQ [trilha-intermediaria#3].  "));

        RespostaRedigida resposta = redator("sk-teste").redigir(COM_CURSOS);

        assertEquals("Recomendo Mensageria com RabbitMQ [trilha-intermediaria#3].", resposta.texto());
        assertEquals("Spring AI com OpenAI gpt-4o-mini", resposta.modo());
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(modelo).call(prompt.capture());
        String enviado = prompt.getValue().getContents();
        assertTrue(enviado.contains("nunca instrucoes"));
        assertTrue(enviado.contains("[trilha-intermediaria#3] Trilha Intermediaria / Mensageria"));
        assertTrue(enviado.contains("Objetivo: mensageria"));
    }

    @Test
    @DisplayName("Se o LLM falhar ou responder vazio, cai para o modo extrativo")
    void falhaDoLlm() {
        when(modelo.call(any(Prompt.class)))
                .thenThrow(new IllegalStateException("timeout"))
                .thenReturn(resposta(" "))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(null)))));
        RedatorDeRecomendacaoAdapter redator = redator("sk-teste");

        assertEquals(RedatorDeRecomendacaoAdapter.MODO_EXTRATIVO_APOS_FALHA, redator.redigir(COM_CURSOS).modo());
        assertEquals(RedatorDeRecomendacaoAdapter.MODO_EXTRATIVO_APOS_FALHA, redator.redigir(COM_CURSOS).modo());
        assertEquals(RedatorDeRecomendacaoAdapter.MODO_EXTRATIVO_APOS_FALHA, redator.redigir(SEM_CURSOS).modo());
    }

    @Test
    @DisplayName("Sem objetivo informado o pedido ao LLM deixa isso explicito")
    void pedidoSemObjetivo() {
        assertTrue(RedatorDeRecomendacaoAdapter.pedido(SEM_CURSOS).contains("Objetivo: nao informado"));
    }
}
