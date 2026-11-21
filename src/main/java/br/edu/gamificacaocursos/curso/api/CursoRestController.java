package br.edu.gamificacaocursos.curso.api;

import br.edu.gamificacaocursos.curso.application.CursoService;
import br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cursos")
@CrossOrigin(origins = "*")
@Tag(name = "Cursos", description = "Cadastro e consulta de cursos (RF01 da AF)")
public class CursoRestController {
    private final CursoService service;

    public CursoRestController(CursoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar um curso")
    public CursoResponseDTO cadastrar(@Valid @RequestBody CursoRequestDTO dto) {
        return service.cadastrar(dto);
    }

    @GetMapping
    @Operation(summary = "Listar cursos, com filtro opcional por nivel e area")
    public List<CursoResponseDTO> listar(@RequestParam(required = false) NivelDeCompetencia nivel,
                                         @RequestParam(required = false) String area) {
        return service.listar(nivel, area);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar um curso pelo id")
    public CursoResponseDTO buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }
}
