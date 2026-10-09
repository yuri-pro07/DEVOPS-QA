package br.edu.gamificacaocursos.ofensiva.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "sequencias_de_estudos")
public class SequenciaDeEstudos {
    static final int DIAS_PARA_RECOMPENSA = 7;
    static final int MOEDAS_POR_MARCO = 1;
    static final int PRIMEIRO_DIA_DA_SEQUENCIA = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aluno;

    @Column(name = "dias_consecutivos", nullable = false)
    private int diasConsecutivos;

    @Column(name = "ultimo_dia_estudado")
    private LocalDate ultimoDiaEstudado;

    @Column(name = "moedas_conquistadas", nullable = false)
    private int moedasConquistadas;

        protected SequenciaDeEstudos() {
    }

    public SequenciaDeEstudos(String aluno) {
        this.aluno = aluno;
        this.diasConsecutivos = 0;
        this.ultimoDiaEstudado = null;
        this.moedasConquistadas = 0;
    }

        public void registrarEstudo(LocalDate data) {
        this.diasConsecutivos = ehDiaSeguinteAoUltimoEstudo(data)
                ? diasConsecutivos + 1
                : PRIMEIRO_DIA_DA_SEQUENCIA;
        this.ultimoDiaEstudado = data;

        premiarSeAtingiuMarco();
    }

    private boolean ehDiaSeguinteAoUltimoEstudo(LocalDate data) {
        return ultimoDiaEstudado != null && data.equals(ultimoDiaEstudado.plusDays(1));
    }

    private void premiarSeAtingiuMarco() {
        if (diasConsecutivos == DIAS_PARA_RECOMPENSA) {
            this.moedasConquistadas += MOEDAS_POR_MARCO;
        }
    }

    public Long getId() {
        return id;
    }

    public String getAluno() {
        return aluno;
    }

    public int getDiasConsecutivos() {
        return diasConsecutivos;
    }

    public LocalDate getUltimoDiaEstudado() {
        return ultimoDiaEstudado;
    }

    public int getMoedasConquistadas() {
        return moedasConquistadas;
    }
}
