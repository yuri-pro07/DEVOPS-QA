package br.edu.gamificacaocursos.recomendacao.internal.conhecimento;

import br.edu.gamificacaocursos.recomendacao.domain.TrechoDeDocumento;
import br.edu.gamificacaocursos.shared.web.ServicoExternoIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ConhecimentoHttpAdapterTest {

    private MockRestServiceServer servidor;
    private ConhecimentoHttpAdapter adapter;

    @BeforeEach
    void prepara() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://conhecimento:8000");
        servidor = MockRestServiceServer.bindTo(builder).build();
        adapter = new ConhecimentoHttpAdapter(builder.build());
    }

    @Test
    @DisplayName("Consulta /buscar com a pergunta e a quantidade e converte os trechos")
    void busca() {
        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith("http://conhecimento:8000/buscar")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("q", "mensageria%20cursos"))
                .andExpect(queryParam("k", "4"))
                .andRespond(withSuccess("{\"consulta\":\"mensageria cursos\",\"trechos\":[{\"id\":\"trilha#3\","
                        + "\"documento\":\"Trilha\",\"fonte\":\"trilha.md\",\"secao\":\"Mensageria\","
                        + "\"texto\":\"Mensageria com RabbitMQ\",\"relevancia\":0.41}]}", MediaType.APPLICATION_JSON));

        List<TrechoDeDocumento> trechos = adapter.buscar("mensageria cursos", 4);

        assertEquals(1, trechos.size());
        assertEquals("trilha#3", trechos.get(0).id());
        assertEquals(0.41, trechos.get(0).relevancia());
        servidor.verify();
    }

    @Test
    @DisplayName("Resposta sem trechos vira lista vazia")
    void respostaSemTrechos() {
        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith("http://conhecimento:8000/buscar")))
                .andRespond(withSuccess("{\"consulta\":\"x\"}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith("http://conhecimento:8000/buscar")))
                .andRespond(withSuccess());

        assertEquals(List.of(), adapter.buscar("x", 4));
        assertEquals(List.of(), adapter.buscar("x", 4));
    }

    @Test
    @DisplayName("Erro no servico de documentos vira ServicoExternoIndisponivel")
    void servicoFora() {
        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith("http://conhecimento:8000/buscar")))
                .andRespond(withServerError());

        ServicoExternoIndisponivelException erro = assertThrows(ServicoExternoIndisponivelException.class,
                () -> adapter.buscar("java", 4));
        assertEquals("Servico de documentos indisponivel no momento", erro.getMessage());
    }
}
