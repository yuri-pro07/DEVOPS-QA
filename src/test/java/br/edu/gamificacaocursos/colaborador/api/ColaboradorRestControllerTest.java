package br.edu.gamificacaocursos.colaborador.api;

import br.edu.gamificacaocursos.colaborador.internal.persistence.ColaboradorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class ColaboradorRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ColaboradorRepository repository;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    private ResultActions cadastrar(String nome, String email) throws Exception {
        return mockMvc.perform(post("/api/colaboradores")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"" + nome + "\",\"email\":\"" + email
                        + "\",\"cargo\":\"Analista\",\"area\":\"TI\"}"));
    }

    @Test
    @DisplayName("POST /api/colaboradores cadastra e devolve 201")
    void cadastra() throws Exception {
        cadastrar("Ana Souza", "Ana@Empresa.com")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("ana@empresa.com"));
    }

    @Test
    @DisplayName("Email repetido devolve 400 com a mensagem da regra")
    void recusaEmailRepetido() throws Exception {
        cadastrar("Ana", "ana@empresa.com");

        cadastrar("Ana Clara", "ana@empresa.com")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Ja existe um colaborador com o email ana@empresa.com"));
    }

    @Test
    @DisplayName("Campos invalidos devolvem 400 por campo")
    void validaCampos() throws Exception {
        cadastrar("", "nao-e-email")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists())
                .andExpect(jsonPath("$.erros.email").value("Email invalido"));
    }

    @Test
    @DisplayName("GET lista os colaboradores e consulta pelo id")
    void listaEConsulta() throws Exception {
        String corpo = cadastrar("Bruno", "bruno@empresa.com").andReturn().getResponse().getContentAsString();
        String id = corpo.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(get("/api/colaboradores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Bruno"));

        mockMvc.perform(get("/api/colaboradores/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("bruno@empresa.com"));

        mockMvc.perform(get("/api/colaboradores/999999"))
                .andExpect(status().isBadRequest());
    }
}
