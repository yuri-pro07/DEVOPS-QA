package br.edu.gamificacaocursos.colaborador.application;

import br.edu.gamificacaocursos.colaborador.api.ColaboradorRequestDTO;
import br.edu.gamificacaocursos.colaborador.api.ColaboradorResponseDTO;
import br.edu.gamificacaocursos.colaborador.domain.Colaborador;
import br.edu.gamificacaocursos.colaborador.internal.persistence.ColaboradorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColaboradorServiceTest {

    @Mock
    private ColaboradorRepository repository;

    private ColaboradorService service;

    @BeforeEach
    void prepara() {
        service = new ColaboradorService(repository);
    }

    @Test
    @DisplayName("Cadastra o colaborador com email normalizado e campos sem espacos")
    void cadastraNormalizando() {
        when(repository.existsByEmail("ana@empresa.com")).thenReturn(false);
        when(repository.save(any(Colaborador.class))).thenAnswer(chamada -> chamada.getArgument(0));

        ColaboradorResponseDTO criado = service.cadastrar(
                new ColaboradorRequestDTO(" Ana Souza ", " ANA@Empresa.com ", " Analista ", " TI "));

        ArgumentCaptor<Colaborador> salvo = ArgumentCaptor.forClass(Colaborador.class);
        verify(repository).save(salvo.capture());
        assertEquals("ana@empresa.com", salvo.getValue().getEmail());
        assertEquals("Ana Souza", criado.nome());
        assertEquals("Analista", criado.cargo());
        assertEquals("TI", criado.area());
    }

    @Test
    @DisplayName("Email ja cadastrado e recusado sem gravar")
    void recusaEmailDuplicado() {
        when(repository.existsByEmail("ana@empresa.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.cadastrar(
                new ColaboradorRequestDTO("Ana", "ana@empresa.com", "Analista", "TI")));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Lista e consulta colaboradores pelo id")
    void listaEConsulta() {
        Colaborador ana = new Colaborador("Ana", "ana@empresa.com", "Analista", "TI");
        when(repository.findAll()).thenReturn(List.of(ana));
        when(repository.findById(1L)).thenReturn(Optional.of(ana));

        assertEquals(1, service.listar().size());
        assertEquals("ana@empresa.com", service.buscarPorId(1L).email());
    }

    @Test
    @DisplayName("Id inexistente gera erro de regra de negocio")
    void idInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.buscarPorId(99L));

        assertEquals("Colaborador nao encontrado: 99", erro.getMessage());
    }
}
