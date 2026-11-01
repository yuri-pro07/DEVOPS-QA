package br.edu.gamificacaocursos.ranking.api;

public record RankingItemDTO(long posicao, String aluno, int moedas, int diasConsecutivos) {
}
