package br.edu.gamificacaocursos.orientacao.api;

import br.edu.gamificacaocursos.orientacao.application.OrientacaoService;
import br.edu.gamificacaocursos.orientacao.application.PoliticaDeAcessoDosAgentes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Orientacao (agentes de IA)", description = "Planos de estudo propostos por agentes e autorizados pelo colaborador")
public class PlanoRestController {
    private final OrientacaoService service;
    private final PoliticaDeAcessoDosAgentes politica;

    public PlanoRestController(OrientacaoService service, PoliticaDeAcessoDosAgentes politica) {
        this.service = service;
        this.politica = politica;
    }

    @PostMapping("/colaboradores/{colaboradorId}/planos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "O agente orientador propoe um plano de estudos que fica aguardando autorizacao")
    public PlanoResponseDTO propor(@PathVariable Long colaboradorId,
                                   @Valid @RequestBody(required = false) PlanoRequestDTO pedido) {
        return service.propor(colaboradorId, pedido == null ? null : pedido.objetivo());
    }

    @GetMapping("/colaboradores/{colaboradorId}/planos")
    @Operation(summary = "Lista os planos de estudo do colaborador, do mais recente para o mais antigo")
    public List<PlanoResponseDTO> listar(@PathVariable Long colaboradorId) {
        return service.listar(colaboradorId);
    }

    @GetMapping("/planos/{planoId}")
    @Operation(summary = "Consulta o plano com as etapas e o acompanhamento feito pelo agente")
    public PlanoResponseDTO consultar(@PathVariable Long planoId) {
        return service.consultar(planoId);
    }

    @PostMapping("/planos/{planoId}/decisao")
    @Operation(summary = "O colaborador autoriza ou recusa o plano proposto")
    public PlanoResponseDTO decidir(@PathVariable Long planoId, @Valid @RequestBody DecisaoRequestDTO decisao) {
        return service.decidir(planoId, decisao.autorizado());
    }

    @PostMapping("/planos/{planoId}/etapas/{etapaId}/conclusao")
    @Operation(summary = "Marca uma etapa do plano autorizado como concluida")
    public PlanoResponseDTO concluirEtapa(@PathVariable Long planoId, @PathVariable Long etapaId) {
        return service.concluirEtapa(planoId, etapaId);
    }

    @GetMapping("/agentes/permissoes")
    @Operation(summary = "Mostra quais ferramentas cada agente pode usar")
    public Map<String, List<String>> permissoes() {
        return politica.permissoes();
    }
}
