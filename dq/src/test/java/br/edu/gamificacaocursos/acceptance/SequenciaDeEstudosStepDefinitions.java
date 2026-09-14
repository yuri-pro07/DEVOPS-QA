package br.edu.gamificacaocursos.acceptance;

import br.edu.gamificacaocursos.domain.SequenciaDeEstudos;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ATDD - glue dos cenarios do Case A.
 *
 * Os passos apenas conduzem o dominio; nenhuma regra de negocio mora aqui.
 * Enquanto SequenciaDeEstudos.registrarEstudo(...) nao estiver implementado,
 * os tres cenarios permanecem RED por causa da regra ausente - e nao por
 * passo indefinido.
 */
public class SequenciaDeEstudosStepDefinitions {

    private SequenciaDeEstudos sequencia;
    private LocalDate primeiroDia;

    @Dado("que o aluno {string} estudou em {string}")
    public void queOAlunoEstudouEm(String aluno, String data) {
        sequencia = new SequenciaDeEstudos(aluno);
        sequencia.registrarEstudo(LocalDate.parse(data));
    }

    @E("que ele também estudou em {string}")
    public void queEleTambemEstudouEm(String data) {
        sequencia.registrarEstudo(LocalDate.parse(data));
    }

    @Dado("que o aluno {string} começou a estudar em {string}")
    public void queOAlunoComecouAEstudarEm(String aluno, String data) {
        sequencia = new SequenciaDeEstudos(aluno);
        primeiroDia = LocalDate.parse(data);
    }

    @Quando("ele registra estudo em {string}")
    public void eleRegistraEstudoEm(String data) {
        sequencia.registrarEstudo(LocalDate.parse(data));
    }

    @Quando("ele deixa passar um dia e só volta a estudar em {string}")
    public void eleDeixaPassarUmDiaESoVoltaAEstudarEm(String data) {
        sequencia.registrarEstudo(LocalDate.parse(data));
    }

    @Quando("ele estuda por {int} dias consecutivos")
    public void eleEstudaPorDiasConsecutivos(int dias) {
        for (int i = 0; i < dias; i++) {
            sequencia.registrarEstudo(primeiroDia.plusDays(i));
        }
    }

    @Entao("sua sequência de dias consecutivos deve ser {int}")
    public void suaSequenciaDeDiasConsecutivosDeveSer(int esperado) {
        assertEquals(esperado, sequencia.getDiasConsecutivos());
    }

    @E("ele deve ter {int} moeda conquistada")
    public void eleDeveTerMoedaConquistada(int esperado) {
        assertEquals(esperado, sequencia.getMoedasConquistadas());
    }
}
