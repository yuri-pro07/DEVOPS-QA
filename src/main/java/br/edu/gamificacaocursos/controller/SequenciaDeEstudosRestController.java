package br.edu.gamificacaocursos.controller;

import br.edu.gamificacaocursos.dto.RegistrarEstudoRequestDTO;
import br.edu.gamificacaocursos.dto.SequenciaDeEstudosRequestDTO;
import br.edu.gamificacaocursos.dto.SequenciaDeEstudosResponseDTO;
import br.edu.gamificacaocursos.service.SequenciaDeEstudosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Camada: CONTROLLER.
// Conversa com HTTP: recebe a requisicao, chama o service e devolve JSON.
@RestController
@RequestMapping("/api/sequencias")
@CrossOrigin(origins = "*")
@Tag(name = "Sequencias de Estudos", description = "Ofensiva de estudos do aluno (US1 do Case A)")
public class SequenciaDeEstudosRestController {

    private final SequenciaDeEstudosService service;

    public SequenciaDeEstudosRestController(SequenciaDeEstudosService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar todas as sequencias de estudos")
    public List<SequenciaDeEstudosResponseDTO> listar() {
        return service.listarTodas();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar uma sequencia pelo id")
    public SequenciaDeEstudosResponseDTO buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar a sequencia de estudos de um aluno")
    public SequenciaDeEstudosResponseDTO criar(@Valid @RequestBody SequenciaDeEstudosRequestDTO dto) {
        return service.criar(dto.getAluno());
    }

    @PostMapping("/{id}/estudos")
    @Operation(summary = "Registrar um dia de estudo e atualizar a ofensiva")
    public SequenciaDeEstudosResponseDTO registrarEstudo(@PathVariable Long id,
                                                         @Valid @RequestBody RegistrarEstudoRequestDTO dto) {
        return service.registrarEstudo(id, dto.getData());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover uma sequencia de estudos")
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }
}
