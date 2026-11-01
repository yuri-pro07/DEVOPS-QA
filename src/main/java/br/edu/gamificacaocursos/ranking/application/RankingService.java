package br.edu.gamificacaocursos.ranking.application;

import br.edu.gamificacaocursos.ranking.api.RankingItemDTO;
import br.edu.gamificacaocursos.ranking.api.RankingPaginaDTO;
import br.edu.gamificacaocursos.ranking.internal.persistence.LinhaDoRanking;
import br.edu.gamificacaocursos.ranking.internal.persistence.RankingRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RankingService {

    static final int TAMANHO_MAXIMO_DA_PAGINA = 100;

    private final RankingRepository repository;
    private final Timer tempoDeConsulta;

    public RankingService(RankingRepository repository, MeterRegistry metricas) {
        this.repository = repository;
        this.tempoDeConsulta = Timer.builder("gamificacao.ranking.consulta")
                .description("Tempo para montar uma pagina do ranking")
                .register(metricas);
    }

    @Transactional(readOnly = true)
    public RankingPaginaDTO consultar(int pagina, int tamanho) {
        if (pagina < 0) {
            throw new IllegalArgumentException("A pagina nao pode ser negativa");
        }
        if (tamanho < 1 || tamanho > TAMANHO_MAXIMO_DA_PAGINA) {
            throw new IllegalArgumentException("O tamanho da pagina deve ficar entre 1 e " + TAMANHO_MAXIMO_DA_PAGINA);
        }

        Page<LinhaDoRanking> resultado = tempoDeConsulta.record(
                () -> repository.buscarRanking(PageRequest.of(pagina, tamanho)));

        long primeiraPosicao = (long) pagina * tamanho + 1;
        List<RankingItemDTO> itens = new ArrayList<>();
        for (LinhaDoRanking linha : resultado.getContent()) {
            itens.add(new RankingItemDTO(primeiraPosicao + itens.size(),
                    linha.aluno(), linha.moedas(), linha.diasConsecutivos()));
        }
        return new RankingPaginaDTO(pagina, tamanho, resultado.getTotalElements(), resultado.getTotalPages(), itens);
    }

    @Transactional(readOnly = true)
    public RankingItemDTO posicaoDe(String aluno) {
        LinhaDoRanking linha = repository.buscarPorAluno(aluno)
                .orElseThrow(() -> new IllegalArgumentException("Aluno fora do ranking: " + aluno));
        long aFrente = repository.contarAFrente(linha.moedas(), linha.diasConsecutivos(), linha.aluno());
        return new RankingItemDTO(aFrente + 1, linha.aluno(), linha.moedas(), linha.diasConsecutivos());
    }
}
