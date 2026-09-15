package br.edu.gamificacaocursos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Camada: DOMINIO (e tambem a ENTIDADE persistida).
 *
 * Ofensiva de estudos do aluno: conta os dias consecutivos de estudo e premia a
 * regularidade quando um marco e atingido. Toda a regra de negocio da US1 mora
 * aqui e foi construida por TDD (RED -> GREEN -> BLUE).
 */
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

    /**
     * Exigido pelo JPA para reconstruir a entidade vinda do banco.
     * Protegido de proposito: fora do dominio, use o construtor com o aluno.
     */
    protected SequenciaDeEstudos() {
    }

    public SequenciaDeEstudos(String aluno) {
        this.aluno = aluno;
        this.diasConsecutivos = 0;
        this.ultimoDiaEstudado = null;
        this.moedasConquistadas = 0;
    }

    /**
     * Registra que o aluno estudou na data informada: a sequencia avanca quando o
     * estudo ocorre no dia seguinte ao ultimo, e recomeca do zero caso contrario.
     */
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
