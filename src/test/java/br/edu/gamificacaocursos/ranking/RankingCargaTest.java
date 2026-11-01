package br.edu.gamificacaocursos.ranking;

import br.edu.gamificacaocursos.ranking.api.RankingItemDTO;
import br.edu.gamificacaocursos.ranking.api.RankingPaginaDTO;
import br.edu.gamificacaocursos.ranking.application.RankingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("h2")
class RankingCargaTest {

    static final int ALUNOS = 100_000;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RankingService ranking;

    @BeforeEach
    void carrega() {
        jdbc.execute("DELETE FROM sequencias_de_estudos");
        jdbc.execute("INSERT INTO sequencias_de_estudos (aluno, dias_consecutivos, moedas_conquistadas) "
                + "SELECT 'aluno-' || LPAD(X, 6, '0'), MOD(X * 7, 31), MOD(X * 13, 52) "
                + "FROM SYSTEM_RANGE(1, " + ALUNOS + ")");
    }

    @AfterEach
    void limpa() {
        jdbc.execute("DELETE FROM sequencias_de_estudos");
    }

    @Test
    @DisplayName("Com 100.000 alunos, uma pagina traz so 20 linhas e na ordem certa")
    void paginaComCemMilAlunos() {
        long inicio = System.nanoTime();
        RankingPaginaDTO pagina = ranking.consultar(0, 20);
        long ms = (System.nanoTime() - inicio) / 1_000_000;

        assertEquals(ALUNOS, pagina.totalDeAlunos());
        assertEquals(5_000, pagina.totalDePaginas());
        assertEquals(20, pagina.itens().size());

        int maiorMoeda = jdbc.queryForObject("SELECT MAX(moedas_conquistadas) FROM sequencias_de_estudos", Integer.class);
        assertEquals(maiorMoeda, pagina.itens().get(0).moedas());
        for (int i = 1; i < pagina.itens().size(); i++) {
            RankingItemDTO anterior = pagina.itens().get(i - 1);
            RankingItemDTO atual = pagina.itens().get(i);
            assertTrue(anterior.moedas() > atual.moedas()
                    || (anterior.moedas() == atual.moedas() && anterior.diasConsecutivos() >= atual.diasConsecutivos()));
        }
        System.out.println("RANKING_100K pagina 0 (20 itens) em " + ms + " ms");
    }

    @Test
    @DisplayName("A posicao calculada bate com a posicao real na listagem")
    void posicaoBateComAListagem() {
        RankingPaginaDTO pagina = ranking.consultar(3, 50);
        RankingItemDTO alvo = pagina.itens().get(17);

        long inicio = System.nanoTime();
        RankingItemDTO calculada = ranking.posicaoDe(alvo.aluno());
        long ms = (System.nanoTime() - inicio) / 1_000_000;

        assertEquals(alvo.posicao(), calculada.posicao());
        assertEquals(168, calculada.posicao());
        System.out.println("RANKING_100K posicao de " + alvo.aluno() + " = " + calculada.posicao() + " em " + ms + " ms");
    }

    @Test
    @DisplayName("O banco usa o indice idx_ranking para ordenar a consulta")
    void consultaUsaOIndiceDoRanking() {
        String plano = String.join("\n", jdbc.queryForList(
                "EXPLAIN SELECT aluno, moedas_conquistadas, dias_consecutivos FROM sequencias_de_estudos "
                        + "ORDER BY moedas_conquistadas DESC, dias_consecutivos DESC, aluno ASC LIMIT 20",
                String.class));

        System.out.println("RANKING_100K plano:\n" + plano);
        assertTrue(plano.toUpperCase().contains("IDX_RANKING"), plano);
    }
}
