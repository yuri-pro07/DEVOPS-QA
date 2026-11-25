package br.edu.gamificacaocursos.orientacao.domain;

import br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "planos_de_estudo",
        indexes = @Index(name = "idx_plano_colaborador", columnList = "colaborador_id, criado_em DESC"))
public class PlanoDeEstudos {
    static final int TAMANHO_DA_ORIENTACAO = 4000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "colaborador_id", nullable = false)
    private Long colaboradorId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NivelDeCompetencia nivel;

    @Column(length = 300)
    private String objetivo;

    @Column(nullable = false, length = TAMANHO_DA_ORIENTACAO)
    private String orientacao;

    @Column(nullable = false, length = 80)
    private String modo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusDoPlano status;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "decidido_em")
    private LocalDateTime decididoEm;

    @ElementCollection
    @CollectionTable(name = "plano_cursos_sugeridos", joinColumns = @JoinColumn(name = "plano_id"))
    @OrderColumn(name = "posicao")
    @Column(name = "curso_id", nullable = false)
    private List<Long> cursosSugeridos = new ArrayList<>();

    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<EtapaDoPlano> etapas = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "plano_registros_dos_agentes", joinColumns = @JoinColumn(name = "plano_id"))
    @OrderColumn(name = "posicao")
    private List<RegistroDeAgente> registros = new ArrayList<>();

    protected PlanoDeEstudos() {
    }

    public PlanoDeEstudos(Long colaboradorId, NivelDeCompetencia nivel, String objetivo, String orientacao,
                          String modo, List<Long> cursosSugeridos, LocalDateTime criadoEm) {
        this.colaboradorId = colaboradorId;
        this.nivel = nivel;
        this.objetivo = objetivo;
        this.orientacao = orientacao.length() > TAMANHO_DA_ORIENTACAO
                ? orientacao.substring(0, TAMANHO_DA_ORIENTACAO)
                : orientacao;
        this.modo = modo;
        this.cursosSugeridos.addAll(cursosSugeridos);
        this.criadoEm = criadoEm;
        this.status = StatusDoPlano.AGUARDANDO_AUTORIZACAO;
    }

    public void exigirAguardandoAutorizacao() {
        if (status != StatusDoPlano.AGUARDANDO_AUTORIZACAO) {
            throw new IllegalStateException("O plano " + id + " ja foi decidido e esta " + status);
        }
    }

    public void autorizar(List<EtapaDoPlano> novasEtapas, LocalDateTime em) {
        exigirAguardandoAutorizacao();
        novasEtapas.forEach(etapa -> {
            etapa.vincular(this);
            etapas.add(etapa);
        });
        status = StatusDoPlano.AUTORIZADO;
        decididoEm = em;
    }

    public void recusar(LocalDateTime em) {
        exigirAguardandoAutorizacao();
        status = StatusDoPlano.RECUSADO;
        decididoEm = em;
    }

    public void concluirEtapa(Long etapaId, LocalDate em) {
        if (status != StatusDoPlano.AUTORIZADO) {
            throw new IllegalStateException("Somente planos autorizados e em andamento podem ter etapas concluidas");
        }
        etapas.stream()
                .filter(etapa -> Objects.equals(etapa.getId(), etapaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Etapa nao encontrada no plano: " + etapaId))
                .concluir(em);
        if (etapas.stream().allMatch(EtapaDoPlano::concluida)) {
            status = StatusDoPlano.CONCLUIDO;
        }
    }

    public void registrar(RegistroDeAgente registro) {
        registros.add(registro);
    }

    public Long getId() {
        return id;
    }

    public Long getColaboradorId() {
        return colaboradorId;
    }

    public NivelDeCompetencia getNivel() {
        return nivel;
    }

    public String getObjetivo() {
        return objetivo;
    }

    public String getOrientacao() {
        return orientacao;
    }

    public String getModo() {
        return modo;
    }

    public StatusDoPlano getStatus() {
        return status;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getDecididoEm() {
        return decididoEm;
    }

    public List<Long> getCursosSugeridos() {
        return List.copyOf(cursosSugeridos);
    }

    public List<EtapaDoPlano> getEtapas() {
        return List.copyOf(etapas);
    }

    public List<RegistroDeAgente> getRegistros() {
        return List.copyOf(registros);
    }
}
