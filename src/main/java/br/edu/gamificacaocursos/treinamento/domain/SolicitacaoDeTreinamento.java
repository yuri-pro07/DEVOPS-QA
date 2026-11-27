package br.edu.gamificacaocursos.treinamento.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@DynamicUpdate
@Table(name = "solicitacoes_de_treinamento",
        indexes = @Index(name = "idx_treinamento_pendente", columnList = "enviada_em, criada_em"))
public class SolicitacaoDeTreinamento {
    public static final int AMOSTRAS_PADRAO = 3000;
    static final String ENVIADA_PARA_A_FILA = "Publicada na fila de treinamentos do RabbitMQ";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String solicitante;

    @Column(length = 300)
    private String motivo;

    @Column(nullable = false)
    private int amostras;

    private Integer semente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusDoTreinamento status;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "enviada_em")
    private LocalDateTime enviadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;

    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;

    @Column(name = "versao_do_modelo", length = 40)
    private String versaoDoModelo;

    @Column(name = "semente_usada")
    private Integer sementeUsada;

    @Column(name = "amostras_de_treino")
    private Integer amostrasDeTreino;

    @Column(name = "amostras_de_teste")
    private Integer amostrasDeTeste;

    private Double acuracia;

    @Column(name = "f1_macro")
    private Double f1Macro;

    @ElementCollection
    @CollectionTable(name = "historico_dos_treinamentos", joinColumns = @JoinColumn(name = "solicitacao_id"))
    @OrderColumn(name = "posicao")
    private List<EventoDoTreinamento> historico = new ArrayList<>();

    protected SolicitacaoDeTreinamento() {
    }

    public static SolicitacaoDeTreinamento nova(String solicitante, String motivo, Integer amostras, Integer semente,
                                                LocalDateTime agora) {
        if (solicitante == null || solicitante.isBlank()) {
            throw new IllegalArgumentException("Informe quem esta solicitando o treinamento");
        }
        SolicitacaoDeTreinamento solicitacao = new SolicitacaoDeTreinamento();
        solicitacao.solicitante = solicitante.trim();
        solicitacao.motivo = motivo == null || motivo.isBlank() ? null : motivo.trim();
        solicitacao.amostras = amostras == null ? AMOSTRAS_PADRAO : amostras;
        solicitacao.semente = semente;
        solicitacao.status = StatusDoTreinamento.RECEBIDA;
        solicitacao.criadaEm = agora;
        solicitacao.atualizadaEm = agora;
        solicitacao.historico.add(new EventoDoTreinamento(StatusDoTreinamento.RECEBIDA,
                "Solicitacao registrada por " + solicitacao.solicitante, agora));
        return solicitacao;
    }

    public boolean registrar(AndamentoDoTreinamento andamento, LocalDateTime agora) {
        StatusDoTreinamento novo = andamento.status();
        if (novo == null || !novo.informadoPeloWorker()) {
            throw new IllegalArgumentException("Status de andamento invalido: " + novo);
        }
        if (status.finalizado()) {
            return false;
        }
        ResultadoDoTreinamento resultado = andamento.resultado();
        if (novo == StatusDoTreinamento.CONCLUIDO && resultado == null) {
            throw new IllegalArgumentException("Treinamento concluido sem o resultado do modelo");
        }
        status = novo;
        atualizadaEm = agora;
        if (andamento.tentativa() != null) {
            tentativas = Math.max(tentativas, andamento.tentativa());
        }
        if (novo.finalizado()) {
            concluidaEm = agora;
        }
        if (resultado != null) {
            versaoDoModelo = resultado.versao();
            sementeUsada = resultado.semente();
            amostrasDeTreino = resultado.amostrasDeTreino();
            amostrasDeTeste = resultado.amostrasDeTeste();
            acuracia = resultado.acuracia();
            f1Macro = resultado.f1Macro();
        }
        historico.add(new EventoDoTreinamento(novo, andamento.detalhe(), agora));
        return true;
    }

    public List<EventoDoTreinamento> linhaDoTempo() {
        List<EventoDoTreinamento> eventos = new ArrayList<>(historico);
        if (enviadaEm != null) {
            eventos.add(new EventoDoTreinamento(StatusDoTreinamento.NA_FILA, ENVIADA_PARA_A_FILA, enviadaEm));
        }
        eventos.sort(Comparator.comparing(EventoDoTreinamento::getRegistradoEm));
        return eventos;
    }

    public ResultadoDoTreinamento getResultado() {
        if (versaoDoModelo == null) {
            return null;
        }
        return new ResultadoDoTreinamento(versaoDoModelo, sementeUsada, amostrasDeTreino, amostrasDeTeste,
                acuracia, f1Macro);
    }

    public PedidoDeTreinamento pedido() {
        return new PedidoDeTreinamento(id, amostras, semente);
    }

    public Long getId() {
        return id;
    }

    public String getSolicitante() {
        return solicitante;
    }

    public String getMotivo() {
        return motivo;
    }

    public int getAmostras() {
        return amostras;
    }

    public Integer getSemente() {
        return semente;
    }

    public StatusDoTreinamento getStatus() {
        return status;
    }

    public int getTentativas() {
        return tentativas;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public LocalDateTime getEnviadaEm() {
        return enviadaEm;
    }

    public LocalDateTime getAtualizadaEm() {
        return atualizadaEm;
    }

    public LocalDateTime getConcluidaEm() {
        return concluidaEm;
    }
}
