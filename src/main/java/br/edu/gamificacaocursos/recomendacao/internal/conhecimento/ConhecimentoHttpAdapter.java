package br.edu.gamificacaocursos.recomendacao.internal.conhecimento;

import br.edu.gamificacaocursos.recomendacao.application.BuscaDeDocumentos;
import br.edu.gamificacaocursos.recomendacao.domain.TrechoDeDocumento;
import br.edu.gamificacaocursos.shared.web.ServicoExternoIndisponivelException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class ConhecimentoHttpAdapter implements BuscaDeDocumentos {
    private final RestClient cliente;

    public ConhecimentoHttpAdapter(@Qualifier("clienteDoConhecimento") RestClient cliente) {
        this.cliente = cliente;
    }

    @Override
    public List<TrechoDeDocumento> buscar(String consulta, int quantidade) {
        try {
            RespostaDaBusca resposta = cliente.get()
                    .uri(uri -> uri.path("/buscar").queryParam("q", consulta).queryParam("k", quantidade).build())
                    .retrieve()
                    .body(RespostaDaBusca.class);
            return resposta == null || resposta.trechos() == null ? List.of() : resposta.trechos();
        } catch (RestClientException e) {
            throw new ServicoExternoIndisponivelException("Servico de documentos indisponivel no momento", e);
        }
    }

    record RespostaDaBusca(List<TrechoDeDocumento> trechos) {
    }
}
