package br.edu.gamificacaocursos.treinamento.internal.mensageria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class MensageriaConfig {

    @Bean
    public Queue filaDeSolicitacoesDeTreinamento(@Value("${treinamento.fila.solicitacoes}") String nome) {
        return new Queue(nome, true);
    }

    @Bean
    public Queue filaDeAndamentoDosTreinamentos(@Value("${treinamento.fila.andamento}") String nome) {
        return new Queue(nome, true);
    }

    @Bean
    public MessageConverter conversorJson(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
