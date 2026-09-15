package br.edu.gamificacaocursos.service;

import br.edu.gamificacaocursos.domain.SequenciaDeEstudos;
import br.edu.gamificacaocursos.dto.SequenciaDeEstudosResponseDTO;
import br.edu.gamificacaocursos.repository.SequenciaDeEstudosRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

// Camada: SERVICE.
// Orquestra o caso de uso e delega a regra de negocio ao dominio.
// Repare que a regra da ofensiva NAO e reimplementada aqui: quem sabe
// contar dias consecutivos e premiar o marco e a SequenciaDeEstudos.
@Service
public class SequenciaDeEstudosService {

    private final SequenciaDeEstudosRepository repository;

    // Injecao de dependencia por construtor: o Spring entrega o repository.
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

        // A regra de negocio vive no dominio.
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
