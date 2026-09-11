# Case A — Gamificação para Engajamento de Educação Continuada

Entrega da equipe aplicando **ATDD → BDD → TDD**.

> 📍 **Estado atual: GREEN.** O ciclo já passou pelo 1º passo (RED, testes falhando de propósito)
> e pelo 2º (GREEN, implementação mínima). O 3º passo — BLUE / refatoração — ainda não foi feito.

## 👥 Equipe

- Yuri Peruzzo
- Enzo Zorzetto
- Pedro Ricci Gomes Nascimento

## 📖 Descrição do estudo de caso

**Case A — Gamificação para Engajamento de Educação Continuada.**

Uma determinada plataforma vende cursos online e EAD no modelo de assinaturas. O aluno paga um
valor mensal e tem acesso a um conjunto de cursos para assinatura básica. A cada curso terminado e
com média acima de 7,0, o aluno tem direito à realização de mais 3 cursos. O aluno que escrever
mais tópicos no fórum e ajudar outros participantes com seus comentários ganha um curso no final do
mês. Quando o aluno conquistar 12 cursos, seu plano de assinatura passa a ser "Premium" e ele passa
a receber voucher para participar de projetos reais durante os cursos, além de 3 moedas, que podem
ser convertidas em conhecimento (novos cursos), acumuladas ou recebidas por criptomoeda.

## 1) Product Backlog — 1 User Story por integrante

| #US | Integrante | As a | I want | So that | Escolhida pelo grupo |
|---|---|---|---|---|---|
| 1 | Yuri Peruzzo | COMO aluno assinante | QUERO manter uma sequência de dias consecutivos estudando (uma "ofensiva" de estudos) e ser recompensado automaticamente ao atingir marcos, como 7 dias seguidos | PARA criar o hábito de estudar com regularidade, não só quando termino um curso inteiro | ✅ **SIM** |
| 2 | Enzo Zorzetto | COMO administrador da plataforma | QUERO visualizar um ranking mensal dos alunos mais engajados no fórum | PARA identificar quem merece destaque e incentivar a comunidade a participar mais | Não |
| 3 | Pedro Ricci Gomes Nascimento | COMO aluno assinante | QUERO desbloquear selos/conquistas visuais (ex.: "Maratonista", "Mentor da Turma") ao atingir marcos de comportamento diferentes entre si | PARA exibir meu progresso de forma divertida e compartilhável, além das notas | Não |

### 2) US escolhida pelo grupo: US1 (Yuri Peruzzo)

> **COMO** aluno assinante
> **QUERO** manter uma sequência de dias consecutivos estudando (uma "ofensiva" de estudos) e ser
> recompensado automaticamente ao atingir marcos, como 7 dias seguidos
> **PARA** criar o hábito de estudar com regularidade, não só quando termino um curso inteiro.

A mecânica foi modelada na classe de domínio `SequenciaDeEstudos`, que premia a regularidade do
aluno, dia após dia.

## 3) BDD — Cenários de aceite da US1 (1 cenário por integrante)

| Cenário | Redigido por | Given | When | Then |
|---|---|---|---|---|
| 1 | **Yuri Peruzzo** | Dado que o aluno estudou ontem | Quando ele registra estudo hoje (dia seguinte) | Então sua sequência de dias consecutivos deve aumentar |
| 2 | **Enzo Zorzetto** | Dado que o aluno tinha uma sequência de 2 dias seguidos | Quando ele deixa passar um dia sem estudar e só volta a estudar depois | Então sua sequência deve reiniciar para 1 |
| 3 | **Pedro Ricci Gomes Nascimento** | Dado que o aluno estudou 6 dias seguidos | Quando ele estuda no 7º dia consecutivo | Então ele deve receber 1 moeda de recompensa pela sequência |

## 4) ATDD — Cenários automatizados em Cucumber

Os três cenários de BDD viraram Gherkin executável em
[`src/test/resources/features/sequencia_de_estudos.feature`](src/test/resources/features/sequencia_de_estudos.feature),
com os passos implementados em
[`SequenciaDeEstudosStepDefinitions`](src/test/java/br/edu/gamificacaocursos/acceptance/SequenciaDeEstudosStepDefinitions.java)
e a suíte executada por
[`RunCucumberTest`](src/test/java/br/edu/gamificacaocursos/acceptance/RunCucumberTest.java).

