package br.edu.gamificacaocursos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Camada: CONFIGURACAO.
// Monta a documentacao OpenAPI que o Swagger UI exibe em /swagger-ui.html
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Case A - Gamificacao para Engajamento de Educacao Continuada")
                .version("1.0")
                .description("API da US1: ofensiva de dias consecutivos de estudo, "
                        + "com recompensa automatica ao completar 7 dias seguidos.")
                .contact(new Contact().name("Yuri Peruzzo, Enzo Zorzetto, Pedro Ricci Gomes Nascimento")));
    }
}
