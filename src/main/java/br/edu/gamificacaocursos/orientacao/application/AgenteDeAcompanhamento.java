package br.edu.gamificacaocursos.orientacao.application;

import br.edu.gamificacaocursos.competencia.application.CompetenciaService;
import br.edu.gamificacaocursos.curso.api.CursoResponseDTO;
import br.edu.gamificacaocursos.curso.application.CursoService;
import br.edu.gamificacaocursos.orientacao.domain.Acompanhamento;
import br.edu.gamificacaocursos.orientacao.domain.EtapaDoPlano;
import br.edu.gamificacaocursos.orientacao.domain.PlanoDeEstudos;
import br.edu.gamificacaocursos.orientacao.domain.RegistroDeAgente;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static br.edu.gamificacaocursos.orientacao.application.PoliticaDeAcessoDosAgentes.AGENTE_DE_ACOMPANHAMENTO;

@Component
public class AgenteDeAcompanhamento {
    static final int HORAS_SEMANAIS_PADRAO = 4;

    private final CompetenciaService competencias;
    private final CursoService cursos;
    private final PoliticaDeAcessoDosAgentes politica;

    public AgenteDeAcompanhamento(CompetenciaService competencias, CursoService cursos,
                                  PoliticaDeAcessoDosAgentes politica) {
        this.competencias = competencias;
        this.cursos = cursos;
        this.politica = politica;
    }

    public List<EtapaDoPlano> criarEtapas(PlanoDeEstudos plano, LocalDate inicio) {
        int horasSemanais = consultarDisponibilidade(plano);
        List<EtapaDoPlano> etapas = new ArrayList<>();
        LocalDate prazo = inicio;
        int ordem = 1;
        for (Long cursoId : plano.getCursosSugeridos()) {
            usar(plano, "consultarCurso", "curso " + cursoId);
            CursoResponseDTO curso = cursos.buscarPorId(cursoId);
            prazo = prazo.plusWeeks(semanas(curso.cargaHoraria(), horasSemanais));
            etapas.add(new EtapaDoPlano(ordem++, curso.id(), curso.titulo(), curso.cargaHoraria(), prazo));
        }
        usar(plano, "criarEtapas", etapas.size() + " etapas com " + horasSemanais + " h semanais");
        return etapas;
    }

    public Acompanhamento acompanhar(PlanoDeEstudos plano, LocalDate hoje) {
        return switch (plano.getStatus()) {
            case AGUARDANDO_AUTORIZACAO -> new Acompanhamento(0,
                    "Aguardando a autorizacao do colaborador. Nenhuma acao foi executada.", List.of());
            case RECUSADO -> new Acompanhamento(0,
                    "Plano recusado pelo colaborador. Nenhuma etapa foi criada.", List.of());
            case CONCLUIDO -> new Acompanhamento(100,
                    "Plano concluido. O colaborador ja pode pedir uma nova avaliacao de competencia.", List.of());
            case AUTORIZADO -> emAndamento(plano.getEtapas(), hoje);
        };
    }

    static long semanas(int cargaHoraria, int horasSemanais) {
        return Math.max(1, (cargaHoraria + horasSemanais - 1) / horasSemanais);
    }

    private Acompanhamento emAndamento(List<EtapaDoPlano> etapas, LocalDate hoje) {
        long concluidas = etapas.stream().filter(EtapaDoPlano::concluida).count();
        List<String> alertas = etapas.stream()
                .filter(etapa -> etapa.atrasadaEm(hoje))
                .map(etapa -> "Etapa " + etapa.getOrdem() + " (" + etapa.getTituloDoCurso() + ") atrasada desde "
                        + etapa.getPrazo() + ". Replaneje com o gestor.")
                .toList();
        EtapaDoPlano proxima = etapas.stream().filter(etapa -> !etapa.concluida()).findFirst().orElseThrow();
        return new Acompanhamento((int) (concluidas * 100 / etapas.size()),
                "Proxima etapa: " + proxima.getTituloDoCurso() + " ate " + proxima.getPrazo() + ".", alertas);
    }

    private int consultarDisponibilidade(PlanoDeEstudos plano) {
        usar(plano, "consultarDisponibilidade", "horas semanais da ultima avaliacao de competencia");
        return competencias.ultimaAvaliacao(plano.getColaboradorId())
                .map(avaliacao -> avaliacao.perfil().horasSemanais())
                .filter(horas -> horas > 0)
                .orElse(HORAS_SEMANAIS_PADRAO);
    }

    private void usar(PlanoDeEstudos plano, String ferramenta, String detalhe) {
        politica.verificar(AGENTE_DE_ACOMPANHAMENTO, ferramenta);
        plano.registrar(new RegistroDeAgente(AGENTE_DE_ACOMPANHAMENTO, ferramenta, detalhe, LocalDateTime.now()));
    }
}
