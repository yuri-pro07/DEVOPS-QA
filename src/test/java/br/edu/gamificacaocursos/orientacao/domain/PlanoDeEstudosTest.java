package br.edu.gamificacaocursos.orientacao.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia.INICIANTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanoDeEstudosTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 23, 10, 0);
    private static final LocalDate HOJE = AGORA.toLocalDate();

    private static PlanoDeEstudos plano() {
        return new PlanoDeEstudos(1L, INICIANTE, "java", "orientacao", "agente", List.of(1L, 2L), AGORA);
    }

    private static EtapaDoPlano etapa(long id, int ordem, LocalDate prazo) {
        EtapaDoPlano etapa = new EtapaDoPlano(ordem, id, "Curso " + id, 20, prazo);
        ReflectionTestUtils.setField(etapa, "id", id);
        return etapa;
    }

    @Test
    @DisplayName("Todo plano nasce aguardando autorizacao, sem etapas e com os cursos sugeridos")
    void nasceAguardando() {
        PlanoDeEstudos plano = plano();

        assertEquals(StatusDoPlano.AGUARDANDO_AUTORIZACAO, plano.getStatus());
        assertEquals(List.of(1L, 2L), plano.getCursosSugeridos());
        assertEquals(List.of(), plano.getEtapas());
        assertNull(plano.getDecididoEm());
        assertEquals(1L, plano.getColaboradorId());
        assertEquals(INICIANTE, plano.getNivel());
        assertEquals("java", plano.getObjetivo());
        assertEquals("agente", plano.getModo());
        assertEquals(AGORA, plano.getCriadoEm());
    }

    @Test
    @DisplayName("Autorizar vincula as etapas e nao permite decidir de novo")
    void autoriza() {
        PlanoDeEstudos plano = plano();

        plano.autorizar(List.of(etapa(1, 1, HOJE.plusWeeks(5)), etapa(2, 2, HOJE.plusWeeks(8))), AGORA);

        assertEquals(StatusDoPlano.AUTORIZADO, plano.getStatus());
        assertEquals(2, plano.getEtapas().size());
        assertEquals(AGORA, plano.getDecididoEm());
        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> plano.recusar(AGORA));
        assertTrue(erro.getMessage().contains("ja foi decidido e esta AUTORIZADO"));
    }

    @Test
    @DisplayName("Recusar nao cria etapas e bloqueia a conclusao de etapas")
    void recusa() {
        PlanoDeEstudos plano = plano();

        plano.recusar(AGORA);

        assertEquals(StatusDoPlano.RECUSADO, plano.getStatus());
        assertThrows(IllegalStateException.class, () -> plano.autorizar(List.of(), AGORA));
        assertThrows(IllegalStateException.class, () -> plano.concluirEtapa(1L, HOJE));
    }

    @Test
    @DisplayName("Concluir todas as etapas conclui o plano; etapa repetida ou inexistente e recusada")
    void concluiEtapas() {
        PlanoDeEstudos plano = plano();
        plano.autorizar(List.of(etapa(1, 1, HOJE.minusDays(1)), etapa(2, 2, HOJE.plusWeeks(8))), AGORA);
        EtapaDoPlano primeira = plano.getEtapas().get(0);
        assertTrue(primeira.atrasadaEm(HOJE));

        plano.concluirEtapa(1L, HOJE);

        assertTrue(primeira.concluida());
        assertFalse(primeira.atrasadaEm(HOJE));
        assertEquals(HOJE, primeira.getConcluidaEm());
        assertEquals(StatusDaEtapa.CONCLUIDA, primeira.getStatus());
        assertEquals(StatusDoPlano.AUTORIZADO, plano.getStatus());
        assertThrows(IllegalStateException.class, () -> plano.concluirEtapa(1L, HOJE));
        assertThrows(IllegalArgumentException.class, () -> plano.concluirEtapa(99L, HOJE));

        plano.concluirEtapa(2L, HOJE);

        assertEquals(StatusDoPlano.CONCLUIDO, plano.getStatus());
    }

    @Test
    @DisplayName("Textos longos do agente sao cortados no tamanho das colunas")
    void cortaTextosLongos() {
        PlanoDeEstudos plano = new PlanoDeEstudos(1L, null, "", "x".repeat(5000), "m", List.of(1L), AGORA);
        RegistroDeAgente registro = new RegistroDeAgente("agente", "ferramenta", "y".repeat(600), AGORA);
        plano.registrar(registro);

        assertEquals(PlanoDeEstudos.TAMANHO_DA_ORIENTACAO, plano.getOrientacao().length());
        assertEquals(RegistroDeAgente.TAMANHO_DO_DETALHE, plano.getRegistros().get(0).getDetalhe().length());
        assertEquals("agente", registro.getAgente());
        assertEquals("ferramenta", registro.getFerramenta());
        assertEquals(AGORA, registro.getRegistradoEm());
    }
}
