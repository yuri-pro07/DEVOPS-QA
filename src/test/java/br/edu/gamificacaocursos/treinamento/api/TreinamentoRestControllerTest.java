package br.edu.gamificacaocursos.treinamento.api;

import br.edu.gamificacaocursos.treinamento.application.FilaDeTreinamentos;
import br.edu.gamificacaocursos.treinamento.application.TreinamentoService;
import br.edu.gamificacaocursos.treinamento.domain.AndamentoDoTreinamento;
import br.edu.gamificacaocursos.treinamento.domain.PedidoDeTreinamento;
import br.edu.gamificacaocursos.treinamento.domain.ResultadoDoTreinamento;
import br.edu.gamificacaocursos.treinamento.internal.mensageria.OuvinteDoAndamento;
import br.edu.gamificacaocursos.treinamento.internal.persistence.SolicitacaoDeTreinamentoRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.ConnectException;

import static br.edu.gamificacaocursos.treinamento.domain.StatusDoTreinamento.AGUARDANDO_ML;
import static br.edu.gamificacaocursos.treinamento.domain.StatusDoTreinamento.CONCLUIDO;
import static br.edu.gamificacaocursos.treinamento.domain.StatusDoTreinamento.EM_TREINAMENTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "treinamento.reenvio.carencia-segundos=0")
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class TreinamentoRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SolicitacaoDeTreinamentoRepository repositorio;

    @Autowired
    private TreinamentoService service;

    @Autowired
    private OuvinteDoAndamento ouvinte;

    @MockitoBean
    private FilaDeTreinamentos fila;

    @BeforeEach
    void prepara() {
        repositorio.deleteAll();
    }

    @Test
    @DisplayName("Aceita a solicitacao com 202, publica na fila e acompanha o andamento ate o modelo novo")
    void fluxoCompleto() throws Exception {
        String resposta = mockMvc.perform(post("/api/treinamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solicitante\":\"Enzo\",\"motivo\":\"retreino\",\"amostras\":5000,\"semente\":7}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("NA_FILA"))
                .andExpect(jsonPath("$.percentual").value(15))
                .andExpect(jsonPath("$.historico.length()").value(2))
                .andExpect(jsonPath("$.historico[1].detalhe").value("Publicada na fila de treinamentos do RabbitMQ"))
                .andReturn().getResponse().getContentAsString();
        Long id = ((Number) JsonPath.read(resposta, "$.id")).longValue();
        verify(fila).enviar(new PedidoDeTreinamento(id, 5000, 7));

        ouvinte.receber(new AndamentoDoTreinamento(id, AGUARDANDO_ML, "Servico de ML fora do ar", 1, null));
        ouvinte.receber(new AndamentoDoTreinamento(id, EM_TREINAMENTO, "Treinando", 2, null));
        ouvinte.receber(new AndamentoDoTreinamento(id, CONCLUIDO, "Modelo publicado", 2,
                new ResultadoDoTreinamento("20261124100005123", 7, 4000, 1000, 0.95, 0.94)));

        mockMvc.perform(get("/api/treinamentos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"))
                .andExpect(jsonPath("$.finalizado").value(true))
                .andExpect(jsonPath("$.tentativas").value(2))
                .andExpect(jsonPath("$.modelo.versao").value("20261124100005123"))
                .andExpect(jsonPath("$.modelo.acuracia").value(0.95))
                .andExpect(jsonPath("$.historico[*].status")
                        .value(org.hamcrest.Matchers.contains("RECEBIDA", "NA_FILA", "AGUARDANDO_ML",
                                "EM_TREINAMENTO", "CONCLUIDO")));
        mockMvc.perform(get("/api/treinamentos"))
                .andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    @DisplayName("Com o RabbitMQ fora do ar continua aceitando e o reenvio publica quando ele volta")
    void filaForaDoArEReenvio() throws Exception {
        doThrow(new AmqpConnectException(new ConnectException("recusada"))).when(fila).enviar(any());

        String resposta = mockMvc.perform(post("/api/treinamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solicitante\":\"Enzo\"}"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/treinamentos/")))
                .andExpect(jsonPath("$.status").value("RECEBIDA"))
                .andExpect(jsonPath("$.amostras").value(3000))
                .andReturn().getResponse().getContentAsString();
        Long id = ((Number) JsonPath.read(resposta, "$.id")).longValue();

        doNothing().when(fila).enviar(any());
        assertEquals(1, service.reenviarPendentes());
        assertEquals(0, service.reenviarPendentes());

        mockMvc.perform(get("/api/treinamentos/" + id)).andExpect(jsonPath("$.status").value("NA_FILA"));
    }

    @Test
    @DisplayName("Valida o pedido e recusa consulta de solicitacao inexistente")
    void validacao() throws Exception {
        mockMvc.perform(post("/api/treinamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solicitante\":\" \",\"amostras\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.solicitante").exists())
                .andExpect(jsonPath("$.erros.amostras").value("Use pelo menos 1000 amostras"));
        mockMvc.perform(get("/api/treinamentos/987654"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Solicitacao de treinamento nao encontrada: 987654"));
    }
}
