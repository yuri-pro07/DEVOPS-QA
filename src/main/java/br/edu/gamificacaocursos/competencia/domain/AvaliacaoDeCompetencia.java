package br.edu.gamificacaocursos.competencia.domain;

import br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "avaliacoes_de_competencia", indexes = {
        @Index(name = "idx_avaliacao_colaborador", columnList = "colaborador_id, avaliado_em DESC")
})
public class AvaliacaoDeCompetencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "colaborador_id", nullable = false)
    private Long colaboradorId;

    @Column(name = "meses_experiencia", nullable = false)
    private int mesesExperiencia;

    @Column(name = "cursos_concluidos", nullable = false)
    private int cursosConcluidos;

    @Column(name = "media_avaliacoes", nullable = false)
    private double mediaAvaliacoes;

    @Column(name = "horas_semanais", nullable = false)
    private int horasSemanais;

    @Column(name = "conhece_rest", nullable = false)
    private boolean conheceRest;

    @Column(name = "conhece_banco_de_dados", nullable = false)
    private boolean conheceBancoDeDados;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelDeCompetencia nivel;

    @Column(name = "probabilidade_iniciante", nullable = false)
    private double probabilidadeIniciante;

    @Column(name = "probabilidade_intermediario", nullable = false)
    private double probabilidadeIntermediario;

    @Column(name = "probabilidade_avancado", nullable = false)
    private double probabilidadeAvancado;

    @Column(nullable = false)
    private String modelo;

    @Column(name = "avaliado_em", nullable = false)
    private LocalDateTime avaliadoEm;

    protected AvaliacaoDeCompetencia() {
    }

    public AvaliacaoDeCompetencia(Long colaboradorId, PerfilDeCompetencia perfil, Classificacao classificacao,
                                  LocalDateTime avaliadoEm) {
        this.colaboradorId = colaboradorId;
        this.mesesExperiencia = perfil.mesesExperiencia();
        this.cursosConcluidos = perfil.cursosConcluidos();
        this.mediaAvaliacoes = perfil.mediaAvaliacoes();
        this.horasSemanais = perfil.horasSemanais();
        this.conheceRest = perfil.conheceRest();
        this.conheceBancoDeDados = perfil.conheceBancoDeDados();
        this.nivel = classificacao.nivel();
        this.probabilidadeIniciante = classificacao.probabilidadeDe(NivelDeCompetencia.INICIANTE);
        this.probabilidadeIntermediario = classificacao.probabilidadeDe(NivelDeCompetencia.INTERMEDIARIO);
        this.probabilidadeAvancado = classificacao.probabilidadeDe(NivelDeCompetencia.AVANCADO);
        this.modelo = classificacao.modelo();
        this.avaliadoEm = avaliadoEm;
    }

    public Long getId() {
        return id;
    }

    public Long getColaboradorId() {
        return colaboradorId;
    }

    public PerfilDeCompetencia getPerfil() {
        return new PerfilDeCompetencia(mesesExperiencia, cursosConcluidos, mediaAvaliacoes, horasSemanais,
                conheceRest, conheceBancoDeDados);
    }

    public NivelDeCompetencia getNivel() {
        return nivel;
    }

    public Map<String, Double> getProbabilidades() {
        Map<String, Double> probabilidades = new LinkedHashMap<>();
        probabilidades.put(NivelDeCompetencia.INICIANTE.name(), probabilidadeIniciante);
        probabilidades.put(NivelDeCompetencia.INTERMEDIARIO.name(), probabilidadeIntermediario);
        probabilidades.put(NivelDeCompetencia.AVANCADO.name(), probabilidadeAvancado);
        return probabilidades;
    }

    public double getConfianca() {
        return getProbabilidades().get(nivel.name());
    }

    public String getModelo() {
        return modelo;
    }

    public LocalDateTime getAvaliadoEm() {
        return avaliadoEm;
    }
}