```gherkin
# language: pt
Funcionalidade: Ofensiva de estudos

  Cenário: Aumentar a sequência ao estudar em dias consecutivos
    Dado que o aluno "Rafael" estudou em "2026-09-01"
    Quando ele registra estudo em "2026-09-02"
    Então sua sequência de dias consecutivos deve ser 2

  Cenário: Reiniciar a sequência quando o aluno deixa passar um dia
    Dado que o aluno "Aline" estudou em "2026-09-01"
    E que ele também estudou em "2026-09-02"
    Quando ele deixa passar um dia e só volta a estudar em "2026-09-04"
    Então sua sequência de dias consecutivos deve ser 1

  Cenário: Receber moeda de recompensa ao completar 7 dias consecutivos
    Dado que o aluno "Pedro" começou a estudar em "2026-09-01"
    Quando ele estuda por 7 dias consecutivos
    Então sua sequência de dias consecutivos deve ser 7
    E ele deve ter 1 moeda conquistada
```

**Todos os passos são encontrados — nenhum passo indefinido.** No log do Cucumber, cada linha do
Gherkin aparece ligada ao seu método de glue. No RED, os cenários quebravam na **regra de negócio
ainda não implementada** — um critério de aceite falhando, e não o Cucumber sem saber executar o
passo. Com o GREEN, os três cenários passam.

## 5) Domínio e teste de domínio

| Camada | Pacote | Classe |
|---|---|---|
| Domínio (`src/main/java`) | `br.edu.gamificacaocursos.domain` | [`SequenciaDeEstudos`](src/main/java/br/edu/gamificacaocursos/domain/SequenciaDeEstudos.java) |
| Teste de domínio (`src/test/java`) | `br.edu.gamificacaocursos.domain` | [`SequenciaDeEstudosTest`](src/test/java/br/edu/gamificacaocursos/domain/SequenciaDeEstudosTest.java) |
| ATDD / glue (`src/test/java`) | `br.edu.gamificacaocursos.acceptance` | [`SequenciaDeEstudosStepDefinitions`](src/test/java/br/edu/gamificacaocursos/acceptance/SequenciaDeEstudosStepDefinitions.java) |

## 6.1) TDD — 1º passo: "teste para falhar" (RED)

Cada cenário de BDD virou também um teste de domínio em `SequenciaDeEstudosTest`, no formato
Arrange / Act / Assert:

| Cenário | Arrange (DADO) | Act (QUANDO) | Assert (ENTÃO) | Status |
|---|---|---|---|---|
| 1 | `new SequenciaDeEstudos("Rafael")`, 01/09 e 02/09 | `registrarEstudo(ontem)` e `registrarEstudo(hoje)` | `assertEquals(2, getDiasConsecutivos())` | 🔴 RED |
| 2 | `new SequenciaDeEstudos("Aline")`, 01/09, 02/09 e 04/09 (pulou o dia 3) | `registrarEstudo` nos três dias | `assertEquals(1, getDiasConsecutivos())` | 🔴 RED |
| 3 | `new SequenciaDeEstudos("Pedro")`, início em 01/09 | `registrarEstudo` em 7 dias seguidos | `assertEquals(7, getDiasConsecutivos())` e `assertEquals(1, getMoedasConquistadas())` | 🔴 RED |

Nesta etapa os três falhavam com `UnsupportedOperationException`, porque
`SequenciaDeEstudos.registrarEstudo(...)` ainda não tinha sido implementado:

```java
public void registrarEstudo(LocalDate data) {
    throw new UnsupportedOperationException("registrarEstudo ainda nao implementado (TDD RED).");
}
```

## ▶️ Como rodar

```bash
mvn test
```

## 🔴 1º passo — Evidência dos testes falhando (RED)

Saída real de `mvn test` **antes** da implementação (bloco `Results:` do Surefire):

```text
[INFO] Results:
[INFO]
[ERROR] Errors:
[ERROR]   RunCucumberTest.Aumentar a sequência ao estudar em dias consecutivos » UnsupportedOperation registrarEstudo ainda nao implementado (TDD RED).
[ERROR]   RunCucumberTest.Receber moeda de recompensa ao completar 7 dias consecutivos » UnsupportedOperation registrarEstudo ainda nao implementado (TDD RED).
[ERROR]   RunCucumberTest.Reiniciar a sequência quando o aluno deixa passar um dia » UnsupportedOperation registrarEstudo ainda nao implementado (TDD RED).
[ERROR]   SequenciaDeEstudosTest.aumentaSequenciaAoEstudarEmDiasConsecutivos:19 » UnsupportedOperation registrarEstudo ainda nao implementado (TDD RED).
[ERROR]   SequenciaDeEstudosTest.recebeMoedaAoCompletarSeteDiasConsecutivos:47 » UnsupportedOperation registrarEstudo ainda nao implementado (TDD RED).
[ERROR]   SequenciaDeEstudosTest.reiniciaSequenciaQuandoPulaUmDiaSemEstudar:33 » UnsupportedOperation registrarEstudo ainda nao implementado (TDD RED).
[INFO]
[ERROR] Tests run: 6, Failures: 0, Errors: 6, Skipped: 0
[INFO]
[INFO] BUILD FAILURE
```

