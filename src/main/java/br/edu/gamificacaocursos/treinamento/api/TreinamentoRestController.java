package br.edu.gamificacaocursos.treinamento.api;

import br.edu.gamificacaocursos.treinamento.application.TreinamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/treinamentos")
@Tag(name = "Treinamentos (RabbitMQ)", description = "Solicitacoes de treinamento do modelo de ML processadas por fila")
public class TreinamentoRestController {
    private final TreinamentoService service;

    public TreinamentoRestController(TreinamentoService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Registra a solicitacao e a coloca na fila. Responde 202 mesmo com o servico de ML fora do ar")
    public ResponseEntity<TreinamentoResponseDTO> solicitar(@Valid @RequestBody TreinamentoRequestDTO pedido) {
        TreinamentoResponseDTO criada = service.solicitar(pedido);
        return ResponseEntity.accepted().location(URI.create("/api/treinamentos/" + criada.id())).body(criada);
    }

    @GetMapping
    @Operation(summary = "Lista as 20 solicitacoes de treinamento mais recentes com o andamento de cada uma")
    public List<TreinamentoResponseDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta o andamento de uma solicitacao, com o historico de eventos e o modelo gerado")
    public TreinamentoResponseDTO consultar(@PathVariable Long id) {
        return service.consultar(id);
    }
}
