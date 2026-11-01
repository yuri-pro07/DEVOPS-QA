package br.edu.gamificacaocursos.ofensiva.application;

import br.edu.gamificacaocursos.ofensiva.domain.SequenciaDeEstudos;
import br.edu.gamificacaocursos.ofensiva.internal.persistence.SequenciaDeEstudosRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SequenciaDeEstudosServiceMockitoTest {

    @Mock
    private SequenciaDeEstudosRepository repository;

    private SimpleMeterRegistry metricas;
    private SequenciaDeEstudosService service;

    @BeforeEach
    void prepara() {
        metricas = new SimpleMeterRegistry();
        service = new SequenciaDeEstudosService(repository, metricas);
    }

    @Test
    @DisplayName("Registrar estudo busca a sequencia e so depois a salva")
    void buscaAntesDeSalvar() {
        SequenciaDeEstudos sequencia = new SequenciaDeEstudos("Maria");
        when(repository.findById(1L)).thenReturn(Optional.of(sequencia));
        when(repository.save(sequencia)).thenReturn(sequencia);

        service.registrarEstudo(1L, LocalDate.of(2026, 9, 1));

        InOrder ordem = inOrder(repository);
        ordem.verify(repository).findById(1L);
        ordem.verify(repository).save(sequencia);
        assertEquals(1, sequencia.getDiasConsecutivos());
    }

    @Test
    @DisplayName("Sequencia inexistente nunca chega a ser salva")
    void naoSalvaSequenciaInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.registrarEstudo(99L, LocalDate.now()));

        verify(repository, never()).save(any());
        assertEquals(0, metricas.get("gamificacao.ofensiva.estudos").counter().count());
    }

    @Test
    @DisplayName("Aluno duplicado e barrado antes de qualquer gravacao")
    void naoCriaAlunoDuplicado() {
        when(repository.existsByAluno(anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.criar("Maria"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Sete dias seguidos incrementam as metricas de estudos e de moedas")
    void alimentaAsMetricasDeNegocio() {
        SequenciaDeEstudos sequencia = new SequenciaDeEstudos("Pedro");
        when(repository.findById(1L)).thenReturn(Optional.of(sequencia));
        when(repository.save(sequencia)).thenReturn(sequencia);

        LocalDate inicio = LocalDate.of(2026, 9, 1);
        for (int i = 0; i < 7; i++) {
            service.registrarEstudo(1L, inicio.plusDays(i));
        }

        assertEquals(7, metricas.get("gamificacao.ofensiva.estudos").counter().count());
        assertEquals(1, metricas.get("gamificacao.ofensiva.moedas").counter().count());
    }
}
