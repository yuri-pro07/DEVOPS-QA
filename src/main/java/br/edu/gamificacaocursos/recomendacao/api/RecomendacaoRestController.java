package br.edu.gamificacaocursos.recomendacao.api;

import br.edu.gamificacaocursos.recomendacao.application.RecomendacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/colaboradores/{colaboradorId}/recomendacoes")
@Tag(name = "Recomendacoes (RAG)", description = "Recomendacao de cursos com base nos documentos institucionais")
public class RecomendacaoRestController {
    private final RecomendacaoService service;

    public RecomendacaoRestController(RecomendacaoService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Recomenda cursos para o colaborador citando os documentos institucionais usados")
    public RecomendacaoResponseDTO recomendar(@PathVariable Long colaboradorId,
                                              @Valid @RequestBody(required = false) RecomendacaoRequestDTO pedido) {
        return service.recomendar(colaboradorId, pedido == null ? null : pedido.objetivo());
    }
}
