package br.edu.gamificacaocursos.competencia.application;

import br.edu.gamificacaocursos.colaborador.application.ColaboradorService;
import br.edu.gamificacaocursos.competencia.api.AvaliacaoResponseDTO;
import br.edu.gamificacaocursos.competencia.domain.AvaliacaoDeCompetencia;
import br.edu.gamificacaocursos.competencia.domain.Classificacao;
import br.edu.gamificacaocursos.competencia.domain.PerfilDeCompetencia;
import br.edu.gamificacaocursos.competencia.internal.persistence.AvaliacaoDeCompetenciaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CompetenciaService {
    private final ColaboradorService colaboradores;
    private final ClassificadorDeCompetencia classificador;
    private final AvaliacaoDeCompetenciaRepository repository;
    private final Timer tempoDeClassificacao;

    public CompetenciaService(ColaboradorService colaboradores, ClassificadorDeCompetencia classificador,
                              AvaliacaoDeCompetenciaRepository repository, MeterRegistry metricas) {
        this.colaboradores = colaboradores;
        this.classificador = classificador;
        this.repository = repository;
        this.tempoDeClassificacao = Timer.builder("gamificacao.competencia.classificacao")
                .description("Tempo da classificacao de competencia feita pelo servico de ML")
                .register(metricas);
    }

    public AvaliacaoResponseDTO avaliar(Long colaboradorId, PerfilDeCompetencia perfil) {
        colaboradores.buscarPorId(colaboradorId);
        Classificacao classificacao = tempoDeClassificacao.record(() -> classificador.classificar(perfil));
        return toDTO(repository.save(
                new AvaliacaoDeCompetencia(colaboradorId, perfil, classificacao, LocalDateTime.now())));
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoResponseDTO> historico(Long colaboradorId) {
        colaboradores.buscarPorId(colaboradorId);
        return repository.findByColaboradorIdOrderByAvaliadoEmDescIdDesc(colaboradorId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<AvaliacaoResponseDTO> ultimaAvaliacao(Long colaboradorId) {
        return repository.findFirstByColaboradorIdOrderByAvaliadoEmDescIdDesc(colaboradorId).map(this::toDTO);
    }

    private AvaliacaoResponseDTO toDTO(AvaliacaoDeCompetencia avaliacao) {
        return new AvaliacaoResponseDTO(avaliacao.getId(), avaliacao.getColaboradorId(), avaliacao.getNivel(),
                avaliacao.getConfianca(), avaliacao.getProbabilidades(), avaliacao.getModelo(),
                avaliacao.getAvaliadoEm(), avaliacao.getPerfil());
    }
}
