package br.edu.gamificacaocursos.competencia.internal.ml;

import br.edu.gamificacaocursos.competencia.application.ClassificadorDeCompetencia;
import br.edu.gamificacaocursos.competencia.domain.Classificacao;
import br.edu.gamificacaocursos.competencia.domain.PerfilDeCompetencia;
import br.edu.gamificacaocursos.shared.web.ServicoExternoIndisponivelException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class FastApiClassificadorAdapter implements ClassificadorDeCompetencia {
    private final RestClient cliente;

    public FastApiClassificadorAdapter(@Qualifier("clienteDoMl") RestClient cliente) {
        this.cliente = cliente;
    }

    @Override
    public Classificacao classificar(PerfilDeCompetencia perfil) {
        Classificacao classificacao;
        try {
            classificacao = cliente.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(perfil)
                    .retrieve()
                    .body(Classificacao.class);
        } catch (RestClientException e) {
            throw new ServicoExternoIndisponivelException("Servico de ML indisponivel no momento", e);
        }
        if (classificacao == null || classificacao.nivel() == null) {
            throw new ServicoExternoIndisponivelException("Servico de ML devolveu uma resposta vazia");
        }
        return classificacao;
    }
}
