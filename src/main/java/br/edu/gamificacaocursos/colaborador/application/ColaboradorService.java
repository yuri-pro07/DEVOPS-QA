package br.edu.gamificacaocursos.colaborador.application;

import br.edu.gamificacaocursos.colaborador.api.ColaboradorRequestDTO;
import br.edu.gamificacaocursos.colaborador.api.ColaboradorResponseDTO;
import br.edu.gamificacaocursos.colaborador.domain.Colaborador;
import br.edu.gamificacaocursos.colaborador.internal.persistence.ColaboradorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ColaboradorService {
    private final ColaboradorRepository repository;

    public ColaboradorService(ColaboradorRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ColaboradorResponseDTO cadastrar(ColaboradorRequestDTO dto) {
        String email = dto.email().trim().toLowerCase();
        if (repository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ja existe um colaborador com o email " + email);
        }
        return toDTO(repository.save(new Colaborador(dto.nome().trim(), email, dto.cargo().trim(), dto.area().trim())));
    }

    @Transactional(readOnly = true)
    public List<ColaboradorResponseDTO> listar() {
        return repository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public ColaboradorResponseDTO buscarPorId(Long id) {
        return repository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Colaborador nao encontrado: " + id));
    }

    private ColaboradorResponseDTO toDTO(Colaborador colaborador) {
        return new ColaboradorResponseDTO(colaborador.getId(), colaborador.getNome(), colaborador.getEmail(),
                colaborador.getCargo(), colaborador.getArea());
    }
}
