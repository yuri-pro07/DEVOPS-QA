package br.edu.exemplo.gamificacaocursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SequenciaDeEstudosTest {

    @Test
    @DisplayName("Cenario 1 (Yuri Peruzzo): dado que o aluno estudou ontem, quando ele registra estudo hoje, entao sua sequencia de dias consecutivos deve aumentar")
    void aumentaSequenciaAoEstudarEmDiasConsecutivos() {
        var sequencia = new SequenciaDeEstudos("Rafael");
        var ontem = LocalDate.of(2026, 9, 1);
        var hoje = LocalDate.of(2026, 9, 2);

        sequencia.registrarEstudo(ontem);
        sequencia.registrarEstudo(hoje);

        assertEquals(2, sequencia.getDiasConsecutivos());
    }

    @Test
    @DisplayName("Cenario 2 (Enzo Zorzetto): dado que o aluno tinha uma sequencia de 2 dias seguidos, quando ele deixa passar um dia sem estudar, entao sua sequencia deve reiniciar para 1")
    void reiniciaSequenciaQuandoPulaUmDiaSemEstudar() {
        var sequencia = new SequenciaDeEstudos("Aline");
        var dia1 = LocalDate.of(2026, 9, 1);
        var dia2 = LocalDate.of(2026, 9, 2);
        var dia4 = LocalDate.of(2026, 9, 4);

        sequencia.registrarEstudo(dia1);
        sequencia.registrarEstudo(dia2);
        sequencia.registrarEstudo(dia4);

        assertEquals(1, sequencia.getDiasConsecutivos());
    }

    @Test
    @DisplayName("Cenario 3 (Pedro Ricci Gomes Nascimento): dado que o aluno estudou 6 dias seguidos, quando ele estuda no 7o dia consecutivo, entao ele deve receber 1 moeda de recompensa")
    void recebeMoedaAoCompletarSeteDiasConsecutivos() {
        var sequencia = new SequenciaDeEstudos("Pedro");
        var inicio = LocalDate.of(2026, 9, 1);

        for (int i = 0; i < 7; i++) {
            sequencia.registrarEstudo(inicio.plusDays(i));
        }

        assertEquals(7, sequencia.getDiasConsecutivos());
        assertEquals(1, sequencia.getMoedasConquistadas());
    }
}
