package br.edu.gamificacaocursos.competencia.api;

import br.edu.gamificacaocursos.competencia.application.CompetenciaService;
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
@RequestMapping("/api/colaboradores/{colaboradorId}/competencias")
@CrossOrigin(origins = "*")
@Tag(name = "Competencias", description = "Classificacao do nivel de competencia por ML (RF02 da AF)")
public class CompetenciaRestController {
    private final CompetenciaService service;

    public CompetenciaRestController(CompetenciaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Avaliar o colaborador e classificar o nivel de competencia com ML")
    public AvaliacaoResponseDTO avaliar(@PathVariable Long colaboradorId,
                                        @Valid @RequestBody AvaliacaoRequestDTO dto) {
        return service.avaliar(colaboradorId, dto.paraPerfil());
    }

    @GetMapping
    @Operation(summary = "Historico de avaliacoes do colaborador, da mais recente para a mais antiga")
    public List<AvaliacaoResponseDTO> historico(@PathVariable Long colaboradorId) {
        return service.historico(colaboradorId);
    }
}
