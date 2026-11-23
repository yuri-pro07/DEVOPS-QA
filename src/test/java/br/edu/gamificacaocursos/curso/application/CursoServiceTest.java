package br.edu.gamificacaocursos.curso.application;

import br.edu.gamificacaocursos.curso.api.CursoRequestDTO;
import br.edu.gamificacaocursos.curso.api.CursoResponseDTO;
import br.edu.gamificacaocursos.curso.domain.Curso;
import br.edu.gamificacaocursos.curso.internal.persistence.CursoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia.AVANCADO;
import static br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia.INICIANTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

    @Mock
    private CursoRepository repository;

    private CursoService service;

    @BeforeEach
    void prepara() {
        service = new CursoService(repository);
    }

    @Test
    @DisplayName("Cadastra o curso com os campos sem espacos")
    void cadastra() {
        when(repository.existsByTituloIgnoreCase("Kotlin")).thenReturn(false);
        when(repository.save(any(Curso.class))).thenAnswer(chamada -> chamada.getArgument(0));

        CursoResponseDTO curso = service.cadastrar(new CursoRequestDTO(" Kotlin ", " Linguagem ", " Dev ", INICIANTE, 10));

        assertEquals("Kotlin", curso.titulo());
        assertEquals("Linguagem", curso.descricao());
        assertEquals("Dev", curso.area());
        assertEquals(INICIANTE, curso.nivel());
        assertEquals(10, curso.cargaHoraria());
    }

    @Test
    @DisplayName("Titulo repetido e recusado sem gravar")
    void recusaTituloRepetido() {
        when(repository.existsByTituloIgnoreCase("Kotlin")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.cadastrar(new CursoRequestDTO("Kotlin", "x", "Dev", INICIANTE, 10)));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Area em branco nao filtra e area preenchida filtra sem espacos")
    void filtraPorNivelEArea() {
        Curso curso = new Curso("ML", "Modelos", "Dados", AVANCADO, 30);
        when(repository.filtrar(AVANCADO, null)).thenReturn(List.of(curso));
        when(repository.filtrar(null, "Dados")).thenReturn(List.of(curso));

        assertEquals(1, service.listar(AVANCADO, null).size());
        assertEquals(1, service.listar(AVANCADO, "  ").size());
        assertEquals("ML", service.listar(null, " Dados ").get(0).titulo());
    }

    @Test
    @DisplayName("Consulta pelo id e erro para id inexistente")
    void consultaPorId() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Curso("ML", "Modelos", "Dados", AVANCADO, 30)));
        when(repository.findById(2L)).thenReturn(Optional.empty());

        assertEquals("ML", service.buscarPorId(1L).titulo());
        assertThrows(IllegalArgumentException.class, () -> service.buscarPorId(2L));
    }
}
