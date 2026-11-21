package br.edu.gamificacaocursos.curso.domain;

import br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cursos")
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(nullable = false)
    private String area;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelDeCompetencia nivel;

    @Column(name = "carga_horaria", nullable = false)
    private int cargaHoraria;

    protected Curso() {
    }

    public Curso(String titulo, String descricao, String area, NivelDeCompetencia nivel, int cargaHoraria) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.area = area;
        this.nivel = nivel;
        this.cargaHoraria = cargaHoraria;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getArea() {
        return area;
    }

    public NivelDeCompetencia getNivel() {
        return nivel;
    }

    public int getCargaHoraria() {
        return cargaHoraria;
    }
}
