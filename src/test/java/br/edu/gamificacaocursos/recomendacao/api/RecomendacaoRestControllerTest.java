package br.edu.gamificacaocursos.recomendacao.api;

import br.edu.gamificacaocursos.colaborador.internal.persistence.ColaboradorRepository;
import br.edu.gamificacaocursos.competencia.internal.persistence.AvaliacaoDeCompetenciaRepository;
import br.edu.gamificacaocursos.orientacao.internal.persistence.PlanoDeEstudosRepository;
import br.edu.gamificacaocursos.recomendacao.application.BuscaDeDocumentos;
import br.edu.gamificacaocursos.recomendacao.domain.TrechoDeDocumento;
import br.edu.gamificacaocursos.shared.web.ServicoExternoIndisponivelException;
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

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class RecomendacaoRestControllerTest {

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
                        .content("{\"nome\":\"Bia\",\"email\":\"bia@empresa.com\",\"cargo\":\"Dev\",\"area\":\"TI\"}"))
                .andReturn().getResponse().getContentAsString();
        colaboradorId = corpo.replaceAll(".*\"id\":(\\d+).*", "$1");
    }

    @Test
    @DisplayName("Sem chave da OpenAI recomenda cursos do catalogo citados nos documentos, com as fontes")
    void recomenda() throws Exception {
        when(documentos.buscar(anyString(), anyInt())).thenReturn(List.of(new TrechoDeDocumento(
                "trilha-intermediaria#3", "Trilha Intermediaria", "trilha-intermediaria.md", "Mensageria",
                "Para filas e mensagens o curso indicado e Mensageria com RabbitMQ.", 0.42)));

        mockMvc.perform(post("/api/colaboradores/" + colaboradorId + "/recomendacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objetivo\":\"mensageria\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modo").value("extrativo, sem LLM"))
                .andExpect(jsonPath("$.cursos[0].titulo").value("Mensageria com RabbitMQ"))
                .andExpect(jsonPath("$.cursos[0].fonte").value("trilha-intermediaria#3"))
                .andExpect(jsonPath("$.fontes[0].fonte").value("trilha-intermediaria.md"))
                .andExpect(jsonPath("$.resposta").value(containsString("Mensageria com RabbitMQ (18 h)")));
    }

    @Test
    @DisplayName("Funciona sem corpo, recusa objetivo longo e colaborador inexistente")
    void validaEntrada() throws Exception {
        when(documentos.buscar(anyString(), anyInt())).thenReturn(List.of());

        mockMvc.perform(post("/api/colaboradores/" + colaboradorId + "/recomendacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modo").value("sem fontes"));
        mockMvc.perform(post("/api/colaboradores/" + colaboradorId + "/recomendacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objetivo\":\"" + "a".repeat(301) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.objetivo").value("O objetivo deve ter no maximo 300 caracteres"));
        mockMvc.perform(post("/api/colaboradores/999999/recomendacoes"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Servico de documentos fora do ar devolve 503 sem afetar o resto da aplicacao")
    void documentosForaDoAr() throws Exception {
        when(documentos.buscar(anyString(), anyInt()))
                .thenThrow(new ServicoExternoIndisponivelException("Servico de documentos indisponivel no momento"));

        mockMvc.perform(post("/api/colaboradores/" + colaboradorId + "/recomendacoes"))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/cursos"))
                .andExpect(status().isOk());
    }
}
