package br.edu.gamificacaocursos.orientacao.application;

import br.edu.gamificacaocursos.colaborador.application.ColaboradorService;
import br.edu.gamificacaocursos.curso.application.CursoService;
import br.edu.gamificacaocursos.orientacao.api.CursoSugeridoDTO;
import br.edu.gamificacaocursos.orientacao.api.EtapaDTO;
import br.edu.gamificacaocursos.orientacao.api.PlanoResponseDTO;
import br.edu.gamificacaocursos.orientacao.api.RegistroDeAgenteDTO;
import br.edu.gamificacaocursos.orientacao.domain.PlanoDeEstudos;
import br.edu.gamificacaocursos.orientacao.domain.PropostaDeOrientacao;
import br.edu.gamificacaocursos.orientacao.internal.persistence.PlanoDeEstudosRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrientacaoService {
    private final ColaboradorService colaboradores;
    private final CursoService cursos;
    private final AgenteOrientador orientador;
    private final AgenteDeAcompanhamento acompanhamento;
    private final PlanoDeEstudosRepository repository;

    public OrientacaoService(ColaboradorService colaboradores, CursoService cursos, AgenteOrientador orientador,
                             AgenteDeAcompanhamento acompanhamento, PlanoDeEstudosRepository repository) {
        this.colaboradores = colaboradores;
        this.cursos = cursos;
        this.orientador = orientador;
        this.acompanhamento = acompanhamento;
        this.repository = repository;
    }

    @Transactional
    public PlanoResponseDTO propor(Long colaboradorId, String objetivo) {
        colaboradores.buscarPorId(colaboradorId);
        String objetivoLimpo = objetivo == null ? "" : objetivo.trim();
        PropostaDeOrientacao proposta = orientador.propor(colaboradorId, objetivoLimpo);
        if (proposta.cursoIds().isEmpty()) {
            throw new IllegalArgumentException(proposta.orientacao());
        }
        PlanoDeEstudos plano = new PlanoDeEstudos(colaboradorId, proposta.nivel(), objetivoLimpo,
                proposta.orientacao(), proposta.modo(), proposta.cursoIds(), LocalDateTime.now());
        proposta.registros().forEach(plano::registrar);
        return toDTO(repository.save(plano));
    }

    @Transactional
    public PlanoResponseDTO decidir(Long planoId, boolean autorizado) {
        PlanoDeEstudos plano = buscar(planoId);
        plano.exigirAguardandoAutorizacao();
        if (autorizado) {
            plano.autorizar(acompanhamento.criarEtapas(plano, LocalDate.now()), LocalDateTime.now());
        } else {
            plano.recusar(LocalDateTime.now());
        }
        return toDTO(repository.saveAndFlush(plano));
    }

    @Transactional
    public PlanoResponseDTO concluirEtapa(Long planoId, Long etapaId) {
        PlanoDeEstudos plano = buscar(planoId);
        plano.concluirEtapa(etapaId, LocalDate.now());
        return toDTO(repository.saveAndFlush(plano));
    }

    @Transactional(readOnly = true)
    public PlanoResponseDTO consultar(Long planoId) {
        return toDTO(buscar(planoId));
    }

    @Transactional(readOnly = true)
    public List<PlanoResponseDTO> listar(Long colaboradorId) {
        colaboradores.buscarPorId(colaboradorId);
        return repository.findByColaboradorIdOrderByCriadoEmDescIdDesc(colaboradorId).stream()
                .map(this::toDTO)
                .toList();
    }

    private PlanoDeEstudos buscar(Long planoId) {
        return repository.findById(planoId)
                .orElseThrow(() -> new IllegalArgumentException("Plano nao encontrado: " + planoId));
    }

    private PlanoResponseDTO toDTO(PlanoDeEstudos plano) {
        List<CursoSugeridoDTO> sugeridos = plano.getCursosSugeridos().stream()
                .map(cursos::buscarPorId)
                .map(curso -> new CursoSugeridoDTO(curso.id(), curso.titulo(), curso.nivel(), curso.cargaHoraria()))
                .toList();
        List<EtapaDTO> etapas = plano.getEtapas().stream()
                .map(etapa -> new EtapaDTO(etapa.getId(), etapa.getOrdem(), etapa.getCursoId(),
                        etapa.getTituloDoCurso(), etapa.getCargaHoraria(), etapa.getPrazo(), etapa.getStatus(),
                        etapa.getConcluidaEm()))
                .toList();
        List<RegistroDeAgenteDTO> registros = plano.getRegistros().stream()
                .map(registro -> new RegistroDeAgenteDTO(registro.getAgente(), registro.getFerramenta(),
                        registro.getDetalhe(), registro.getRegistradoEm()))
                .toList();
        return new PlanoResponseDTO(plano.getId(), plano.getColaboradorId(), plano.getNivel(), plano.getObjetivo(),
                plano.getStatus(), plano.getOrientacao(), plano.getModo(), sugeridos, etapas,
                acompanhamento.acompanhar(plano, LocalDate.now()), registros, plano.getCriadoEm(),
                plano.getDecididoEm());
    }
}
