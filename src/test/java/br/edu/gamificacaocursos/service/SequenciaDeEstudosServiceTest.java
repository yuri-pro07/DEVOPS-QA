package br.edu.gamificacaocursos.service;

import br.edu.gamificacaocursos.domain.SequenciaDeEstudos;
import br.edu.gamificacaocursos.repository.SequenciaDeEstudosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Testes da camada SERVICE rodando contra o H2 em memoria.
@SpringBootTest
@ActiveProfiles("h2")
class SequenciaDeEstudosServiceTest {

    @Autowired
    private SequenciaDeEstudosService service;

    @Autowired
    private SequenciaDeEstudosRepository repository;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("cria a sequência de um aluno e a devolve zerada")
    void criaSequencia() {
        var dto = service.criar("Rafael");

        assertNotNull(dto.getId());
        assertEquals("Rafael", dto.getAluno());
        assertEquals(0, dto.getDiasConsecutivos());
        assertEquals(0, dto.getMoedasConquistadas());
    }

    @Test
    @DisplayName("não aceita duas sequências para o mesmo aluno")
    void naoDuplicaAluno() {
        service.criar("Rafael");

        var erro = assertThrows(IllegalArgumentException.class, () -> service.criar("Rafael"));
        assertTrue(erro.getMessage().contains("Rafael"));
    }

    @Test
    @DisplayName("registrar estudo aplica a regra do domínio e persiste o resultado")
    void registraEstudoEPersiste() {
        var criada = service.criar("Aline");

        service.registrarEstudo(criada.getId(), LocalDate.of(2026, 9, 1));
        var apos = service.registrarEstudo(criada.getId(), LocalDate.of(2026, 9, 2));

        assertEquals(2, apos.getDiasConsecutivos());
        assertEquals(LocalDate.of(2026, 9, 2), apos.getUltimoDiaEstudado());
        assertEquals(2, service.buscarPorId(criada.getId()).getDiasConsecutivos());
    }

    @Test
    @DisplayName("sete dias seguidos rendem a moeda, mesmo passando pelo banco")
    void seteDiasRendemMoeda() {
        var criada = service.criar("Pedro");
        var inicio = LocalDate.of(2026, 9, 1);

        for (int i = 0; i < 7; i++) {
            service.registrarEstudo(criada.getId(), inicio.plusDays(i));
        }

        var dto = service.buscarPorId(criada.getId());
        assertEquals(7, dto.getDiasConsecutivos());
        assertEquals(1, dto.getMoedasConquistadas());
    }

    @Test
    @DisplayName("listar devolve as sequências gravadas")
    void listaSequencias() {
        service.criar("Rafael");
        service.criar("Aline");

        assertEquals(2, service.listarTodas().size());
    }

    @Test
    @DisplayName("remover apaga a sequência")
    void removeSequencia() {
        var criada = service.criar("Rafael");

        service.remover(criada.getId());

        assertTrue(service.listarTodas().isEmpty());
    }

    @Test
    @DisplayName("id inexistente vira erro de regra de negócio")
    void idInexistenteFalha() {
        assertThrows(IllegalArgumentException.class, () -> service.buscarPorId(9999L));
        assertThrows(IllegalArgumentException.class,
                () -> service.registrarEstudo(9999L, LocalDate.of(2026, 9, 1)));
        assertThrows(IllegalArgumentException.class, () -> service.remover(9999L));
    }

    @Test
    @DisplayName("a entidade recuperada do banco mantém o estado da ofensiva")
    void entidadeSobreviveAoBanco() {
        var criada = service.criar("Rafael");
        service.registrarEstudo(criada.getId(), LocalDate.of(2026, 9, 1));

        SequenciaDeEstudos doBanco = repository.findByAluno("Rafael").orElseThrow();

        assertEquals(1, doBanco.getDiasConsecutivos());
        assertNotNull(doBanco.getId());
        assertNotNull(criada.getId());
    }
}
