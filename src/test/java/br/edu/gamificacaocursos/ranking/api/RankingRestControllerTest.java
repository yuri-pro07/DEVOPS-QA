package br.edu.gamificacaocursos.ranking.api;

import br.edu.gamificacaocursos.ofensiva.internal.persistence.SequenciaDeEstudosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("h2")
class RankingRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SequenciaDeEstudosRepository repository;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    private void criarComEstudos(String aluno, int dias) throws Exception {
        String corpo = mockMvc.perform(post("/api/sequencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aluno\":\"" + aluno + "\"}"))
                .andReturn().getResponse().getContentAsString();
        String id = corpo.replaceAll(".*\"id\":(\\d+).*", "$1");
        for (int i = 1; i <= dias; i++) {
            mockMvc.perform(post("/api/sequencias/" + id + "/estudos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"data\":\"2026-09-" + String.format("%02d", i) + "\"}"));
        }
    }

    @Test
    @DisplayName("GET /api/ranking ordena por moedas e depois por dias consecutivos")
    void ordenaORanking() throws Exception {
        criarComEstudos("Aline", 3);
        criarComEstudos("Pedro", 7);
        criarComEstudos("Rafael", 5);

        mockMvc.perform(get("/api/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDeAlunos").value(3))
                .andExpect(jsonPath("$.itens[0].aluno").value("Pedro"))
                .andExpect(jsonPath("$.itens[0].posicao").value(1))
                .andExpect(jsonPath("$.itens[0].moedas").value(1))
                .andExpect(jsonPath("$.itens[1].aluno").value("Rafael"))
                .andExpect(jsonPath("$.itens[2].aluno").value("Aline"));
    }

    @Test
    @DisplayName("GET /api/ranking/alunos/{aluno} devolve a posicao")
    void devolveAPosicaoDoAluno() throws Exception {
        criarComEstudos("Aline", 3);
        criarComEstudos("Pedro", 7);

        mockMvc.perform(get("/api/ranking/alunos/Aline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posicao").value(2));
    }

    @Test
    @DisplayName("Pagina acima do limite devolve 400")
    void recusaPaginaGrandeDemais() throws Exception {
        mockMvc.perform(get("/api/ranking").param("tamanho", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    @DisplayName("As metricas do ranking aparecem no endpoint do Prometheus")
    void expoeMetricasParaOPrometheus() throws Exception {
        mockMvc.perform(get("/api/ranking"));

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("gamificacao_ranking_consulta_seconds_count")))
                .andExpect(content().string(containsString("gamificacao_ofensiva_estudos_total")));
    }
}
