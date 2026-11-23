package br.edu.gamificacaocursos.curso.internal.persistence;

import br.edu.gamificacaocursos.curso.domain.Curso;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

import static br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia.AVANCADO;
import static br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia.INICIANTE;
import static br.edu.gamificacaocursos.shared.domain.NivelDeCompetencia.INTERMEDIARIO;

@Component
public class CatalogoInicial implements ApplicationRunner {
    private final CursoRepository repository;

    public CatalogoInicial(CursoRepository repository) {
        this.repository = repository;
    }

    static List<Curso> cursos() {
        return List.of(
                new Curso("Fundamentos de Java", "Sintaxe, tipos, colecoes e orientacao a objetos para quem esta comecando.", "Desenvolvimento", INICIANTE, 20),
                new Curso("HTTP e APIs REST", "Verbos, codigos de status, JSON e consumo de APIs REST.", "Desenvolvimento", INICIANTE, 12),
                new Curso("SQL Essencial", "Consultas, filtros, juncoes e modelagem relacional basica.", "Dados", INICIANTE, 16),
                new Curso("Spring Boot na Pratica", "APIs REST com Spring Boot, JPA, validacao e testes automatizados.", "Desenvolvimento", INTERMEDIARIO, 30),
                new Curso("Testes Automatizados com JUnit e Mockito", "TDD, testes de unidade, dubles de teste e cobertura com JaCoCo.", "Qualidade", INTERMEDIARIO, 24),
                new Curso("Mensageria com RabbitMQ", "Filas, trocas, confirmacoes e processamento assincrono confiavel.", "Arquitetura", INTERMEDIARIO, 18),
                new Curso("Arquitetura de Software e Monolito Modular", "Modulos, acoplamento, ADRs, C4 e evolucao para servicos.", "Arquitetura", AVANCADO, 32),
                new Curso("Machine Learning para Desenvolvedores", "Classificacao, avaliacao de modelos e servico de inferencia com FastAPI.", "Dados", AVANCADO, 36),
                new Curso("RAG e Agentes de IA com Spring AI", "Recuperacao de documentos, geracao aumentada e agentes com ferramentas.", "Inteligencia Artificial", AVANCADO, 28));
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() == 0) {
            repository.saveAll(cursos());
        }
    }
}
