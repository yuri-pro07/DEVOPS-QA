package br.edu.gamificacaocursos.colaborador.api;

import br.edu.gamificacaocursos.colaborador.application.ColaboradorService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/colaboradores")
@CrossOrigin(origins = "*")
@Tag(name = "Colaboradores", description = "Cadastro e consulta de colaboradores (RF01 da AF)")
public class ColaboradorRestController {
    private final ColaboradorService service;

    public ColaboradorRestController(ColaboradorService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar um colaborador")
    public ColaboradorResponseDTO cadastrar(@Valid @RequestBody ColaboradorRequestDTO dto) {
        return service.cadastrar(dto);
    }

    @GetMapping
    @Operation(summary = "Listar os colaboradores")
    public List<ColaboradorResponseDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar um colaborador pelo id")
    public ColaboradorResponseDTO buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }
}
