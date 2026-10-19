# Prática B1 — Desafio 1 · itens 1 a 4

**Equipe (3 integrantes):** Yuri Peruzzo · Enzo Zorzetto · Pedro Ricci Gomes Nascimento

Roteiro da prática:

| # | Etapa | Nesta entrega |
|---|---|---|
| 1 | Leitura e entendimento sobre a solução proposta | ✅ |
| 2 | Análise do legado (AC1) | ✅ |
| 3 | Código Modelo — Camadas | ✅ |
| 4 | Código Modelo — Modular | ✅ |
| 5 | Alterar ou aceitar o item 1 supramencionado | fora do escopo |
| 6 | Implementação | fora do escopo |

A decisão da equipe está registrada em [`README.md`](README.md) (ADR-001): **Monólito Modular**.

---

## 1) Leitura e entendimento sobre a solução proposta

### O que a solução propõe

O documento orientativo parte de um cenário em que o MVP sai de ~1.000 para até 100.000 usuários.
Ranking, cursos, conquistas e recompensas continuam no Core, mas o Ranking tende a crescer mais que
as demais capacidades. A pergunta que organiza o raciocínio é:

> Há evidência suficiente para introduzir distribuição e deploy independente agora, ou uma
> modularização interna resolve o problema com menor custo operacional?

A resposta proposta é **Monólito Modular**: organizar o Core por capacidade de negócio, manter um
único Spring Boot, e tratar o Ranking como candidato explícito à extração futura, sem transformá-lo
em microsserviço apenas pela previsão de crescimento.

### Como a solução chega lá

| Etapa | Conteúdo da solução proposta |
|---|---|
| RF | RF01 Ranking · RF02 Conquistas/gamificação |
| RNF analisados | Manutenibilidade, escalabilidade, complexidade operacional, desempenho, testabilidade |
| ASR selecionados | Manutenibilidade, escalabilidade, complexidade operacional |
| RNF descartados | Desempenho (sem medição que prove gargalo) e testabilidade (não determina sozinha a decomposição) |
| RPC | Java/Spring Boot · equipe · banco relacional · legado da AC1 |
| Alternativas | A) Camadas · B) Modular · C) Serviços independentes |
| ADR | ADR-001: adotar Monólito Modular |
| C4 | Container permanece único; a organização interna passa a ser modular |
| Evidência | Mini-lab de modularização, comparação antes/depois e testes |

### O que entendemos como essencial

Três pontos mudam a forma de conduzir o desafio:

**A evidência não é um print da aplicação funcionando.** O documento é explícito: o objetivo é
testar a hipótese de que a modularização torna fronteiras e dependências mais explícitas. Mostrar o
sistema no ar prova que ele compila, não que a decisão arquitetural procede.

**Nem todo RNF vira ASR, e o descarte também precisa de justificativa.** Desempenho é relevante e
ainda assim não entra, porque o cenário informa crescimento sem apresentar medição de latência ou
throughput. Essa distinção entre *projeção* e *medição* é o que sustenta a recusa aos microsserviços.

**A decisão precisa ser lida de trás para frente.** O C4 é explicado pelo ADR, o ADR é sustentado
pelos ASRs e RPCs, os ASRs derivam dos RF/RNF, e a evidência demonstra que o raciocínio é plausível.

### Onde nossa leitura diverge do documento

| Ponto | Documento orientativo | Nosso caso |
|---|---|---|
| Tamanho da equipe | 6 desenvolvedores | **3 integrantes** |
| Capacidade implementada | Ranking (RF01) | Ofensiva de estudos (RF02), que é o que a AC1 entregou |

A equipe menor torna a restrição **mais rígida**, não menos: com 3 pessoas, absorver deploys, rede,
observabilidade e falhas distribuídas é ainda menos viável. O argumento contra a alternativa (C)
fica reforçado.

---

## 2) Análise do legado (AC1)

### O que existe

A AC1 entregou um MVP funcional da plataforma, construído por TDD:

| Item | Situação |
|---|---|
| Domínio | `SequenciaDeEstudos`: ofensiva de dias consecutivos, com moeda ao atingir 7 dias (RF02) |
| Camadas | Controller, Service, Repository, DTO, tratamento de erro e configuração |
| Plataforma | Java 17 · Spring Boot 3.5.5 · JPA |
| Bancos | H2 em memória (perfil `h2`) e PostgreSQL 16 (perfil `postgres`) |
| API | 5 endpoints REST documentados em Swagger |
| Front-end | Vue 3 servido pela própria aplicação |
| Operação | Dockerfile em dois estágios e Docker Compose com app, postgres e pgadmin |
| Testes | 25, entre domínio (TDD), aceitação (Cucumber/ATDD), service e controller |
| Cobertura | 100% (JaCoCo), sem amarelo nem vermelho |

