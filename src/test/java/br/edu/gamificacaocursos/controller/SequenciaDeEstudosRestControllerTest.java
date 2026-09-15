package br.edu.gamificacaocursos.controller;

import br.edu.gamificacaocursos.repository.SequenciaDeEstudosRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Testes da camada CONTROLLER: exercitam os endpoints REST de ponta a ponta,
// passando por service, repository e banco H2.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class SequenciaDeEstudosRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SequenciaDeEstudosRepository repository;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    private Long criarSequencia(String aluno) throws Exception {
        String corpo = mockMvc.perform(post("/api/sequencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aluno\":\"" + aluno + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(corpo).get("id").asLong();
    }

    @Test
    @DisplayName("POST /api/sequencias cria e devolve 201")
    void criaSequencia() throws Exception {
        mockMvc.perform(post("/api/sequencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aluno\":\"Rafael\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aluno").value("Rafael"))
                .andExpect(jsonPath("$.diasConsecutivos").value(0));
    }

    @Test
    @DisplayName("GET /api/sequencias lista o que foi criado")
    void listaSequencias() throws Exception {
        criarSequencia("Rafael");

        mockMvc.perform(get("/api/sequencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].aluno").value("Rafael"));
    }

    @Test
    @DisplayName("GET /api/sequencias/{id} devolve uma sequência")
    void buscaPorId() throws Exception {
        Long id = criarSequencia("Rafael");

        mockMvc.perform(get("/api/sequencias/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    @DisplayName("POST /{id}/estudos registra o dia e atualiza a ofensiva")
    void registraEstudo() throws Exception {
        Long id = criarSequencia("Aline");

        mockMvc.perform(post("/api/sequencias/" + id + "/estudos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"2026-09-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diasConsecutivos").value(1))
                .andExpect(jsonPath("$.ultimoDiaEstudado").value("2026-09-01"));
    }

    @Test
    @DisplayName("DELETE /api/sequencias/{id} devolve 204")
    void removeSequencia() throws Exception {
        Long id = criarSequencia("Rafael");

        mockMvc.perform(delete("/api/sequencias/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/sequencias"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("aluno em branco cai na validação do @Valid e devolve 400 com o campo")
    void alunoEmBrancoFalhaValidacao() throws Exception {
        mockMvc.perform(post("/api/sequencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aluno\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.aluno").exists());
    }

    @Test
    @DisplayName("erro de regra de negócio devolve 400 com a mensagem")
    void regraDeNegocioDevolve400() throws Exception {
        mockMvc.perform(get("/api/sequencias/9999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }
}
