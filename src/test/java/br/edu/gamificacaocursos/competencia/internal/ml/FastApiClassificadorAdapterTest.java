package br.edu.gamificacaocursos.competencia.internal.ml;

import br.edu.gamificacaocursos.competencia.domain.Classificacao;
import br.edu.gamificacaocursos.competencia.domain.PerfilDeCompetencia;
import br.edu.gamificacaocursos.shared.web.ServicoExternoIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia.INICIANTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FastApiClassificadorAdapterTest {

    private static final PerfilDeCompetencia PERFIL = new PerfilDeCompetencia(6, 1, 6.0, 5, false, false);

    private MockRestServiceServer servidor;
    private FastApiClassificadorAdapter adapter;

    @BeforeEach
    void prepara() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ml:8000");
        servidor = MockRestServiceServer.bindTo(builder).build();
        adapter = new FastApiClassificadorAdapter(builder.build());
    }

    @Test
    @DisplayName("Envia o perfil em JSON para /predict e converte a resposta do FastAPI")
    void classifica() {
        servidor.expect(requestTo("http://ml:8000/predict"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.mesesExperiencia").value(6))
                .andExpect(jsonPath("$.conheceRest").value(false))
                .andRespond(withSuccess("{\"nivel\":\"INICIANTE\",\"probabilidades\":{\"INICIANTE\":0.93,"
                        + "\"INTERMEDIARIO\":0.07,\"AVANCADO\":0.0},\"modelo\":\"regressao-logistica v1\"}",
                        MediaType.APPLICATION_JSON));

        Classificacao classificacao = adapter.classificar(PERFIL);

        assertEquals(INICIANTE, classificacao.nivel());
        assertEquals(0.93, classificacao.confianca());
        servidor.verify();
    }

    @Test
    @DisplayName("Erro 500 do ML vira servico indisponivel")
    void erroDoServidor() {
        servidor.expect(requestTo("http://ml:8000/predict")).andRespond(withServerError());

        assertThrows(ServicoExternoIndisponivelException.class, () -> adapter.classificar(PERFIL));
    }

    @Test
    @DisplayName("Tempo esgotado ou conexao recusada vira servico indisponivel")
    void tempoEsgotado() {
        servidor.expect(requestTo("http://ml:8000/predict"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        ServicoExternoIndisponivelException erro =
                assertThrows(ServicoExternoIndisponivelException.class, () -> adapter.classificar(PERFIL));

        assertInstanceOf(ResourceAccessException.class, erro.getCause());
    }

    @Test
    @DisplayName("Resposta sem corpo ou sem nivel tambem e tratada como indisponibilidade")
    void respostaVazia() {
        servidor.expect(requestTo("http://ml:8000/predict")).andRespond(withSuccess());
        assertThrows(ServicoExternoIndisponivelException.class, () -> adapter.classificar(PERFIL));

        servidor.reset();
        servidor.expect(requestTo("http://ml:8000/predict"))
                .andRespond(withSuccess("{\"probabilidades\":{}}", MediaType.APPLICATION_JSON));
        assertThrows(ServicoExternoIndisponivelException.class, () -> adapter.classificar(PERFIL));
    }
}
