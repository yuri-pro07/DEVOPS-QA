package br.edu.gamificacaocursos.ofensiva.application;

import br.edu.gamificacaocursos.ofensiva.domain.SequenciaDeEstudos;
import br.edu.gamificacaocursos.ofensiva.api.SequenciaDeEstudosResponseDTO;
import br.edu.gamificacaocursos.ofensiva.internal.persistence.SequenciaDeEstudosRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class SequenciaDeEstudosService {
    private final SequenciaDeEstudosRepository repository;

    public SequenciaDeEstudosService(SequenciaDeEstudosRepository repository) {
        this.repository = repository;
    }

    public List<SequenciaDeEstudosResponseDTO> listarTodas() {
        return repository.findAll().stream().map(this::toDTO).toList();
    }

    public SequenciaDeEstudosResponseDTO buscarPorId(Long id) {
        return toDTO(buscarEntidade(id));
    }

    @Transactional
    public SequenciaDeEstudosResponseDTO criar(String aluno) {
        if (repository.existsByAluno(aluno)) {
            throw new IllegalArgumentException("Ja existe uma sequencia para o aluno " + aluno);
        }
        return toDTO(repository.save(new SequenciaDeEstudos(aluno)));
    }

    @Transactional
    public SequenciaDeEstudosResponseDTO registrarEstudo(Long id, LocalDate data) {
        SequenciaDeEstudos sequencia = buscarEntidade(id);

        sequencia.registrarEstudo(data);

        return toDTO(repository.save(sequencia));
    }

    @Transactional
    public void remover(Long id) {
        repository.delete(buscarEntidade(id));
    }

    private SequenciaDeEstudos buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sequencia nao encontrada: " + id));
    }

    private SequenciaDeEstudosResponseDTO toDTO(SequenciaDeEstudos sequencia) {
        return new SequenciaDeEstudosResponseDTO(
                sequencia.getId(),
                sequencia.getAluno(),
                sequencia.getDiasConsecutivos(),
                sequencia.getUltimoDiaEstudado(),
                sequencia.getMoedasConquistadas());
    }
}