### Estrutura herdada

Organização **horizontal, por camada técnica**:

```text
br.edu.gamificacaocursos/
├── config/        OpenApiConfig
├── controller/    SequenciaDeEstudosRestController, ApiExceptionHandler
├── domain/        SequenciaDeEstudos
├── dto/           3 DTOs
├── repository/    SequenciaDeEstudosRepository
└── service/       SequenciaDeEstudosService
```

### Medição do legado

Relatório em [`evidencias/dependencias-antes.txt`](evidencias/dependencias-antes.txt):

```text
Pacotes de primeiro nivel: 7
controller -> dto           3
controller -> service       1
repository -> domain        1
service    -> domain        1
service    -> dto           1
service    -> repository    1
Total: 8        Cruzando o primeiro nivel: 8
```

**Todas as 8 dependências cruzam o primeiro nível.** Não é defeito de implementação: é a consequência
direta de organizar por camada. Uma capacidade de negócio não existe como unidade, pois está
espalhada por seis pastas.

### O que o legado sustenta e o que ele limita

**Sustenta.** A base é sólida para evoluir: regra de negócio isolada em uma classe de domínio sem
dependência de framework, suíte de testes que protege contra regressão, operação já containerizada.
Nada disso precisa ser refeito.

**Limita.** Com uma só capacidade, a organização horizontal funciona. O problema aparece na projeção
do cenário: ao acrescentar ranking, cursos e recompensas, as quatro capacidades passam a dividir os
mesmos seis pacotes. Localizar tudo que pertence ao Ranking exigiria navegar por `controller`,
`service`, `repository` e `dto`, e qualquer mudança em uma capacidade passaria a conviver com o
código das outras nos mesmos arquivos de pasta.

É exatamente o sintoma que o Desafio 1 pede para antecipar.

---

## 3) Código Modelo — Camadas

Projeto de referência: `B1_Desafio1_Monolito_Camadas`.

### O que o modelo faz

Acrescenta o RF de Ranking **sem trocar a arquitetura macro**, mantendo a separação horizontal
`controller → service → repository → domain`. Serve de baseline de comparação.

```text
com.exemplo.usuario/
├── config/        OpenApiConfig, SecurityConfig
├── controller/    Curso, Matricula, Ranking, Usuario, Home, ApiExceptionHandler
├── domain/        Assinatura, Curso, Matricula, Usuario, enums
│   └── vo/        Value Objects
├── dto/           8 DTOs de 4 capacidades
├── repository/    Assinatura, Curso, Matricula, Ranking, Usuario
└── service/       Curso, Matricula, Ranking, Usuario
```

### Medição

Relatório em [`evidencias/modelo-camadas.txt`](evidencias/modelo-camadas.txt):

```text
Pacotes de primeiro nivel: 7
controller -> dto            8
controller -> service        4
domain     -> domain.vo      5
repository -> domain         5
service    -> domain         8
service    -> dto            6
service    -> repository     7
Total: 43       Cruzando o primeiro nivel: 38
```

### Leitura

**38 das 43 dependências cruzam o primeiro nível, ou 88%.** O modelo tem quatro capacidades de negócio
e nenhuma delas existe como unidade no código: as quatro compartilham os mesmos `controller`,
`service`, `repository` e `dto`.

O próprio README do modelo aponta a característica a observar: *"para localizar tudo que pertence ao
Ranking, é necessário navegar por várias camadas"*.

Comparando com a nossa AC1, a forma é idêntica: mesma organização, mesma proporção de dependências
cruzando o topo (88% lá, 100% aqui). A diferença é só de tamanho: o modelo mostra onde a AC1 chega
quando ganhar as outras três capacidades.

### O que isso prova sobre a alternativa (A)

A alternativa "manter Monólito em Camadas" não falha por ser simples. Ela falha porque o
acoplamento estrutural cresce com o número de capacidades. Com uma capacidade, 8 dependências
cruzando. Com quatro, 38. A escala do problema é proporcional ao crescimento previsto no cenário.

---

## 4) Código Modelo — Modular

Projeto de referência: `B1_Desafio1_Monolito_Modular`.

### O que o modelo faz

Reorganiza a mesma aplicação por **capacidade de negócio**, sem criar microsserviços: continua um
processo Spring Boot, um banco e um deploy.

```text
com.exemplo.usuario/
├── usuario/
│   ├── package-info.java
│   ├── api/                    Controller + DTOs
│   ├── application/            Service
│   ├── domain/                 Entidades e Value Objects
│   └── internal/persistence/   Repositories
├── curso/          mesma forma
├── matricula/      mesma forma
├── ranking/        api, application, internal/persistence
└── shared/
    ├── config/                 OpenApi, Security
    └── web/                    ApiExceptionHandler, HomeController
```

### Convenções que adotamos

