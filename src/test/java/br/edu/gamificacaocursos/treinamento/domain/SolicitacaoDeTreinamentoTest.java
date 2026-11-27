package br.edu.gamificacaocursos.treinamento.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static br.edu.gamificacaocursos.treinamento.domain.StatusDoTreinamento.AGUARDANDO_ML;
import static br.edu.gamificacaocursos.treinamento.domain.StatusDoTreinamento.CONCLUIDO;
import static br.edu.gamificacaocursos.treinamento.domain.StatusDoTreinamento.EM_TREINAMENTO;
import static br.edu.gamificacaocursos.treinamento.domain.StatusDoTreinamento.FALHOU;
import static br.edu.gamificacaocursos.treinamento.domain.StatusDoTreinamento.NA_FILA;
import static br.edu.gamificacaocursos.treinamento.domain.StatusDoTreinamento.RECEBIDA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolicitacaoDeTreinamentoTest {
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 24, 10, 0);
    private static final ResultadoDoTreinamento MODELO =
            new ResultadoDoTreinamento("20261124100005123", 7, 4000, 1000, 0.95, 0.94);

    private static SolicitacaoDeTreinamento nova() {
        return SolicitacaoDeTreinamento.nova("  Enzo ", "  retreino mensal ", 5000, 7, INICIO);
    }

    private static AndamentoDoTreinamento andamento(StatusDoTreinamento status, Integer tentativa) {
        return new AndamentoDoTreinamento(1L, status, "detalhe " + status, tentativa,
                status == CONCLUIDO ? MODELO : null);
    }

    @Test
    @DisplayName("Nova solicitacao comeca recebida, com valores padrao e o primeiro evento do historico")
    void novaSolicitacao() {
        SolicitacaoDeTreinamento solicitacao = nova();
        SolicitacaoDeTreinamento padrao = SolicitacaoDeTreinamento.nova("Ana", " ", null, null, INICIO);

        assertEquals("Enzo", solicitacao.getSolicitante());
        assertEquals("retreino mensal", solicitacao.getMotivo());
        assertEquals(RECEBIDA, solicitacao.getStatus());
        assertEquals(new PedidoDeTreinamento(null, 5000, 7), solicitacao.pedido());
        assertEquals(INICIO, solicitacao.getAtualizadaEm());
        assertEquals("Solicitacao registrada por Enzo", solicitacao.linhaDoTempo().get(0).getDetalhe());
        assertEquals(3000, padrao.getAmostras());
        assertNull(padrao.getMotivo());
        assertNull(padrao.getSemente());
        assertNull(padrao.getResultado());
        assertThrows(IllegalArgumentException.class, () -> SolicitacaoDeTreinamento.nova(" ", null, null, null, INICIO));
        assertThrows(IllegalArgumentException.class, () -> SolicitacaoDeTreinamento.nova(null, null, null, null, INICIO));
    }

    @Test
    @DisplayName("Andamento informado pelo worker avanca o status, guarda as tentativas e o modelo gerado")
    void registraOAndamentoAteConcluir() {
        SolicitacaoDeTreinamento solicitacao = nova();

        assertTrue(solicitacao.registrar(andamento(AGUARDANDO_ML, 3), INICIO.plusSeconds(1)));
        assertTrue(solicitacao.registrar(andamento(EM_TREINAMENTO, null), INICIO.plusSeconds(2)));
        assertEquals(3, solicitacao.getTentativas());
        assertNull(solicitacao.getConcluidaEm());
        assertTrue(solicitacao.registrar(andamento(CONCLUIDO, 4), INICIO.plusSeconds(3)));

        assertEquals(CONCLUIDO, solicitacao.getStatus());
        assertEquals(4, solicitacao.getTentativas());
        assertEquals(MODELO, solicitacao.getResultado());
        assertEquals(INICIO.plusSeconds(3), solicitacao.getConcluidaEm());
        assertEquals(List.of(RECEBIDA, AGUARDANDO_ML, EM_TREINAMENTO, CONCLUIDO),
                solicitacao.linhaDoTempo().stream().map(EventoDoTreinamento::getStatus).toList());
    }

    @Test
    @DisplayName("Depois de finalizada a solicitacao ignora eventos atrasados ou repetidos")
    void ignoraEventosDepoisDeFinalizada() {
        SolicitacaoDeTreinamento solicitacao = nova();
        solicitacao.registrar(andamento(FALHOU, 1), INICIO.plusSeconds(1));

        assertFalse(solicitacao.registrar(andamento(EM_TREINAMENTO, 2), INICIO.plusSeconds(2)));
        assertEquals(FALHOU, solicitacao.getStatus());
        assertEquals(2, solicitacao.linhaDoTempo().size());
        assertNull(solicitacao.getResultado());
    }

    @Test
    @DisplayName("Recusa status que o worker nao pode informar e conclusao sem modelo")
    void recusaAndamentoInvalido() {
        SolicitacaoDeTreinamento solicitacao = nova();

        assertThrows(IllegalArgumentException.class, () -> solicitacao.registrar(andamento(NA_FILA, 1), INICIO));
        assertThrows(IllegalArgumentException.class, () -> solicitacao.registrar(andamento(null, 1), INICIO));
        assertThrows(IllegalArgumentException.class,
                () -> solicitacao.registrar(new AndamentoDoTreinamento(1L, CONCLUIDO, "ok", 1, null), INICIO));
        assertEquals(RECEBIDA, solicitacao.getStatus());
    }

    @Test
    @DisplayName("A linha do tempo inclui o envio para a fila na ordem em que aconteceu")
    void linhaDoTempoComEnvio() {
        SolicitacaoDeTreinamento solicitacao = nova();
        solicitacao.registrar(andamento(EM_TREINAMENTO, 1), INICIO.plusSeconds(4));
        ReflectionTestUtils.setField(solicitacao, "enviadaEm", INICIO.plusSeconds(2));

        List<EventoDoTreinamento> eventos = solicitacao.linhaDoTempo();

        assertEquals(List.of(RECEBIDA, NA_FILA, EM_TREINAMENTO),
                eventos.stream().map(EventoDoTreinamento::getStatus).toList());
        assertEquals("Publicada na fila de treinamentos do RabbitMQ", eventos.get(1).getDetalhe());
        assertEquals(INICIO.plusSeconds(2), solicitacao.getEnviadaEm());
    }

    @Test
    @DisplayName("Evento sem detalhe usa a descricao do status e detalhe longo e cortado")
    void detalheDoEvento() {
        assertEquals(FALHOU.descricao(), new EventoDoTreinamento(FALHOU, " ", INICIO).getDetalhe());
        assertEquals(CONCLUIDO.descricao(), new EventoDoTreinamento(CONCLUIDO, null, INICIO).getDetalhe());
        assertEquals(500, new EventoDoTreinamento(FALHOU, "x".repeat(600), INICIO).getDetalhe().length());
    }

    @Test
    @DisplayName("Percentual e situacao de cada status")
    void percentuais() {
        assertEquals(5, RECEBIDA.percentual());
        assertEquals(15, NA_FILA.percentual());
        assertEquals(100, CONCLUIDO.percentual());
        assertTrue(FALHOU.finalizado());
        assertFalse(AGUARDANDO_ML.finalizado());
        assertFalse(RECEBIDA.informadoPeloWorker());
        assertTrue(AGUARDANDO_ML.informadoPeloWorker());
    }
}
