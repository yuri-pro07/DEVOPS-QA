package br.edu.gamificacaocursos.orientacao.application;

import br.edu.gamificacaocursos.shared.web.AcessoNegadoException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Component
public class PoliticaDeAcessoDosAgentes {
    public static final String AGENTE_ORIENTADOR = "agente-orientador";
    public static final String AGENTE_DE_ACOMPANHAMENTO = "agente-de-acompanhamento";

    private static final Map<String, Set<String>> PERMISSOES = Map.of(
            AGENTE_ORIENTADOR, Set.of("consultarNivel", "buscarDocumentos", "consultarCurso"),
            AGENTE_DE_ACOMPANHAMENTO, Set.of("consultarDisponibilidade", "consultarCurso", "criarEtapas"));

    public void verificar(String agente, String ferramenta) {
        if (!PERMISSOES.getOrDefault(agente, Set.of()).contains(ferramenta)) {
            throw new AcessoNegadoException("O " + agente + " nao tem permissao para usar a ferramenta " + ferramenta);
        }
    }

    public Map<String, List<String>> permissoes() {
        Map<String, List<String>> ordenadas = new TreeMap<>();
        PERMISSOES.forEach((agente, ferramentas) -> ordenadas.put(agente, ferramentas.stream().sorted().toList()));
        return ordenadas;
    }
}
