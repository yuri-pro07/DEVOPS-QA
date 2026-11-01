package br.edu.gamificacaocursos.ranking.api;

import java.util.List;

public record RankingPaginaDTO(int pagina, int tamanho, long totalDeAlunos, int totalDePaginas,
                               List<RankingItemDTO> itens) {
}
