package br.edu.gamificacaocursos.competencia.domain;

import br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia;

import java.util.Map;

public record Classificacao(NivelDeCompetencia nivel, Map<String, Double> probabilidades, String modelo) {

    public double probabilidadeDe(NivelDeCompetencia alvo) {
        return probabilidades.getOrDefault(alvo.name(), 0.0);
    }

    public double confianca() {
        return probabilidadeDe(nivel);
    }
}
