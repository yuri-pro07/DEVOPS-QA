# Case A: Gamificação para Engajamento de Educação Continuada

Entrega da nossa equipe aplicando ATDD, BDD e TDD.

> ⚠️ **Os 3 testes deste projeto falham de propósito.** Este é o 1º passo do TDD, o "teste para falhar" (RED). Não é defeito.

## 👥 Equipe

- Yuri Peruzzo
- Enzo Zorzetto
- Pedro Ricci Gomes Nascimento

## 🎯 Visão do produto

Uma determinada plataforma vende cursos online e EAD no modelo de assinaturas. O aluno paga um valor mensal e tem acesso a um conjunto de cursos para assinatura básica. A cada curso terminado e com média acima de 7,0, o aluno tem direito à realização de mais 3 cursos. O aluno que escrever mais tópicos no fórum e ajudar outros participantes com seus comentários ganha um curso no final do mês. Quando o aluno conquistar 12 cursos, seu plano de assinatura passa a ser "Premium" e ele passa a receber voucher para participar de projetos reais durante os cursos, além de 3 moedas, que podem ser convertidas em conhecimento (novos cursos), acumuladas ou recebidas por criptomoeda.

## 1) Product Backlog

Cada integrante escreveu 1 User Story:

| #US | Integrante | As a | I want | So that | Escolhida pelo grupo |
|---|---|---|---|---|---|
| 1 | Yuri Peruzzo | COMO aluno assinante | QUERO manter uma sequência de dias consecutivos estudando (uma "ofensiva" de estudos) e ser recompensado automaticamente ao atingir marcos, como 7 dias seguidos | PARA criar o hábito de estudar com regularidade, não só quando termino um curso inteiro | ✅ **SIM** |
| 2 | Enzo Zorzetto | COMO administrador da plataforma | QUERO visualizar um ranking mensal dos alunos mais engajados no fórum | PARA identificar quem merece destaque e incentivar a comunidade a participar mais | Não |
| 3 | Pedro Ricci Gomes Nascimento | COMO aluno assinante | QUERO desbloquear selos/conquistas visuais (ex.: "Maratonista", "Mentor da Turma") ao atingir marcos de comportamento diferentes entre si | PARA exibir meu progresso de forma divertida e compartilhável, além das notas | Não |

### US1 (escolhida)

> **COMO** aluno assinante
> **QUERO** manter uma sequência de dias consecutivos estudando (uma "ofensiva" de estudos) e ser recompensado automaticamente ao atingir marcos, como 7 dias seguidos
> **PARA** criar o hábito de estudar com regularidade, não só quando termino um curso inteiro.

Modelamos essa mecânica na classe `SequenciaDeEstudos`, que premia a regularidade do aluno, dia após dia.

## 3) BDD: cenários de aceite da US1

Um cenário por integrante:

| Cenário | Integrante | Given | When | Then |
|---|---|---|---|---|
| 1 | Yuri Peruzzo | Dado que o aluno estudou ontem | Quando ele registra estudo hoje (dia seguinte) | Então sua sequência de dias consecutivos deve aumentar |
| 2 | Enzo Zorzetto | Dado que o aluno tinha uma sequência de 2 dias seguidos | Quando ele deixa passar um dia sem estudar e só volta a estudar depois | Então sua sequência deve reiniciar para 1 |
| 3 | Pedro Ricci | Dado que o aluno estudou 6 dias seguidos | Quando ele estuda no 7º dia consecutivo | Então ele deve receber 1 moeda de recompensa pela sequência |

## 6.1) TDD: primeiro passo, "teste para falhar" (RED)

Cada cenário de BDD virou um teste em `SequenciaDeEstudosTest`, no formato Arrange / Act / Assert:

| Cenário | Arrange (DADO) | Act (QUANDO) | Assert (ENTÃO) | Status |
|---|---|---|---|---|
| 1 | `new SequenciaDeEstudos("Rafael")`, 01/09 e 02/09 | `registrarEstudo(ontem)` e `registrarEstudo(hoje)` | `assertEquals(2, getDiasConsecutivos())` | 🔴 RED |
| 2 | `new SequenciaDeEstudos("Aline")`, 01/09, 02/09 e 04/09 (pulou o dia 3) | `registrarEstudo` nos três dias | `assertEquals(1, getDiasConsecutivos())` | 🔴 RED |
| 3 | `new SequenciaDeEstudos("Pedro")`, início em 01/09 | `registrarEstudo` em 7 dias seguidos | `assertEquals(7, getDiasConsecutivos())` e `assertEquals(1, getMoedasConquistadas())` | 🔴 RED |

Os três falham com `UnsupportedOperationException`, porque `SequenciaDeEstudos.registrarEstudo(...)` ainda não foi implementado:

```
Tests run: 3, Failures: 0, Errors: 3, Skipped: 0
java.lang.UnsupportedOperationException: registrarEstudo ainda nao implementado (TDD RED).
```

## 📦 Escopo desta entrega

Vai até o 1º passo do TDD (RED). Os passos seguintes ficam para a próxima etapa:

- [ ] **GREEN**: implementar `registrarEstudo` para fazer os testes passarem
- [ ] **BLUE / REFACTOR**: refatorar mantendo os testes verdes
- [ ] Cobertura **JaCoCo** 100%

## ▶️ Como rodar

```bash
mvn test
```

Ou abrindo a pasta como projeto Maven no IntelliJ IDEA. Em ambos os casos, os 3 testes devem falhar.

## 🛠️ Tecnologias

- Java 17
- Maven
- JUnit Jupiter 5.10.2
- Maven Surefire Plugin 3.2.5

## 📁 Estrutura

```
case-a-gamificacao-cursos/
├── pom.xml
└── src/
    ├── main/java/br/edu/exemplo/gamificacaocursos/domain/
    │   └── SequenciaDeEstudos.java
    └── test/java/br/edu/exemplo/gamificacaocursos/domain/
        └── SequenciaDeEstudosTest.java
```

---
*Projeto acadêmico desenvolvido para a disciplina de Engenharia de Software / Testes de Software.*
