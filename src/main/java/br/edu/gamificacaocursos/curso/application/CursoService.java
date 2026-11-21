package br.edu.gamificacaocursos.curso.application;

import br.edu.gamificacaocursos.curso.api.CursoRequestDTO;
import br.edu.gamificacaocursos.curso.api.CursoResponseDTO;
import br.edu.gamificacaocursos.curso.domain.Curso;
import br.edu.gamificacaocursos.curso.internal.persistence.CursoRepository;
import br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CursoService {
    private final CursoRepository repository;

    public CursoService(CursoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CursoResponseDTO cadastrar(CursoRequestDTO dto) {
        String titulo = dto.titulo().trim();
        if (repository.existsByTituloIgnoreCase(titulo)) {
            throw new IllegalArgumentException("Ja existe um curso com o titulo " + titulo);
        }
        return toDTO(repository.save(new Curso(titulo, dto.descricao().trim(), dto.area().trim(),
                dto.nivel(), dto.cargaHoraria())));
    }

    @Transactional(readOnly = true)
    public List<CursoResponseDTO> listar(NivelDeCompetencia nivel, String area) {
        String areaFiltrada = area == null || area.isBlank() ? null : area.trim();
        return repository.filtrar(nivel, areaFiltrada).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public CursoResponseDTO buscarPorId(Long id) {
        return repository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Curso nao encontrado: " + id));
    }

    private CursoResponseDTO toDTO(Curso curso) {
        return new CursoResponseDTO(curso.getId(), curso.getTitulo(), curso.getDescricao(), curso.getArea(),
                curso.getNivel(), curso.getCargaHoraria());
    }
}
