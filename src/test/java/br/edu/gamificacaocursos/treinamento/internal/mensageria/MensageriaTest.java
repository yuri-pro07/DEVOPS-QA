package br.edu.gamificacaocursos.treinamento.internal.mensageria;

import br.edu.gamificacaocursos.treinamento.application.TreinamentoService;
import br.edu.gamificacaocursos.treinamento.domain.AndamentoDoTreinamento;
import br.edu.gamificacaocursos.treinamento.domain.PedidoDeTreinamento;
import br.edu.gamificacaocursos.treinamento.domain.ResultadoDoTreinamento;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;

import java.nio.charset.StandardCharsets;

import static br.edu.gamificacaocursos.treinamento.domain.StatusDoTreinamento.CONCLUIDO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MensageriaTest {
    private final MensageriaConfig config = new MensageriaConfig();

    @Test
    @DisplayName("As filas sao duraveis para nao perder solicitacoes se o RabbitMQ reiniciar")
    void filasDuraveis() {
        Queue solicitacoes = config.filaDeSolicitacoesDeTreinamento("treinamentos.solicitados");
        Queue andamento = config.filaDeAndamentoDosTreinamentos("treinamentos.andamento");

        assertEquals("treinamentos.solicitados", solicitacoes.getName());
        assertEquals("treinamentos.andamento", andamento.getName());
        assertTrue(solicitacoes.isDurable() && andamento.isDurable());
    }

    @Test
    @DisplayName("Converte o JSON publicado pelo worker Python no andamento do treinamento")
    void converteOEventoDoWorker() {
        MessageConverter conversor = config.conversorJson(new ObjectMapper());
        MessageProperties propriedades = new MessageProperties();
        propriedades.setContentType("application/json");
        propriedades.setInferredArgumentType(AndamentoDoTreinamento.class);
        String json = "{\"solicitacaoId\":3,\"status\":\"CONCLUIDO\",\"detalhe\":\"ok\",\"tentativa\":2,"
                + "\"resultado\":{\"versao\":\"v1\",\"semente\":7,\"amostrasDeTreino\":4000,\"amostrasDeTeste\":1000,"
                + "\"acuracia\":0.95,\"f1Macro\":0.94}}";

        Object convertido = conversor.fromMessage(new Message(json.getBytes(StandardCharsets.UTF_8), propriedades));

        assertEquals(new AndamentoDoTreinamento(3L, CONCLUIDO, "ok", 2,
                new ResultadoDoTreinamento("v1", 7, 4000, 1000, 0.95, 0.94)), convertido);
    }

    @Test
    @DisplayName("Publica o pedido na fila de solicitacoes pela exchange padrao")
    void publicaNaFila() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        PedidoDeTreinamento pedido = new PedidoDeTreinamento(1L, 3000, null);

        new RabbitFilaDeTreinamentos(rabbit, "treinamentos.solicitados").enviar(pedido);

        verify(rabbit).convertAndSend("", "treinamentos.solicitados", pedido);
    }

    @Test
    @DisplayName("O ouvinte repassa o andamento recebido para o servico")
    void ouvinteRepassaOAndamento() {
        TreinamentoService service = mock(TreinamentoService.class);
        AndamentoDoTreinamento andamento = new AndamentoDoTreinamento(1L, CONCLUIDO, "ok", 1, null);

        new OuvinteDoAndamento(service).receber(andamento);

        verify(service).registrarAndamento(andamento);
    }
}
