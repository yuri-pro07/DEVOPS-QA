package br.edu.gamificacaocursos.curso.internal.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoInicialTest {

    @Mock
    private CursoRepository repository;

    @Test
    @DisplayName("Banco vazio recebe os nove cursos do catalogo inicial")
    void populaBancoVazio() {
        when(repository.count()).thenReturn(0L);

        new CatalogoInicial(repository).run(null);

        verify(repository).saveAll(argThat(cursos -> cursos instanceof java.util.List<?> lista && lista.size() == 9));
    }

    @Test
    @DisplayName("Banco com cursos nao e alterado")
    void naoDuplicaCatalogo() {
        when(repository.count()).thenReturn(3L);

        new CatalogoInicial(repository).run(null);

        verify(repository, never()).saveAll(anyList());
    }
}
