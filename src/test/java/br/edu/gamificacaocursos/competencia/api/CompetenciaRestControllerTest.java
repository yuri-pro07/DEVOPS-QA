package br.edu.gamificacaocursos.competencia.api;

import br.edu.gamificacaocursos.colaborador.internal.persistence.ColaboradorRepository;
import br.edu.gamificacaocursos.competencia.application.ClassificadorDeCompetencia;
import br.edu.gamificacaocursos.competencia.domain.Classificacao;
import br.edu.gamificacaocursos.competencia.internal.persistence.AvaliacaoDeCompetenciaRepository;
import br.edu.gamificacaocursos.shared.web.ServicoExternoIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia.AVANCADO;
import static br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia.INICIANTE;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("h2")
class CompetenciaRestControllerTest {

    private static final String PERFIL = "{\"mesesExperiencia\":6,\"cursosConcluidos\":1,\"mediaAvaliacoes\":6.0,"
            + "\"horasSemanais\":5,\"conheceRest\":false,\"conheceBancoDeDados\":false}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ColaboradorRepository colaboradores;

    @Autowired
    private AvaliacaoDeCompetenciaRepository avaliacoes;

    @MockitoBean
    private ClassificadorDeCompetencia classificador;

    private String colaboradorId;

    @BeforeEach
    void prepara() throws Exception {
        avaliacoes.deleteAll();
        colaboradores.deleteAll();
        String corpo = mockMvc.perform(post("/api/colaboradores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@empresa.com\",\"cargo\":\"Dev\",\"area\":\"TI\"}"))
                .andReturn().getResponse().getContentAsString();
        colaboradorId = corpo.replaceAll(".*\"id\":(\\d+).*", "$1");
    }

    private ResultActions avaliar(String id, String perfil) throws Exception {
        return mockMvc.perform(post("/api/colaboradores/" + id + "/competencias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(perfil));
    }

    @Test
    @DisplayName("POST classifica pelo ML, grava e o GET devolve o historico do mais recente ao mais antigo")
    void avaliaEConsultaHistorico() throws Exception {
        when(classificador.classificar(any())).thenReturn(
                new Classificacao(INICIANTE, Map.of("INICIANTE", 0.9, "INTERMEDIARIO", 0.1, "AVANCADO", 0.0), "rl v1"),
                new Classificacao(AVANCADO, Map.of("INICIANTE", 0.0, "INTERMEDIARIO", 0.2, "AVANCADO", 0.8), "rl v1"));

        avaliar(colaboradorId, PERFIL)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nivel").value("INICIANTE"))
                .andExpect(jsonPath("$.confianca").value(0.9))
                .andExpect(jsonPath("$.perfil.mesesExperiencia").value(6));
        avaliar(colaboradorId, PERFIL).andExpect(status().isCreated());

        mockMvc.perform(get("/api/colaboradores/" + colaboradorId + "/competencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nivel").value("AVANCADO"))
                .andExpect(jsonPath("$[1].nivel").value("INICIANTE"));
    }

    @Test
    @DisplayName("ML fora do ar devolve 503 e nao grava avaliacao")
    void mlForaDoAr() throws Exception {
        when(classificador.classificar(any()))
                .thenThrow(new ServicoExternoIndisponivelException("Servico de ML indisponivel no momento"));

        avaliar(colaboradorId, PERFIL)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.erro").value("Servico de ML indisponivel no momento"));

        mockMvc.perform(get("/api/colaboradores/" + colaboradorId + "/competencias"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Colaborador inexistente e media acima de 10 devolvem 400")
    void validaEntrada() throws Exception {
        avaliar("999999", PERFIL)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Colaborador nao encontrado: 999999"));

        avaliar(colaboradorId, PERFIL.replace("6.0", "11.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.mediaAvaliacoes").value("Media maxima 10"));
    }

    @Test
    @DisplayName("O tempo de classificacao aparece no Prometheus com a faixa de 2 segundos do RNF01")
    void expoeMetricaDeClassificacao() throws Exception {
        when(classificador.classificar(any())).thenReturn(
                new Classificacao(INICIANTE, Map.of("INICIANTE", 1.0), "rl v1"));
        avaliar(colaboradorId, PERFIL);

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("gamificacao_competencia_classificacao_seconds_count")))
                .andExpect(content().string(containsString("le=\"2.0\"")));
    }
}
