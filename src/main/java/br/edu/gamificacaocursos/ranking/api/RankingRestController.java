package br.edu.gamificacaocursos.ranking.api;

import br.edu.gamificacaocursos.ranking.application.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ranking")
@CrossOrigin(origins = "*")
@Tag(name = "Ranking", description = "Ranking de alunos por moedas e dias consecutivos (RF01)")
public class RankingRestController {

    private final RankingService service;

    public RankingRestController(RankingService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Consultar uma pagina do ranking")
    public RankingPaginaDTO consultar(@RequestParam(defaultValue = "0") int pagina,
                                      @RequestParam(defaultValue = "20") int tamanho) {
        return service.consultar(pagina, tamanho);
    }

    @GetMapping("/alunos/{aluno}")
    @Operation(summary = "Consultar a posicao de um aluno no ranking")
    public RankingItemDTO posicaoDe(@PathVariable String aluno) {
        return service.posicaoDe(aluno);
    }
}