As 6 execuções são os 3 testes de domínio (`SequenciaDeEstudosTest`) e os 3 cenários de aceitação
(`sequencia_de_estudos.feature`), todos parando no mesmo ponto:
`SequenciaDeEstudos.registrarEstudo(...)`.

| Suíte | Resultado | Por quê |
|---|---|---|
| `SequenciaDeEstudosTest` (3) | 🔴 3 erros | **TDD RED** — `registrarEstudo` ainda não implementado |
| `RunCucumberTest` → `sequencia_de_estudos.feature` (3) | 🔴 3 erros | **ATDD RED** — mesma causa, alcançada através do glue |

## 6.2) TDD — 2º passo: implementação mínima (GREEN)

Com os testes em RED, `registrarEstudo(...)` foi implementado com o mínimo necessário para
satisfazer os 3 cenários — nenhum teste foi alterado:

```java
public void registrarEstudo(LocalDate data) {
    // TDD - GREEN: implementacao minima para os 3 cenarios de aceite da US1.
    if (ultimoDiaEstudado != null && data.equals(ultimoDiaEstudado.plusDays(1))) {
        diasConsecutivos = diasConsecutivos + 1;
    } else {
        diasConsecutivos = 1;
    }

    ultimoDiaEstudado = data;

    if (diasConsecutivos == DIAS_PARA_RECOMPENSA) {
        moedasConquistadas = moedasConquistadas + MOEDAS_POR_MARCO;
    }
}
```

Como cada cenário é atendido:

| Cenário | Regra aplicada | Resultado |
|---|---|---|
| 1 — dias consecutivos | `data` é o dia seguinte ao último estudado → incrementa | `diasConsecutivos = 2` ✅ |
| 2 — pulou um dia | 04/09 não é o dia seguinte a 02/09 → reinicia | `diasConsecutivos = 1` ✅ |
| 3 — marco de 7 dias | ao chegar em `DIAS_PARA_RECOMPENSA`, credita a moeda | `7 dias` e `1 moeda` ✅ |

## ✅ 2º passo — Evidência dos testes passando (GREEN)

```text
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in br.edu.gamificacaocursos.domain.SequenciaDeEstudosTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in br.edu.gamificacaocursos.acceptance.RunCucumberTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] BUILD SUCCESS
```

O TDD e o ATDD ficaram verdes juntos: os 3 testes de domínio e os 3 cenários de aceitação passam
com a mesma implementação, sem que nenhum teste ou passo de Gherkin fosse tocado.

### Cobertura de testes (JaCoCo)

```bash
mvn clean test
```

O relatório sai em `target/site/jacoco/index.html`.

| Classe | Instruções | Branches | Linhas não cobertas |
|---|---|---|---|
| `SequenciaDeEstudos` | 🟡 90,2% | 🟢 100% | 2 de 17 |
| `GamificacaoCursosApplication` | 🔴 0% | — | 3 de 3 |
| **Total** | **79,7%** | **100%** | |

**Onde está o amarelo e o vermelho — e por quê:**

- 🟡 `SequenciaDeEstudos` — `getAluno()` e `getUltimoDiaEstudado()` nunca são chamados pelos
  testes. Toda a regra de negócio de `registrarEstudo(...)` está coberta, inclusive os 100% de
  branches (dia seguinte / dia pulado / marco atingido).
- 🔴 `GamificacaoCursosApplication` — classe de bootstrap do Spring Boot, sem teste que a exercite.

Fechar esses dois pontos e chegar a 100% sem amarelo nem vermelho é tarefa do **3º passo (BLUE)**,
que ainda não foi feito.

## 📁 Estrutura

```text
case-a-gamificacao-cursos/
├── pom.xml
├── README.md
└── src/
    ├── main/java/br/edu/gamificacaocursos/
    │   ├── GamificacaoCursosApplication.java
    │   └── domain/
    │       └── SequenciaDeEstudos.java                      <- domínio
    └── test/
        ├── java/br/edu/gamificacaocursos/
        │   ├── acceptance/
        │   │   ├── RunCucumberTest.java                     <- runner do Cucumber
        │   │   └── SequenciaDeEstudosStepDefinitions.java   <- ATDD
        │   └── domain/
        │       └── SequenciaDeEstudosTest.java              <- TDD
        └── resources/features/
            └── sequencia_de_estudos.feature                 <- BDD
```

## 🛠️ Tecnologias

- Java 17
- Maven
- Spring Boot (starter + starter-test)
- JUnit Jupiter
- Cucumber 7 (cucumber-java + cucumber-junit-platform-engine)
- JaCoCo 0.8.12 (cobertura de testes)
- IntelliJ IDEA Ultimate
