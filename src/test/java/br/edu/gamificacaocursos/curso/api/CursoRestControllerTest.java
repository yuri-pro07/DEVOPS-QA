package br.edu.gamificacaocursos.curso.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class CursoRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions cadastrar(String titulo, String nivel, int cargaHoraria) throws Exception {
        return mockMvc.perform(post("/api/cursos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"" + titulo + "\",\"descricao\":\"Conteudo do curso\","
                        + "\"area\":\"Gestao\",\"nivel\":\"" + nivel + "\",\"cargaHoraria\":" + cargaHoraria + "}"));
    }

    @Test
    @DisplayName("O catalogo inicial ja vem carregado e filtra por nivel")
    void filtraPorNivel() throws Exception {
        mockMvc.perform(get("/api/cursos").param("nivel", "AVANCADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$[*].nivel", everyItem(is("AVANCADO"))));
    }

    @Test
    @DisplayName("O filtro por area ignora maiusculas e minusculas")
    void filtraPorArea() throws Exception {
        mockMvc.perform(get("/api/cursos").param("area", "dados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[*].area", everyItem(is("Dados"))));
    }

    @Test
    @DisplayName("POST /api/cursos cadastra, GET consulta pelo id e titulo repetido devolve 400")
    void cadastraEConsulta() throws Exception {
        String corpo = cadastrar("Lideranca Tecnica", "INTERMEDIARIO", 16)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = corpo.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(get("/api/cursos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Lideranca Tecnica"))
                .andExpect(jsonPath("$.nivel").value("INTERMEDIARIO"));

        cadastrar("lideranca tecnica", "AVANCADO", 10)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    @DisplayName("Carga horaria fora do limite devolve 400 e id inexistente tambem")
    void validaCampos() throws Exception {
        cadastrar("Curso Relampago", "INICIANTE", 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.cargaHoraria").value("Carga horaria minima de 1 hora"));

        mockMvc.perform(get("/api/cursos/999999"))
                .andExpect(status().isBadRequest());
    }
}
