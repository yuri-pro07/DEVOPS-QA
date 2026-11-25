package br.edu.gamificacaocursos.treinamento.application;

import br.edu.gamificacaocursos.treinamento.api.TreinamentoRequestDTO;
import br.edu.gamificacaocursos.treinamento.api.TreinamentoResponseDTO;
import br.edu.gamificacaocursos.treinamento.domain.AndamentoDoTreinamento;
import br.edu.gamificacaocursos.treinamento.domain.PedidoDeTreinamento;
import br.edu.gamificacaocursos.treinamento.domain.SolicitacaoDeTreinamento;
import br.edu.gamificacaocursos.treinamento.internal.persistence.SolicitacaoDeTreinamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.ConnectException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static br.edu.gamificacaocursos.treinamento.domain.StatusDoTreinamento.AGUARDANDO_ML;
import static br.edu.gamificacaocursos.treinamento.domain.StatusDoTreinamento.NA_FILA;
import static br.edu.gamificacaocursos.treinamento.domain.StatusDoTreinamento.RECEBIDA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreinamentoServiceTest {

    @Mock
    private SolicitacaoDeTreinamentoRepository repositorio;

    @Mock
    private FilaDeTreinamentos fila;

    private TreinamentoService service;

    @BeforeEach
    void prepara() {
        service = new TreinamentoService(repositorio, fila, 5);
    }

    private static SolicitacaoDeTreinamento salva(Long id) {
        SolicitacaoDeTreinamento solicitacao = SolicitacaoDeTreinamento.nova("Enzo", null, 5000, 7, LocalDateTime.now());
        ReflectionTestUtils.setField(solicitacao, "id", id);
        return solicitacao;
    }

    @Test
    @DisplayName("Grava a solicitacao, publica na fila e marca como enviada")
    void solicitaEEnviaParaAFila() {
        SolicitacaoDeTreinamento solicitacao = salva(1L);
        when(repositorio.save(any())).thenReturn(solicitacao);
        when(repositorio.findById(1L)).thenReturn(Optional.of(solicitacao));

        TreinamentoResponseDTO resposta = service.solicitar(new TreinamentoRequestDTO("Enzo", null, 5000, 7));

        verify(fila).enviar(new PedidoDeTreinamento(1L, 5000, 7));
        verify(repositorio).marcarComoEnviada(eq(1L), any(), eq(RECEBIDA), eq(NA_FILA));
        assertEquals(1L, resposta.id());
        assertEquals(5000, resposta.amostras());
    }

    @Test
    @DisplayName("Com o RabbitMQ fora do ar a solicitacao continua aceita e fica guardada para reenvio")
    void filaIndisponivelNaoPerdeASolicitacao() {
        SolicitacaoDeTreinamento solicitacao = salva(2L);
        when(repositorio.save(any())).thenReturn(solicitacao);
        when(repositorio.findById(2L)).thenReturn(Optional.of(solicitacao));
        doThrow(new AmqpConnectException(new ConnectException("recusada"))).when(fila).enviar(any());

        TreinamentoResponseDTO resposta = service.solicitar(new TreinamentoRequestDTO("Enzo", null, null, null));

        assertEquals(RECEBIDA, resposta.status());
        verify(repositorio, never()).marcarComoEnviada(any(), any(), any(), any());
    }

    @Test
    @DisplayName("O reenvio publica as pendentes e para na primeira falha da fila")
    void reenviaPendentes() {
        when(repositorio.findTop50ByEnviadaEmIsNullAndCriadaEmBeforeOrderByIdAsc(any()))
                .thenReturn(List.of(salva(3L), salva(4L), salva(5L)));
        doAnswer(invocacao -> {
            if (invocacao.<PedidoDeTreinamento>getArgument(0).solicitacaoId() == 4L) {
                throw new AmqpConnectException(new ConnectException("caiu"));
            }
            return null;
        }).when(fila).enviar(any());

        assertEquals(1, service.reenviarPendentes());

        verify(fila, times(2)).enviar(any());
        verify(repositorio).marcarComoEnviada(eq(3L), any(), eq(RECEBIDA), eq(NA_FILA));
    }

    @Test
    @DisplayName("Sem pendentes o reenvio nao publica nada")
    void semPendentes() {
        when(repositorio.findTop50ByEnviadaEmIsNullAndCriadaEmBeforeOrderByIdAsc(any())).thenReturn(List.of());

        assertEquals(0, service.reenviarPendentes());
        verifyNoInteractions(fila);
    }

    @Test
    @DisplayName("Andamento atualiza a solicitacao; evento sem id ou de solicitacao inexistente e descartado")
    void registraAndamento() {
        SolicitacaoDeTreinamento solicitacao = salva(6L);
        when(repositorio.findById(6L)).thenReturn(Optional.of(solicitacao));
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        service.registrarAndamento(new AndamentoDoTreinamento(6L, AGUARDANDO_ML, "ML fora do ar", 1, null));
        service.registrarAndamento(new AndamentoDoTreinamento(99L, AGUARDANDO_ML, "x", 1, null));
        service.registrarAndamento(new AndamentoDoTreinamento(null, AGUARDANDO_ML, "x", 1, null));

        assertEquals(AGUARDANDO_ML, solicitacao.getStatus());
        verify(repositorio, times(2)).findById(any());
    }

    @Test
    @DisplayName("Lista as recentes e recusa consulta de solicitacao inexistente")
    void listaEConsulta() {
        when(repositorio.findTop20ByOrderByCriadaEmDescIdDesc()).thenReturn(List.of(salva(8L), salva(7L)));
        when(repositorio.findById(50L)).thenReturn(Optional.empty());

        assertEquals(List.of(8L, 7L), service.listar().stream().map(TreinamentoResponseDTO::id).toList());
        assertThrows(IllegalArgumentException.class, () -> service.consultar(50L));
    }

    @Test
    @DisplayName("O agendamento chama o reenvio das pendentes")
    void agendamento() {
        TreinamentoService mockDoServico = mock(TreinamentoService.class);

        new ReenvioAgendado(mockDoServico).executar();

        verify(mockDoServico).reenviarPendentes();
    }
}
