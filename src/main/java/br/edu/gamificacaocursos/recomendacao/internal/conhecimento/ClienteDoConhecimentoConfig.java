package br.edu.gamificacaocursos.recomendacao.internal.conhecimento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ClienteDoConhecimentoConfig {

    @Bean
    public RestClient clienteDoConhecimento(RestClient.Builder builder,
                                            @Value("${conhecimento.url}") String url,
                                            @Value("${conhecimento.timeout}") Duration timeout) {
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(timeout);
        return builder.baseUrl(url).requestFactory(fabrica).build();
    }
}
