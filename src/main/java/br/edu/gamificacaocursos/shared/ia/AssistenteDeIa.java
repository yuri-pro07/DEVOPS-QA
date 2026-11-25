package br.edu.gamificacaocursos.shared.ia;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AssistenteDeIa {
    static final String SEM_CHAVE = "sem-chave";

    private final ChatClient cliente;
    private final boolean habilitada;
    private final String modelo;

    public AssistenteDeIa(ChatClient.Builder builder, @Value("${ia.chave}") String chave,
                          @Value("${ia.modelo}") String modelo) {
        this.cliente = builder.build();
        this.habilitada = chave != null && !chave.isBlank() && !SEM_CHAVE.equals(chave);
        this.modelo = modelo;
    }

    public boolean habilitada() {
        return habilitada;
    }

    public ChatClient cliente() {
        return cliente;
    }

    public String descricao() {
        return "Spring AI com OpenAI " + modelo;
    }
}
