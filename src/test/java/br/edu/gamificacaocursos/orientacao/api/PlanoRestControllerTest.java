package br.edu.gamificacaocursos.orientacao.api;

import br.edu.gamificacaocursos.colaborador.internal.persistence.ColaboradorRepository;
import br.edu.gamificacaocursos.competencia.internal.persistence.AvaliacaoDeCompetenciaRepository;
import br.edu.gamificacaocursos.orientacao.internal.persistence.PlanoDeEstudosRepository;
import br.edu.gamificacaocursos.recomendacao.application.BuscaDeDocumentos;
import br.edu.gamificacaocursos.recomendacao.domain.TrechoDeDocumento;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class PlanoRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ColaboradorRepository colaboradores;

    @Autowired
    private AvaliacaoDeCompetenciaRepository avaliacoes;

    @Autowired
    private PlanoDeEstudosRepository planos;

    @MockitoBean
    private BuscaDeDocumentos documentos;

    private String colaboradorId;

    @BeforeEach
    void prepara() throws Exception {
        planos.deleteAll();
        avaliacoes.deleteAll();
        colaboradores.deleteAll();
        String corpo = mockMvc.perform(post("/api/colaboradores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Caio\",\"email\":\"caio@empresa.com\",\"cargo\":\"Dev\",\"area\":\"TI\"}"))
                .andReturn().getResponse().getContentAsString();
        colaboradorId = corpo.replaceAll(".*\"id\":(\\d+).*", "$1");
        when(documentos.buscar(anyString(), anyInt())).thenReturn(List.of(new TrechoDeDocumento(
                "trilha-iniciante#2", "Trilha Iniciante", "trilha-iniciante.md", "Cursos da trilha iniciante",
                "Os cursos sao Fundamentos de Java, HTTP e APIs REST e SQL Essencial.", 0.5)));
    }

    private String proporPlano() throws Exception {
        String corpo = mockMvc.perform(post("/api/colaboradores/" + colaboradorId + "/planos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objetivo\":\"comecar a programar\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_AUTORIZACAO"))
                .andExpect(jsonPath("$.cursosSugeridos.length()").value(3))
                .andExpect(jsonPath("$.etapas.length()").value(0))
                .andExpect(jsonPath("$.modo").value("agente deterministico, sem LLM"))
                .andExpect(jsonPath("$.registrosDosAgentes[0].agente").value("agente-orientador"))
                .andExpect(jsonPath("$.acompanhamento.situacao")
                        .value("Aguardando a autorizacao do colaborador. Nenhuma acao foi executada."))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.id").toString();
    }

    private ResultActions decidir(String planoId, String corpo) throws Exception {
        return mockMvc.perform(post("/api/planos/" + planoId + "/decisao")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    @Test
    @DisplayName("Fluxo completo: o agente propoe, o colaborador autoriza, as etapas sao criadas e concluidas")
    void fluxoAutorizado() throws Exception {
        String planoId = proporPlano();

        String autorizado = decidir(planoId, "{\"autorizado\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTORIZADO"))
                .andExpect(jsonPath("$.etapas.length()").value(3))
                .andExpect(jsonPath("$.etapas[0].tituloDoCurso").value("Fundamentos de Java"))
                .andExpect(jsonPath("$.registrosDosAgentes[-1:].ferramenta").value("criarEtapas"))
                .andReturn().getResponse().getContentAsString();
        List<Integer> etapas = JsonPath.read(autorizado, "$.etapas[*].id");

        for (Integer etapa : etapas) {
            mockMvc.perform(post("/api/planos/" + planoId + "/etapas/" + etapa + "/conclusao"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/planos/" + planoId))
                .andExpect(jsonPath("$.status").value("CONCLUIDO"))
                .andExpect(jsonPath("$.acompanhamento.percentualConcluido").value(100));
        mockMvc.perform(get("/api/colaboradores/" + colaboradorId + "/planos"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Plano recusado nao cria etapas e nao pode ser decidido de novo (409)")
    void fluxoRecusado() throws Exception {
        String planoId = proporPlano();

        decidir(planoId, "{\"autorizado\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECUSADO"))
                .andExpect(jsonPath("$.etapas.length()").value(0));
        decidir(planoId, "{\"autorizado\":true}")
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/planos/" + planoId + "/etapas/1/conclusao"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Decisao sem resposta, plano inexistente e permissoes publicadas")
    void validacoesEPermissoes() throws Exception {
        decidir("1", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.autorizado").value("Informe se o plano foi autorizado"));
        mockMvc.perform(get("/api/planos/999999"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/colaboradores/" + colaboradorId + "/planos"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/agentes/permissoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['agente-orientador'].length()").value(3))
                .andExpect(jsonPath("$['agente-de-acompanhamento'][2]").value("criarEtapas"));
    }
}
