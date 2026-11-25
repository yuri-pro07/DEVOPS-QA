package br.edu.gamificacaocursos.treinamento.application;

import br.edu.gamificacaocursos.treinamento.api.EventoDoTreinamentoDTO;
import br.edu.gamificacaocursos.treinamento.api.TreinamentoRequestDTO;
import br.edu.gamificacaocursos.treinamento.api.TreinamentoResponseDTO;
import br.edu.gamificacaocursos.treinamento.domain.AndamentoDoTreinamento;
import br.edu.gamificacaocursos.treinamento.domain.SolicitacaoDeTreinamento;
import br.edu.gamificacaocursos.treinamento.internal.persistence.SolicitacaoDeTreinamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static br.edu.gamificacaocursos.treinamento.domain.StatusDoTreinamento.NA_FILA;
import static br.edu.gamificacaocursos.treinamento.domain.StatusDoTreinamento.RECEBIDA;

@Service
public class TreinamentoService {
    private static final Logger log = LoggerFactory.getLogger(TreinamentoService.class);

    private final SolicitacaoDeTreinamentoRepository repositorio;
    private final FilaDeTreinamentos fila;
    private final long carenciaEmSegundos;

    public TreinamentoService(SolicitacaoDeTreinamentoRepository repositorio, FilaDeTreinamentos fila,
                              @Value("${treinamento.reenvio.carencia-segundos:5}") long carenciaEmSegundos) {
        this.repositorio = repositorio;
        this.fila = fila;
        this.carenciaEmSegundos = carenciaEmSegundos;
    }

    public TreinamentoResponseDTO solicitar(TreinamentoRequestDTO pedido) {
        SolicitacaoDeTreinamento solicitacao = repositorio.save(SolicitacaoDeTreinamento.nova(pedido.solicitante(),
                pedido.motivo(), pedido.amostras(), pedido.semente(), LocalDateTime.now()));
        enviar(solicitacao);
        return consultar(solicitacao.getId());
    }

    public int reenviarPendentes() {
        LocalDateTime limite = LocalDateTime.now().minusSeconds(carenciaEmSegundos);
        int enviadas = 0;
        for (SolicitacaoDeTreinamento pendente : repositorio.findTop50ByEnviadaEmIsNullAndCriadaEmBeforeOrderByIdAsc(limite)) {
            if (!enviar(pendente)) {
                break;
            }
            enviadas++;
        }
        if (enviadas > 0) {
            log.info("{} solicitacao(oes) de treinamento reenviada(s) para a fila", enviadas);
        }
        return enviadas;
    }

    private boolean enviar(SolicitacaoDeTreinamento solicitacao) {
        try {
            fila.enviar(solicitacao.pedido());
        } catch (RuntimeException erro) {
            log.warn("Fila de treinamentos indisponivel. A solicitacao {} fica guardada para reenvio: {}",
                    solicitacao.getId(), erro.getMessage());
            return false;
        }
        repositorio.marcarComoEnviada(solicitacao.getId(), LocalDateTime.now(), RECEBIDA, NA_FILA);
        return true;
    }

    @Transactional
    public void registrarAndamento(AndamentoDoTreinamento andamento) {
        if (andamento.solicitacaoId() == null) {
            log.warn("Andamento de treinamento sem identificador descartado");
            return;
        }
        repositorio.findById(andamento.solicitacaoId()).ifPresentOrElse(
                solicitacao -> solicitacao.registrar(andamento, LocalDateTime.now()),
                () -> log.warn("Andamento recebido para a solicitacao inexistente {}", andamento.solicitacaoId()));
    }

    @Transactional(readOnly = true)
    public TreinamentoResponseDTO consultar(Long id) {
        return repositorio.findById(id).map(TreinamentoService::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Solicitacao de treinamento nao encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public List<TreinamentoResponseDTO> listar() {
        return repositorio.findTop20ByOrderByCriadaEmDescIdDesc().stream().map(TreinamentoService::toDTO).toList();
    }

    static TreinamentoResponseDTO toDTO(SolicitacaoDeTreinamento s) {
        List<EventoDoTreinamentoDTO> historico = s.linhaDoTempo().stream()
                .map(e -> new EventoDoTreinamentoDTO(e.getStatus(), e.getDetalhe(), e.getRegistradoEm()))
                .toList();
        return new TreinamentoResponseDTO(s.getId(), s.getSolicitante(), s.getMotivo(), s.getAmostras(), s.getSemente(),
                s.getStatus(), s.getStatus().percentual(), s.getStatus().descricao(), s.getStatus().finalizado(),
                s.getTentativas(), s.getCriadaEm(), s.getEnviadaEm(), s.getAtualizadaEm(), s.getConcluidaEm(),
                s.getResultado(), historico);
    }
}