| Convenção | Para que serve |
|---|---|
| Pasta de primeiro nível = capacidade de negócio | A unidade de organização passa a ser o domínio, não a camada |
| Subcamadas `api`, `application`, `domain`, `internal/persistence` | Preserva a separação técnica, mas dentro da fronteira do módulo |
| DTOs dentro de `api/` | O contrato fica junto de quem o publica |
| Segmento `internal/` | Marca explicitamente o que é detalhe do módulo e não deve ser consumido de fora |
| `package-info.java` por módulo | Declara a responsabilidade do módulo no próprio código |
| `shared/` | Concentra config e web transversais, sem virar depósito de domínio |

### Medição

Relatório em [`evidencias/modelo-modular.txt`](evidencias/modelo-modular.txt):

```text
Pacotes de primeiro nivel: 6
Total: 30       Cruzando o primeiro nivel: 7
```

### Leitura

**7 das 30 dependências cruzam o primeiro nível, ou 23%**, contra 88% na versão em camadas. A mesma
aplicação, as mesmas quatro capacidades, e o acoplamento entre capacidades caiu de quase total para
pontual.

As 7 que restam são dependências **reais de negócio**, não acidentes de organização:

```text
matricula.application -> usuario.domain                 2
matricula.application -> usuario.internal.persistence   2
matricula.application -> curso.domain                   1
matricula.application -> curso.internal.persistence     1
ranking.internal.persistence -> usuario.domain          1
```

Matrícula precisa mesmo de usuário e curso; ranking precisa mesmo ler dados de usuário. O ganho é
que agora essas dependências são **visíveis e contáveis**. Na versão em camadas elas existiam
igualmente, misturadas às outras 38.

### Uma observação sobre o modelo

Quatro dessas dependências apontam para `internal/persistence` de outro módulo:
`matricula.application` alcança `usuario.internal.persistence` e `curso.internal.persistence`.
O segmento `internal` sinaliza "não consuma de fora", e aqui ele é atravessado.

Isso não invalida o modelo. Mostra que a fronteira do monólito modular é **convenção, não barreira
física**. É precisamente o trade-off que o ADR-001 registra: a estrutura só se sustenta se a equipe
não a atravessar por conveniência. No Desafio 2, quando entrarem Ports & Adapters, esse é o tipo de
acesso que passa a ser mediado por contrato em vez de alcance direto ao repositório alheio.

---

## Quadro comparativo — as quatro bases, medidas com a mesma ferramenta

Todas as medições usam [`evidencias/analisar-dependencias.py`](evidencias/analisar-dependencias.py),
que conta os imports entre pacotes do próprio projeto.

| Base | Capacidades | Pacotes de 1º nível | Dependências | Cruzando o 1º nível | % |
|---|---|---|---|---|---|
| Modelo de referência, Camadas | 4 | 7 | 43 | 38 | **88%** |
| Modelo de referência, Modular | 4 | 6 | 30 | 7 | **23%** |
| Nossa AC1, antes | 1 | 7 | 8 | 8 | **100%** |
| Nosso Core, depois | 1 | 3 | 5 | 0 | **0%** |

### O que o quadro mostra

**A modularização reduz o acoplamento entre capacidades, não o número de dependências.** No modelo
de referência, passar de camadas para modular levou 43 dependências a 30, mas o que caiu de verdade
foi o que cruza fronteira: de 38 para 7. As dependências de negócio continuam existindo; deixam é de
estar misturadas.

**Nossos 0% precisam de ressalva.** Temos uma única capacidade implementada, então não há como haver
dependência entre módulos. O número é verdadeiro e insuficiente sozinho: ele prova que a fronteira
foi criada, não que ela resiste a pressão. A prova real virá quando `ranking` e `cursos` existirem,
e o modelo de referência já mostra o que esperar: algo em torno de 20%, não 0%.

**O padrão de comparação entre as duas versões da AC1 é o mesmo do modelo.** Saímos de 100% para 0%
de dependências cruzando o topo, com a mesma reorganização que levou o modelo de 88% para 23%. A
direção e a natureza da mudança se repetem, o que sustenta que a decisão do ADR-001 não foi tomada
no vazio.

---

## Conclusão dos itens 1 a 4

A leitura da solução proposta, a análise do legado e a medição dos dois códigos-modelo convergem
para o mesmo ponto: a organização por camada técnica faz o acoplamento entre capacidades crescer na
mesma proporção que o produto, e a organização por capacidade de negócio contém esse crescimento sem
introduzir custo de distribuição.

A AC1 chega ao Desafio 1 com base técnica sólida (domínio isolado, 25 testes, cobertura integral,
operação containerizada) e com a limitação estrutural esperada para um MVP de uma capacidade só.

Os itens 5 e 6 do roteiro não fazem parte desta entrega.
