package br.edu.gamificacaocursos.orientacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "etapas_do_plano")
public class EtapaDoPlano {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plano_id", nullable = false)
    private PlanoDeEstudos plano;

    @Column(nullable = false)
    private int ordem;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    @Column(name = "titulo_do_curso", nullable = false)
    private String tituloDoCurso;

    @Column(name = "carga_horaria", nullable = false)
    private int cargaHoraria;

    @Column(nullable = false)
    private LocalDate prazo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusDaEtapa status;

    @Column(name = "concluida_em")
    private LocalDate concluidaEm;

    protected EtapaDoPlano() {
    }

    public EtapaDoPlano(int ordem, Long cursoId, String tituloDoCurso, int cargaHoraria, LocalDate prazo) {
        this.ordem = ordem;
        this.cursoId = cursoId;
        this.tituloDoCurso = tituloDoCurso;
        this.cargaHoraria = cargaHoraria;
        this.prazo = prazo;
        this.status = StatusDaEtapa.PENDENTE;
    }

    void vincular(PlanoDeEstudos plano) {
        this.plano = plano;
    }

    void concluir(LocalDate em) {
        if (concluida()) {
            throw new IllegalStateException("A etapa " + ordem + " ja foi concluida");
        }
        this.status = StatusDaEtapa.CONCLUIDA;
        this.concluidaEm = em;
    }

    public boolean concluida() {
        return status == StatusDaEtapa.CONCLUIDA;
    }

    public boolean atrasadaEm(LocalDate hoje) {
        return !concluida() && prazo.isBefore(hoje);
    }

    public Long getId() {
        return id;
    }

    public int getOrdem() {
        return ordem;
    }

    public Long getCursoId() {
        return cursoId;
    }

    public String getTituloDoCurso() {
        return tituloDoCurso;
    }

    public int getCargaHoraria() {
        return cargaHoraria;
    }

    public LocalDate getPrazo() {
        return prazo;
    }

    public StatusDaEtapa getStatus() {
        return status;
    }

    public LocalDate getConcluidaEm() {
        return concluidaEm;
    }
}
