package br.edu.gamificacaocursos.competencia.domain;

public record PerfilDeCompetencia(int mesesExperiencia, int cursosConcluidos, double mediaAvaliacoes,
                                  int horasSemanais, boolean conheceRest, boolean conheceBancoDeDados) {
}
