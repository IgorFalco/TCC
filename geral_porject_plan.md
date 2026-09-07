# Plano Geral de Construção — Plataforma BPMN com Engenharia de Sistemas (TCC2)

> **Documento de referência do projeto.** Ele é a fonte única de verdade sobre escopo, arquitetura,
> módulos, contratos, modelo de dados e sequência de implementação. Deve ser lido antes do
> planejamento de qualquer atividade de desenvolvimento e atualizado sempre que uma decisão de
> projeto mudar.

- **Autor:** Igor Tonussi Santos Falco
- **Trabalho:** TCC2 — Engenharia de Sistemas / UFMG
- **Base:** monografia de TCC1 "Projeto e Desenvolvimento de uma Plataforma de Apoio à Modelagem e Acompanhamento de Processos Organizacionais"
- **Versão do documento:** 1.0
- **Status:** baseline de início do TCC2

---

## Estado de implementação

> Atualizar esta seção ao fim de cada bloco/módulo. É o primeiro lugar que uma nova sessão de
> trabalho deve consultar para saber de onde continuar.

| Bloco / Módulo | Estado | Evidência |
|---|---|---|
| Bloco 0 — Fundação (`M01`, infra) | ✅ concluído | commit `9b0aa21` em `main` |
| `M01` — Core | ✅ concluído | `backend/src/main/java/br/ufmg/plataforma/core/`; `mvnw clean test` = 9 verdes; `docker compose up` sobe `db`+`backend`, `/actuator/health` UP, Swagger OK |
| Bloco 1 — `M02` (IAM) | ⬜ **próximo** | — |
| Bloco 1 — `M03` (Projetos) | ⬜ | — |
| Bloco 1 — parte de `M14` (shell do front) | ⬜ | — |
| Blocos 2–9 | ⬜ | — |

**Baseline técnico efetivo** (ver `D-08`..`D-11` e `docs/adr/0001`):
Java 21 · Spring Boot **4.1.1** (starters de teste modularizados; `@WebMvcTest` em
`org.springframework.boot.webmvc.test.autoconfigure`) · pacote raiz `br.ufmg.plataforma` ·
um módulo = um pacote Java (não um serviço) · `mvnw` com `JAVA_HOME` =
`C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`.

**O que o `M01` já oferece ao `M02`:**
`BaseEntity` / `AuditableEntity` (`core.domain`) · `DomainException` /
`ResourceNotFoundException` / `BusinessRuleException` · `GlobalExceptionHandler` +
`ErrorType` (`core.web`, contrato `IF-01` RFC 9457) · `DomainEventPublisher` (`core.event`,
`IF-02`) · `@EnableJpaAuditing` + `AuditorAware` devolvendo `"system"` (`core.config` —
o `M02` deve trocar para o usuário autenticado) · `SecurityConfig` **provisório** que libera
tudo (`core.config` — o `M02` **substitui** por `SecurityFilterChain` com JWT + RBAC) ·
OpenAPI com `SecurityScheme` `bearer-jwt` já registrado · CORS para `app.cors.allowed-origins`.
Migrations: só `V1__baseline.sql` (`pgcrypto`); o `M02` cria `V2__iam.sql`.
`PageResponse`/DTO de paginação ainda **não existe** — criar no `M02`.

---

## 0. Como usar este documento

### 0.1 Para uma IA que for planejar ou executar uma atividade

1. Identifique o **módulo** (`M01`–`M15`) ao qual a atividade pertence (Seção 6).
2. Leia a ficha do módulo: responsabilidade, escopo, **interfaces de entrada e saída**, requisitos
   atendidos, dependências e critérios de aceitação.
3. Verifique as **dependências**: nunca implemente um módulo cujas dependências não estejam com o
   contrato definido (o contrato pode existir sem implementação completa — usar *stub*).
4. Consulte a Seção 7 (modelo de dados) e a Seção 8 (contratos de API) antes de criar entidades ou
   endpoints novos. Entidade ou endpoint fora do que está aqui exige **atualizar este documento**
   na mesma entrega.
5. Respeite as **regras invioláveis** da Seção 3.3.
6. Toda entrega deve satisfazer a *Definition of Done* da Seção 12.4.

### 0.2 Convenção de identificadores

| Prefixo | Significado |
|---|---|
| `NEC-xx` | Necessidade de parte interessada (herdada/atualizada do TCC1) |
| `RF-xx` | Requisito funcional |
| `RNF-xx` | Requisito não funcional |
| `M-xx` | Módulo do sistema |
| `IF-xx` | Interface entre módulos |
| `E-xx` | Entidade de domínio |
| `TV-xx` | Teste de verificação/validação |
| `R-xx` | Risco |
| `D-xx` | Decisão de arquitetura (ADR) |

Rastreabilidade obrigatória: `NEC → RF → M → IF/E → TV`.

---

## 1. Visão geral do sistema

### 1.1 Descrição em uma frase

Plataforma web que permite **modelar processos em BPMN, enriquecê-los com requisitos,
stakeholders e responsáveis, definir comportamento programável em JavaScript, criar classes de
dados dinâmicas pelo próprio frontend e executar/simular instâncias desses processos**, mantendo
rastreabilidade entre o que foi exigido, o que foi modelado e o que foi executado.

### 1.2 O que diferencia esta plataforma

1. **Modelagem + execução no mesmo ambiente.** O diagrama não é documentação: é a definição
   executável do processo.
2. **Extensibilidade por script.** Atividades de script, condições de gateway e validações são
   trechos de JavaScript versionados junto com o modelo, no banco.
3. **Metamodelo dinâmico.** O usuário cria classes de negócio pelo frontend; o backend passa a
   persistir, validar e expor CRUD dessas classes sem recompilação.
4. **Engenharia de Sistemas embutida.** Requisitos são objetos de primeira classe, vinculados a
   elementos BPMN, e a conformidade é verificável em tempo de execução.

### 1.3 Contexto do sistema (fronteiras)

```
        ┌──────────────────────────────────────────────────────────┐
        │                  Ambiente externo                         │
        │                                                           │
   Usuário ──HTTP──► [ Frontend Next.js ] ──REST/JSON──► [ Backend Spring Boot ]
        │                     │                                │     │
        │              bpmn-js / Monaco                        │     │
        │                                                 GraalJS   JDBC
        │                                                      │     │
        │                                              (sandbox JS) [PostgreSQL]
        └──────────────────────────────────────────────────────────┘
```

**Dentro da fronteira:** frontend, backend, motor de execução, motor de script, metamodelo,
banco de dados.
**Fora da fronteira:** navegador, sistema operacional, infraestrutura de hospedagem, ferramentas
corporativas externas (não integradas neste TCC, mas previstas por `RNF-07`).

### 1.4 Stack tecnológica (baseline)

| Camada | Tecnologia | Observação |
|---|---|---|
| Frontend | Next.js (App Router) + TypeScript + React | UI, editor, painéis |
| Editor BPMN | `bpmn-js` + `bpmn-moddle` | modelagem e overlays |
| Editor de código | Monaco Editor | edição dos scripts JS |
| Estado/dados no front | TanStack Query + Zustand (ou Context) | cache de API e estado local do editor |
| Estilo | Tailwind CSS + biblioteca de componentes (shadcn/ui) | consistência visual |
| Backend | Java 21 + Spring Boot 4.1.x | API REST, domínio, execução (ver `D-08`; o Initializr não gera mais 3.x) |
| Persistência | Spring Data JPA + Hibernate | mapeamento ORM |
| Banco | PostgreSQL 16 (colunas `JSONB`) | dados relacionais + dinâmicos |
| Migrations | Flyway | versionamento do schema |
| Motor de script | GraalVM JavaScript (`org.graalvm.polyglot`) | execução isolada de JS |
| Parser BPMN | Camunda `bpmn-model` ou parser próprio sobre XML | leitura do XML no backend |
| Segurança | Spring Security + JWT | autenticação e RBAC |
| Testes | JUnit 5, Mockito, Testcontainers, Vitest/RTL, Playwright | por camada |
| Documentação de API | springdoc-openapi (Swagger UI) | contrato vivo |

> Decisão `D-01`: **não usar um motor BPMN pronto** (Camunda/Flowable/Activiti) como executor. O
> motor de execução próprio é uma contribuição central do trabalho e permite acoplar requisitos e
> scripts ao ciclo de vida do token. A biblioteca `bpmn-model` (se usada) entra apenas como parser
> de XML, nunca como executor.

---

## 2. Escopo do TCC2

### 2.1 Dentro do escopo

- Autenticação, usuários, papéis e permissões.
- Projetos como unidade de organização (processos, classes, requisitos, usuários).
- Modelagem BPMN no navegador, com persistência do XML e dos metadados por elemento.
- Subconjunto executável da BPMN: evento de início, evento de fim, tarefa humana (`userTask`),
  tarefa de script (`scriptTask`), gateway exclusivo, gateway paralelo, fluxo de sequência
  (com condição em JS).
- Motor de execução por tokens, com instâncias, variáveis de instância, tarefas e histórico.
- Motor de script JavaScript sandboxado, com API controlada de acesso a dados e variáveis.
- Metamodelo: criação de classes dinâmicas pelo frontend, com atributos tipados, e CRUD genérico.
- Requisitos vinculados a elementos BPMN, com justificativa e verificação de conformidade.
- Matriz e visualização de rastreabilidade.
- Modo simulação: execução passo a passo, com destaque no diagrama e inspeção de variáveis.
- Caixa de tarefas por usuário/papel ("inbox").
- Exportação de dados (BPMN XML, JSON do projeto, matriz de rastreabilidade em CSV).
- Cenário demonstrativo completo para validação.

### 2.2 Fora do escopo (explícito)

- Cobertura completa da especificação BPMN 2.0 (subprocessos, eventos intermediários complexos,
  compensação, transações, *event subprocess*, *call activity*, *multi-instance*).
- Colaboração multiusuário em tempo real no mesmo diagrama.
- Motor de regras de negócio (DMN), mineração de processos, simulação estatística/Monte Carlo.
- Integrações com sistemas corporativos externos, conectores HTTP arbitrários a partir de scripts.
- Deploy em produção real, alta disponibilidade, multi-tenancy avançado.
- Versionamento completo de modelos com *merge* (haverá apenas versionamento linear simples).

> Regra: qualquer item da Seção 2.2 só entra no escopo com registro explícito de mudança neste
> documento e revisão do cronograma da Seção 11.

---

## 3. Princípios de Engenharia de Sistemas aplicados

### 3.1 Processos de ciclo de vida adotados (INCOSE, 2023)

| Processo INCOSE | Como aparece neste projeto |
|---|---|
| Definição de necessidades e requisitos de stakeholders | Seção 4 (necessidades) e 5 (requisitos) |
| Definição de requisitos do sistema | Seção 5, requisitos `RF`/`RNF` verificáveis |
| Definição de arquitetura | Seções 1, 6, 7 e 8 |
| Definição de projeto (design) | Fichas de módulo, contratos de interface, modelo de dados |
| Implementação | Seção 10 (fases) e 11 (cronograma) |
| Integração | Fase de integração ao fim de cada bloco (Seção 10) |
| Verificação | Seção 12 (testes por requisito) |
| Validação | Seção 12.3 (cenário demonstrativo + critérios qualitativos) |
| Gestão de riscos | Seção 13 |
| Gestão de decisões | Registro de ADR (Seção 14) |

### 3.2 Estratégia de decomposição

O sistema é decomposto em **módulos com fronteiras explícitas**. Cada módulo:

- tem **uma responsabilidade principal** e um dono de contrato;
- expõe suas capacidades **apenas** por interfaces declaradas (`IF-xx`);
- pode ser desenvolvido, testado e verificado **isoladamente**, com dependências substituídas por
  *stubs*;
- rastreia para pelo menos um requisito funcional.

No backend, cada módulo é um **pacote Java de primeiro nível** com estrutura interna padronizada:

```
br.ufmg.plataforma
├── core            (M01)  config, erros, auditoria, tipos comuns
├── iam             (M02)  usuários, papéis, permissões, JWT
├── project         (M03)  projetos, membros, escopo de dados
├── metamodel       (M04)  classes dinâmicas e atributos
├── datastore       (M05)  registros das classes dinâmicas (CRUD genérico)
├── bpmn            (M06)  definições, versões, parser, elementos
├── scripting       (M08)  runtime GraalJS, sandbox, API exposta
├── engine          (M09)  tokens, instâncias, handlers de elemento
├── task            (M10)  tarefas humanas, atribuição, inbox
├── requirements    (M11)  requisitos, vínculos, conformidade
├── simulation      (M12)  execução em modo simulação, passo a passo
└── reporting       (M13)  consultas consolidadas, matriz, exportação
```

Cada pacote de módulo segue: `api/` (controllers e DTOs) · `application/` (serviços de caso de
uso) · `domain/` (entidades e regras) · `infrastructure/` (repositórios, adaptadores).

**Regra de dependência:** um módulo só pode depender de módulos abaixo dele na ordem acima, mais
`core`. Dependência invertida deve ser resolvida por **evento de domínio** ou por interface
declarada no módulo de baixo e implementada pelo de cima.

### 3.3 Regras invioláveis do projeto

1. **Nenhum script do usuário executa fora do sandbox** (`M08`). Nem no frontend, nem via
   `eval` no backend.
2. **Nenhuma consulta a dados dinâmicos é montada por concatenação de string** vinda de script;
   a API de dados recebe filtros estruturados.
3. **Toda mudança de estado de instância de processo gera registro em `execution_history`.**
4. **Todo elemento BPMN referenciado por metadado usa o `id` do XML** como chave de vínculo; o
   backend nunca inventa ids.
5. **Definição de processo é imutável após publicação**; alterações geram nova versão.
6. **Todo requisito funcional tem ao menos um teste automatizado** que o verifica.
7. **O modelo de dados dinâmico nunca cria/altera tabelas em runtime** (ver `D-02`).

---

## 4. Partes interessadas e necessidades

Herdadas do TCC1 e ampliadas para o novo escopo.

| ID | Parte interessada | Necessidade |
|---|---|---|
| `NEC-01` | Administrador da plataforma | Organizar projetos e controlar usuários, papéis e permissões de acesso. |
| `NEC-02` | Modelador / analista de processos | Criar, editar, versionar e consultar modelos BPMN compreensíveis. |
| `NEC-03` | Analista de requisitos | Registrar, classificar e consultar requisitos ligados ao projeto, processo e atividades. |
| `NEC-04` | Analista de processos e gestor | Relacionar requisitos, papéis e responsáveis a elementos BPMN e entender por que cada atividade existe. |
| `NEC-05` | Participante do processo | Ver as tarefas atribuídas a si, seu contexto no fluxo e atualizar o andamento. |
| `NEC-06` | Desenvolvedor de processo (*power user*) | Programar comportamento do fluxo (decisões, cálculos, leitura e escrita de dados) sem alterar o código da plataforma. |
| `NEC-07` | Analista de negócio / modelador de dados | Definir as estruturas de dados manipuladas pelo processo (classes e atributos) pelo próprio frontend. |
| `NEC-08` | Modelador e avaliador | Simular o processo antes de operá-lo, verificando caminhos, decisões e dados. |
| `NEC-09` | Gestor, orientador e banca | Consultar, exportar e demonstrar evidências de modelagem, rastreabilidade, conformidade e execução. |

---

## 5. Requisitos

### 5.1 Requisitos funcionais

Coluna "TCC1" indica o requisito correspondente na monografia original, quando existir.

| ID | Requisito | Descrição verificável | NEC | TCC1 |
|---|---|---|---|---|
| `RF-01` | Autenticar usuário | O sistema deve autenticar usuários por credenciais e emitir token de sessão. | 01 | — |
| `RF-02` | Gerenciar usuários e papéis | O administrador deve criar, editar, desativar usuários e atribuir papéis. | 01 | RF07 |
| `RF-03` | Controlar permissões | O sistema deve restringir ações por papel (modelar, executar, administrar, consultar). | 01 | RF07/RNF11 |
| `RF-04` | Gerenciar projetos | Criar, listar, editar e remover projetos, com membros associados. | 01 | RF01 |
| `RF-05` | Criar modelo BPMN | Criar um diagrama BPMN dentro de um projeto. | 02 | RF02 |
| `RF-06` | Editar modelo BPMN | Editar elementos (eventos, tarefas humanas, tarefas de script, gateways, raias, fluxos). | 02 | RF03 |
| `RF-07` | Salvar e versionar modelo | Persistir o XML BPMN, com versão incremental e recuperação de versões anteriores. | 02 | RF04 |
| `RF-08` | Publicar definição de processo | Marcar uma versão como publicada e apta a gerar instâncias; versão publicada é imutável. | 02, 08 | — |
| `RF-09` | Validar modelo | Verificar estrutura mínima (início, fim, alcançabilidade, gateways com saídas) antes de publicar. | 02, 08 | — |
| `RF-10` | Cadastrar requisitos | Cadastrar requisitos ligados ao projeto, ao processo ou a um elemento BPMN. | 03 | RF05 |
| `RF-11` | Classificar requisitos | Classificar como funcional, não funcional ou restrição, com prioridade e status. | 03 | RF06 |
| `RF-12` | Cadastrar stakeholders e papéis | Registrar stakeholders, áreas, papéis e responsáveis do processo. | 04 | RF07 |
| `RF-13` | Associar requisitos a elementos BPMN | Vincular requisito a atividade, evento, gateway ou fluxo. | 04 | RF08 |
| `RF-14` | Associar responsáveis a atividades | Definir usuário, papel ou área responsável por uma atividade humana. | 04, 05 | RF09 |
| `RF-15` | Registrar justificativa de vínculo | Registrar justificativa textual para cada vínculo de rastreabilidade. | 04, 09 | RF12 |
| `RF-16` | Visualizar rastreabilidade | Exibir relações entre requisitos, papéis, responsáveis e elementos BPMN. | 04, 09 | RF10 |
| `RF-17` | Gerar matriz de rastreabilidade | Apresentar matriz/lista estruturada requisito × elemento × responsável. | 04, 09 | RF11 |
| `RF-18` | Verificar conformidade de requisitos | Registrar, ao concluir uma atividade, o atendimento (ou não) dos requisitos vinculados a ela. | 03, 04 | — |
| `RF-19` | Definir classes dinâmicas | Criar, editar e remover classes de negócio pelo frontend, com nome, chave e descrição. | 07 | — |
| `RF-20` | Definir atributos de classe | Definir atributos tipados (texto, número, booleano, data, enum, referência, lista) com obrigatoriedade e valor padrão. | 07 | — |
| `RF-21` | Persistir instâncias de classes dinâmicas | Criar, ler, atualizar e remover registros de uma classe dinâmica via API genérica. | 07, 06 | — |
| `RF-22` | Consultar dados dinâmicos por filtro | Consultar registros por filtros estruturados, com paginação e ordenação. | 06, 07 | — |
| `RF-23` | Editar script de elemento | Editar e salvar código JavaScript associado a tarefa de script, condição de fluxo ou validação. | 06 | — |
| `RF-24` | Executar tarefa de script | Executar o script de uma `scriptTask` durante a execução da instância, no sandbox. | 06 | — |
| `RF-25` | Avaliar condição de gateway por script | Determinar o fluxo de saída de um gateway exclusivo avaliando expressão JavaScript. | 06 | — |
| `RF-26` | Acessar dados a partir do script | Expor à API de script operações de consulta e gravação sobre classes dinâmicas e variáveis da instância. | 06, 07 | — |
| `RF-27` | Manter variáveis de instância | Guardar e recuperar variáveis (memória) da instância de processo entre atividades. | 06 | — |
| `RF-28` | Iniciar instância de processo | Iniciar uma instância a partir de uma definição publicada, com dados iniciais opcionais. | 05, 08 | RF13 |
| `RF-29` | Executar fluxo por tokens | Avançar a execução pelos elementos suportados, criando tarefas quando encontrar atividades humanas. | 05 | RF13 |
| `RF-30` | Consultar tarefas por responsável | Listar tarefas atribuídas ao usuário ou ao seu papel. | 05 | RF16 |
| `RF-31` | Atualizar status de tarefa | Reivindicar, concluir, bloquear ou reatribuir uma tarefa, com formulário de dados. | 05 | RF14 |
| `RF-32` | Consultar contexto da tarefa | Exibir requisitos relacionados, responsável, justificativa, variáveis e posição no fluxo BPMN. | 04, 05 | RF15 |
| `RF-33` | Registrar histórico de execução | Registrar cronologicamente eventos de instância, tarefa e script. | 05, 09 | — |
| `RF-34` | Simular processo | Executar uma instância em modo simulação, passo a passo, com avanço manual e inspeção de estado. | 08 | — |
| `RF-35` | Visualizar execução no diagrama | Destacar no diagrama BPMN o elemento atual, os já percorridos e o caminho tomado. | 05, 08 | — |
| `RF-36` | Exportar informações | Exportar XML BPMN, JSON do projeto e matriz de rastreabilidade (CSV). | 09 | RF17 |
| `RF-37` | Demonstrar cenário completo | Executar um cenário fim a fim: modelar, definir classe, escrever script, vincular requisitos, executar e acompanhar. | 09 | RF18 |

### 5.2 Requisitos não funcionais

| ID | Requisito | Critério de verificação |
|---|---|---|
| `RNF-01` | Usabilidade | Um usuário sem treinamento avançado conclui os fluxos principais seguindo o roteiro do cenário demonstrativo. |
| `RNF-02` | Modularidade | Cada módulo é um pacote isolado; verificação por teste de arquitetura (ArchUnit) das regras de dependência da Seção 3.2. |
| `RNF-03` | Manutenibilidade | Código organizado em camadas por módulo; cobertura de testes ≥ 60% no backend nos módulos `engine`, `scripting`, `datastore`. |
| `RNF-04` | Persistência | Dados de projeto, modelo, requisitos, classes, registros, instâncias e histórico sobrevivem a reinício da aplicação. |
| `RNF-05` | Segurança de acesso | Todo endpoint exige autenticação; ações restritas verificadas por papel; teste automatizado de acesso negado. |
| `RNF-06` | Isolamento de script | Script do usuário não acessa sistema de arquivos, rede, classes Java arbitrárias nem loop infinito (timeout obrigatório). |
| `RNF-07` | Extensibilidade | Novo tipo de elemento BPMN é suportado adicionando um `BpmnElementHandler` sem alterar o motor. |
| `RNF-08` | Desempenho básico | Operações CRUD respondem em < 500 ms e o avanço de um token em < 1 s em cenário de pequeno porte. |
| `RNF-09` | Portabilidade | Aplicação executa em navegadores modernos; backend e banco sobem via `docker compose`. |
| `RNF-10` | Rastreabilidade | Toda relação requisito ↔ elemento ↔ responsável é consultável e exportável. |
| `RNF-11` | Compreensibilidade | A UI exibe, junto ao elemento, o "porquê" (requisitos e justificativas), não apenas o desenho. |
| `RNF-12` | Auditabilidade | Toda operação de escrita relevante registra autor, data/hora e entidade afetada. |
| `RNF-13` | Observabilidade | Logs estruturados por instância de processo e por execução de script (com duração e resultado). |

---

## 6. Arquitetura e decomposição em módulos

### 6.1 Visão em camadas

```
┌─────────────────────────────────────────────────────────────────────────┐
│ APRESENTAÇÃO (Next.js)                                                  │
│  Shell/Auth · Editor BPMN · Painel de propriedades · Editor de script   │
│  Designer de classes · Formulários dinâmicos · Inbox · Rastreabilidade  │
│  Simulador · Relatórios                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│ API REST (Spring MVC) — controllers + DTOs + validação + segurança      │
├─────────────────────────────────────────────────────────────────────────┤
│ APLICAÇÃO — serviços de caso de uso, transações, orquestração           │
├───────────────┬───────────────┬──────────────┬──────────────────────────┤
│ DOMÍNIO       │ EXECUÇÃO      │ SCRIPTING    │ METAMODELO/DADOS         │
│ projeto,      │ tokens,       │ sandbox      │ classes dinâmicas,       │
│ processo,     │ handlers,     │ GraalJS,     │ atributos, registros,    │
│ requisito,    │ instâncias,   │ API exposta  │ consultas estruturadas   │
│ vínculo       │ tarefas       │              │                          │
├───────────────┴───────────────┴──────────────┴──────────────────────────┤
│ PERSISTÊNCIA — Spring Data JPA / Hibernate / Flyway                     │
├─────────────────────────────────────────────────────────────────────────┤
│ PostgreSQL (relacional + JSONB)                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Mapa de módulos e dependências

```
                    ┌────────────────────────────┐
                    │  M14 Frontend (Next.js)    │
                    └────────────┬───────────────┘
                                 │ REST
   ┌──────────┬──────────┬───────┴────┬───────────┬───────────┬──────────┐
   │ M13      │ M12      │ M11        │ M10       │ M09       │ M06      │
   │Reporting │Simulação │Requisitos  │Tarefas    │Engine     │BPMN      │
   └────┬─────┴────┬─────┴─────┬──────┴────┬──────┴─────┬─────┴────┬─────┘
        │          │           │           │            │          │
        └──────────┴───────────┴───────────┴────────────┤          │
                                                        ▼          ▼
                                            ┌───────────────┐ ┌──────────┐
                                            │ M08 Scripting │ │ M05 Data │
                                            └───────┬───────┘ └────┬─────┘
                                                    └──────┬───────┘
                                                           ▼
                                                    ┌─────────────┐
                                                    │ M04 Metamod.│
                                                    └──────┬──────┘
                                                           ▼
                             ┌──────────────┐  ┌───────────────┐  ┌──────────┐
                             │ M03 Projetos │──│ M02 IAM       │──│ M01 Core │
                             └──────────────┘  └───────────────┘  └──────────┘
                                        M15 Observabilidade (transversal)
```

### 6.3 Fichas dos módulos

---

#### `M01` — Core / Plataforma

- **Responsabilidade:** fundações compartilhadas — configuração, tratamento global de erros,
  tipos comuns (`Id`, `AuditInfo`, `Page`), utilitários de JSON, base de auditoria, eventos de
  domínio internos.
- **Entrega:** `@ControllerAdvice` com formato de erro padronizado, `BaseEntity` com auditoria
  (`createdAt`, `createdBy`, `updatedAt`, `updatedBy`), publicador de eventos de domínio,
  configuração de CORS, OpenAPI, Flyway e `docker-compose`.
- **Interfaces expostas:** `IF-01` — contrato de erro HTTP; `IF-02` — barramento de eventos de
  domínio (`DomainEventPublisher`).
- **Depende de:** nada.
- **Requisitos:** apoio a todos; `RNF-12`.
- **Critérios de aceitação:** aplicação sobe com `docker compose up`; erro de validação retorna
  payload padronizado; migration inicial aplicada; Swagger acessível.
- **Nota de implementação (2026-09-07):** entregue com `@RestControllerAdvice` + RFC 9457
  `ProblemDetail` (`D-09`), `BaseEntity`/`AuditableEntity`, `DomainEventPublisher` (`D-10`),
  CORS, OpenAPI e harness ArchUnit. Os tipos comuns `Id`/`AuditInfo`/`Page` viram `UUID` puro
  (`D-11`) + `PageResponse` a ser criado no M02; a tabela `audit_logs` (E-27) e o filtro de
  correlação/MDC migram para o `M15`. Segurança fica liberada por um filtro provisório até o M02.

---

#### `M02` — IAM (identidade e acesso)

- **Responsabilidade:** usuários, papéis, autenticação, autorização.
- **Escopo:** cadastro de usuário, login com emissão de JWT, refresh, hash de senha (BCrypt),
  papéis globais (`ADMIN`, `MODELER`, `DEVELOPER`, `MANAGER`, `PARTICIPANT`) e papéis por
  projeto, filtro de autorização por *authority*.
- **Modelo:** `E-01 User`, `E-02 Role`, `E-03 UserRole`.
- **Interfaces:** `IF-03 AuthService` (`authenticate`, `currentUser`), `IF-04 AccessGuard`
  (`canModel(projectId)`, `canExecute(projectId)`, `canAdminister(projectId)`).
- **Depende de:** `M01`.
- **Requisitos:** `RF-01`, `RF-02`, `RF-03`; `RNF-05`.
- **Critérios de aceitação:** login retorna token válido; endpoint protegido rejeita requisição
  sem token (401) e usuário sem papel (403); usuário autenticado é resolvível por qualquer
  serviço via `IF-03`.

---

#### `M03` — Projetos e workspace

- **Responsabilidade:** o projeto é o **agregado raiz de isolamento**. Tudo (processo, classe,
  requisito, instância) pertence a exatamente um projeto.
- **Escopo:** CRUD de projeto, associação de membros com papel no projeto, verificação de escopo
  em todas as consultas dos demais módulos.
- **Modelo:** `E-04 Project`, `E-05 ProjectMember`.
- **Interfaces:** `IF-05 ProjectScope` (`assertMember(projectId, userId, role)`,
  `resolveCurrentProject()`).
- **Depende de:** `M01`, `M02`.
- **Requisitos:** `RF-04`, `RF-03`.
- **Critérios de aceitação:** um usuário não-membro recebe 403 ao acessar qualquer recurso do
  projeto; remover projeto remove (ou bloqueia, se houver instâncias ativas) seus dependentes de
  forma consistente.

---

#### `M04` — Metamodelo (classes dinâmicas)

- **Responsabilidade:** permitir que o usuário defina, pelo frontend, as **classes de negócio**
  manipuladas pelos processos.
- **Escopo:** CRUD de `EntityClass` e `EntityAttribute`; tipos suportados: `STRING`, `TEXT`,
  `INTEGER`, `DECIMAL`, `BOOLEAN`, `DATE`, `DATETIME`, `ENUM`, `REFERENCE` (para outra classe),
  `LIST` (de tipo simples ou de referência). Regras: chave (`key`) única por projeto, imutável
  após criação de registros; alteração de atributo com registros existentes segue política de
  migração (Seção 7.4).
- **Modelo:** `E-06 EntityClass`, `E-07 EntityAttribute`.
- **Interfaces:** `IF-06 MetamodelRegistry` (`getClass(projectId, key)`, `describe(classId)`,
  `validate(classId, payload)` → lista de violações).
- **Depende de:** `M01`, `M03`.
- **Requisitos:** `RF-19`, `RF-20`.
- **Critérios de aceitação:** criar classe "Pedido" com 5 atributos pelo frontend; tentar salvar
  registro sem atributo obrigatório retorna erro de validação apontando o atributo.

---

#### `M05` — Datastore dinâmico

- **Responsabilidade:** persistir, validar e consultar **registros** das classes definidas em
  `M04`, sem alterar o schema físico.
- **Escopo:** CRUD genérico (`/api/projects/{p}/data/{classKey}`), validação via `IF-06`,
  consulta por filtros estruturados (campo, operador, valor), paginação, ordenação, resolução de
  referências (`expand`).
- **Modelo:** `E-08 EntityRecord` (coluna `data JSONB`).
- **Interfaces:** `IF-07 DataService` (`create`, `update`, `delete`, `findById`, `query(spec)`).
  Esta é a interface consumida pelo script (`M08`) — **nunca** SQL bruto.
- **Depende de:** `M01`, `M03`, `M04`.
- **Requisitos:** `RF-21`, `RF-22`.
- **Critérios de aceitação:** criar/consultar/atualizar registro pela API; filtro composto
  (`status = 'ABERTO' AND valor > 100`) retorna resultado correto e paginado; índice GIN
  utilizado (verificado com `EXPLAIN`).

> Decisão `D-02`: registros dinâmicos ficam em **uma tabela única com `JSONB`**, não em tabelas
> criadas em runtime. Justificativa: evita DDL dinâmico (risco de segurança e de migração),
> simplifica *rollback*, e o PostgreSQL suporta índices GIN e operadores de consulta sobre
> `JSONB` com desempenho adequado à escala do trabalho. Consequência aceita: menor garantia de
> integridade referencial no nível do banco — compensada por validação em `M04`/`M05`.

---

#### `M06` — Modelagem BPMN

- **Responsabilidade:** guardar a definição do processo e os metadados por elemento.
- **Escopo:** CRUD de `ProcessDefinition`, versionamento linear, publicação, persistência do XML,
  parsing do XML para extrair o grafo (`BpmnElement` + `SequenceFlow`), sincronização de
  metadados quando o diagrama muda (elemento removido → metadados órfãos sinalizados, nunca
  apagados silenciosamente), validação estrutural (`RF-09`).
- **Modelo:** `E-09 ProcessDefinition`, `E-10 ProcessVersion`, `E-11 BpmnElement`,
  `E-12 BpmnElementMetadata`.
- **Interfaces:** `IF-08 ProcessCatalog` (`getPublishedVersion`, `getGraph(versionId)`),
  `IF-09 BpmnParser` (`parse(xml)` → grafo).
- **Depende de:** `M01`, `M03`.
- **Requisitos:** `RF-05`, `RF-06`, `RF-07`, `RF-08`, `RF-09`.
- **Critérios de aceitação:** salvar e reabrir diagrama sem perda de layout; publicar versão 1,
  editar e publicar versão 2 sem afetar instâncias da versão 1; publicar modelo sem evento de fim
  é bloqueado com mensagem clara.

---

#### `M07` — Extensões de elemento (painel de propriedades)

> Módulo majoritariamente de frontend, com contrapartida de dados em `M06`.

- **Responsabilidade:** editar, para cada elemento BPMN selecionado, seus metadados: responsável
  (usuário/papel), requisitos vinculados, script associado, formulário de tarefa, variáveis de
  entrada/saída e documentação.
- **Escopo:** painel lateral acoplado ao `bpmn-js`, dirigido por *schema* (o que mostrar depende do
  tipo do elemento), com salvamento incremental por elemento.
- **Interfaces:** consome `IF-08`, `IF-11`, `IF-13`, `IF-15`.
- **Depende de:** `M06`, `M08`, `M10`, `M11`, `M14`.
- **Requisitos:** `RF-13`, `RF-14`, `RF-23`.
- **Critérios de aceitação:** selecionar uma `userTask` mostra abas Geral / Responsável /
  Requisitos / Formulário; selecionar `scriptTask` mostra aba Script com Monaco; selecionar
  gateway mostra condições por fluxo de saída.

---

#### `M08` — Scripting (motor de scripts JavaScript)

- **Responsabilidade:** armazenar, validar e **executar com segurança** o JavaScript escrito pelo
  usuário.
- **Escopo:**
  - `Script` como entidade versionada, vinculada a um elemento BPMN (ou a uma classe/validação).
  - Runtime GraalJS com contexto criado por execução, `HostAccess.EXPLICIT`, sem acesso a
    `java.*`, sem I/O, sem `Thread`, sem rede.
  - Limites: *timeout* de execução (padrão 5 s, configurável), limite de instruções
    (`sandbox.MaxStatements`), limite de memória do contexto quando disponível.
  - **API exposta ao script** (objeto global controlado):

    ```js
    // contexto da execução
    execution.get(name)               // lê variável da instância
    execution.set(name, value)        // grava variável da instância
    execution.variables               // snapshot somente leitura
    execution.instanceId
    execution.elementId

    // dados dinâmicos (delegam a IF-07, com escopo do projeto)
    data.find(classKey, filter, options)   // -> lista
    data.findOne(classKey, filter)
    data.create(classKey, payload)
    data.update(classKey, id, payload)
    data.remove(classKey, id)

    // utilidades
    log.info(msg) / log.warn(msg) / log.error(msg)
    util.now() / util.uuid() / util.parseDate(s) / util.format(...)

    // resultado (condições de gateway)
    return <boolean|any>
    ```
  - Registro de cada execução: script, instância, duração, sucesso/erro, saída de `log`.
- **Modelo:** `E-13 Script`, `E-14 ScriptExecutionLog`.
- **Interfaces:** `IF-10 ScriptRuntime` (`evaluate(script, context)` → `ScriptResult`),
  `IF-11 ScriptRepositoryApi` (CRUD de scripts por elemento).
- **Depende de:** `M01`, `M05`.
- **Requisitos:** `RF-23`, `RF-24`, `RF-25`, `RF-26`; `RNF-06`, `RNF-13`.
- **Critérios de aceitação:** script `while(true){}` é interrompido por timeout e a instância
  registra erro sem travar a aplicação; tentativa de `Java.type('java.io.File')` falha;
  `data.find` respeita o projeto corrente; erro de sintaxe é reportado com linha e coluna no
  editor.

---

#### `M09` — Engine (motor de execução BPMN)

- **Responsabilidade:** transformar uma definição publicada em execução concreta.
- **Escopo:**
  - `ProcessInstance` com estado (`RUNNING`, `SUSPENDED`, `COMPLETED`, `FAILED`, `CANCELLED`) e
    modo (`PRODUCTION` | `SIMULATION`).
  - `ExecutionToken` marcando posição corrente (um por caminho ativo).
  - Variáveis de instância (`ProcessVariable`, `JSONB`).
  - **Handlers polimórficos** por tipo de elemento, resolvidos por `supports(type)`:

    | Handler | Comportamento |
    |---|---|
    | `StartEventHandler` | cria token, aplica variáveis iniciais, avança |
    | `EndEventHandler` | consome token; sem tokens restantes → instância concluída |
    | `UserTaskHandler` | cria `TaskInstance`, resolve responsável, **suspende o token** |
    | `ScriptTaskHandler` | invoca `IF-10`, aplica variáveis de saída, avança |
    | `ExclusiveGatewayHandler` | avalia condições dos fluxos de saída (JS) na ordem; usa `default` se nenhuma for verdadeira |
    | `ParallelGatewayHandler` | *fork*: replica tokens; *join*: aguarda todos os tokens de entrada |
    | `SequenceFlowHandler` | move o token entre elementos, registra trilha |

  - Motor **orientado a eventos e reentrante**: `advance(instanceId)` processa até encontrar um
    ponto de espera (tarefa humana) ou o fim. Conclusão de tarefa dispara `advance` novamente.
  - Tratamento de erro: exceção em handler → instância em `FAILED` com histórico do erro, sem
    perder estado; possibilidade de retomar.
- **Modelo:** `E-15 ProcessInstance`, `E-16 ExecutionToken`, `E-17 ProcessVariable`,
  `E-18 ExecutionHistory`.
- **Interfaces:** `IF-12 ProcessEngine` (`start(versionId, vars, mode)`, `advance(instanceId)`,
  `signalTaskCompleted(taskId, outputVars)`, `cancel(instanceId)`),
  `IF-13 ElementHandlerRegistry` (registro de handlers — ponto de extensão de `RNF-07`).
- **Depende de:** `M01`, `M03`, `M06`, `M08`.
- **Requisitos:** `RF-27`, `RF-28`, `RF-29`, `RF-33`; `RNF-07`, `RNF-08`.
- **Critérios de aceitação:** processo linear (início → tarefa humana → script → fim) executa fim
  a fim; gateway exclusivo com duas saídas escolhe o caminho conforme variável; gateway paralelo
  sincroniza corretamente; histórico contém todos os passos em ordem.

---

#### `M10` — Tarefas humanas

- **Responsabilidade:** ciclo de vida das tarefas geradas por `userTask`.
- **Escopo:** criação (pelo `UserTaskHandler`), atribuição direta ou por papel (fila),
  reivindicação (*claim*), conclusão com dados de formulário, bloqueio, reatribuição, prazo
  opcional, inbox por usuário/papel, contexto da tarefa (`RF-32`).
- **Formulário de tarefa:** definido nos metadados do elemento como lista de campos ligados a
  atributos de uma classe dinâmica ou a variáveis da instância; renderizado dinamicamente no
  frontend.
- **Modelo:** `E-19 TaskInstance`, `E-20 TaskForm` (metadado), `E-21 TaskAssignment`.
- **Interfaces:** `IF-14 TaskService` (`listForUser`, `claim`, `complete`, `block`, `reassign`,
  `getContext`).
- **Depende de:** `M02`, `M09`, `M11` (para contexto), `M05` (para dados do formulário).
- **Requisitos:** `RF-14`, `RF-30`, `RF-31`, `RF-32`.
- **Critérios de aceitação:** tarefa aparece na inbox do responsável correto; concluir tarefa com
  dados grava variáveis e faz o processo avançar; usuário sem o papel não consegue concluir a
  tarefa.

---

#### `M11` — Requisitos e rastreabilidade

- **Responsabilidade:** tratar requisito como objeto de primeira classe e conectá-lo ao processo.
- **Escopo:**
  - CRUD de `Requirement` (código, título, descrição, tipo, prioridade, status, fonte).
  - CRUD de `Stakeholder` e `Role`.
  - `TraceabilityLink` (origem, destino, tipo de relação, justificativa) — tipos de relação:
    `SATISFIES`, `IMPACTS`, `RESPONSIBLE_FOR`, `DERIVES_FROM`, `VERIFIES`.
  - Matriz de rastreabilidade (requisito × elemento × responsável).
  - **Conformidade em execução (`RF-18`):** ao concluir uma tarefa cujo elemento tem requisitos
    vinculados, o sistema registra `RequirementCheck` (atendido / não atendido / não aplicável +
    observação). Opcionalmente, o requisito pode ter um **script verificador** avaliado
    automaticamente pelo `M08`.
  - Detecção de lacunas: requisitos sem elemento vinculado; elementos sem requisito.
- **Modelo:** `E-22 Requirement`, `E-23 Stakeholder`, `E-24 RoleDefinition`,
  `E-25 TraceabilityLink`, `E-26 RequirementCheck`.
- **Interfaces:** `IF-15 TraceabilityService` (`linksForElement`, `matrix(projectId)`,
  `gaps(projectId)`, `recordCheck(...)`).
- **Depende de:** `M03`, `M06`, `M02`.
- **Requisitos:** `RF-10` a `RF-18`, `RF-16`, `RF-17`; `RNF-10`, `RNF-11`.
- **Critérios de aceitação:** vincular requisito a uma atividade e vê-lo no painel e na matriz;
  relatório de lacunas lista corretamente requisitos órfãos; concluir tarefa com requisito
  pendente registra o *check*.

---

#### `M12` — Simulação e depuração

- **Responsabilidade:** executar o processo em modo controlado, para verificação pelo modelador.
- **Escopo:** instância com `mode = SIMULATION`, isolada dos dados de produção (política de
  isolamento na Seção 7.5); avanço **passo a passo** (`step`), avanço completo (`run`),
  auto-conclusão de tarefas humanas com dados informados pelo simulador, inspeção e edição de
  variáveis entre passos, *replay* do histórico, destaque do caminho no diagrama.
- **Interfaces:** `IF-16 SimulationService` (`startSimulation`, `step`, `setVariable`,
  `inspect`, `reset`).
- **Depende de:** `M09`, `M08`, `M10`, `M14`.
- **Requisitos:** `RF-34`, `RF-35`.
- **Critérios de aceitação:** simular processo com gateway percorrendo os dois caminhos ao alterar
  a variável entre execuções; dados criados em simulação não aparecem nas consultas de produção.

---

#### `M13` — Consultas, visualizações e exportação

- **Responsabilidade:** leitura consolidada e evidências.
- **Escopo:** painel do projeto (contagens, instâncias por status, tarefas em aberto), matriz de
  rastreabilidade em tela e em CSV, exportação de XML BPMN, exportação do projeto em JSON
  (modelo + requisitos + classes + scripts), linha do tempo de execução de uma instância.
- **Interfaces:** `IF-17 ReportingService`.
- **Depende de:** `M06`, `M09`, `M10`, `M11`.
- **Requisitos:** `RF-16`, `RF-17`, `RF-36`, `RF-37`.
- **Critérios de aceitação:** CSV da matriz abre corretamente e contém todos os vínculos; JSON
  exportado é suficiente para reconstruir o projeto conceitualmente.

---

#### `M14` — Frontend (Next.js)

- **Responsabilidade:** toda a experiência de uso.
- **Estrutura de rotas (App Router):**

```
/login
/projects
/projects/[projectId]
  /overview
  /processes                     lista de processos
  /processes/[processId]         editor BPMN + painel de propriedades (M07)
  /processes/[processId]/simulate
  /classes                       designer de classes dinâmicas (M04)
  /classes/[classKey]/records    grade de registros (M05)
  /requirements                  requisitos, stakeholders, papéis
  /traceability                  matriz e lacunas
  /instances                     instâncias e histórico
  /instances/[instanceId]        linha do tempo + diagrama destacado
  /tasks                         inbox do usuário
  /settings                      membros e permissões
```

- **Organização de código:** `app/` (rotas) · `features/<modulo>/` (componentes, hooks e serviços
  por módulo, espelhando `M02`–`M13`) · `components/ui/` · `lib/api/` (cliente HTTP tipado
  gerado ou escrito a partir do OpenAPI) · `lib/bpmn/` (integração com `bpmn-js`).
- **Regra:** cada *feature* do frontend consome exclusivamente os endpoints do módulo backend
  correspondente. Nada de chamada cruzada dentro do componente.
- **Depende de:** todos os módulos de backend.
- **Requisitos:** todos os de interface; `RNF-01`, `RNF-09`, `RNF-11`.

---

#### `M15` — Observabilidade e auditoria (transversal)

- **Responsabilidade:** logs estruturados, correlação por instância de processo, auditoria de
  escrita, métricas básicas (Actuator).
- **Escopo:** `AuditLog` genérico, `MDC` com `instanceId`/`projectId`/`userId`, endpoint de saúde.
- **Requisitos:** `RNF-12`, `RNF-13`.

---

## 7. Modelo de dados

### 7.1 Diagrama conceitual

```
User ──< ProjectMember >── Project
                              │
        ┌─────────────────────┼───────────────────────┬──────────────────┐
        │                     │                       │                  │
  ProcessDefinition      EntityClass             Requirement        Stakeholder
        │ 1..*                │ 1..*                  │                  │
  ProcessVersion         EntityAttribute              │             RoleDefinition
        │                     │                       │                  │
   BpmnElement ────────► EntityRecord            TraceabilityLink ◄───────┘
        │  │                 (JSONB)                  │
        │  └── BpmnElementMetadata ──► Script         │
        │                                             │
   ProcessInstance ──< ExecutionToken                 │
        │  ├──< ProcessVariable (JSONB)               │
        │  ├──< ExecutionHistory                      │
        │  └──< TaskInstance ──< RequirementCheck ────┘
        │                └──< TaskAssignment
        └──< ScriptExecutionLog
```

### 7.2 Entidades principais

| ID | Entidade | Campos-chave | Notas |
|---|---|---|---|
| `E-01` | `users` | id, username, email, password_hash, active | |
| `E-02` | `roles` | id, code, name | papéis globais |
| `E-03` | `user_roles` | user_id, role_id | |
| `E-04` | `projects` | id, name, key, description, audit | agregado raiz |
| `E-05` | `project_members` | project_id, user_id, project_role | `OWNER/MODELER/DEVELOPER/PARTICIPANT/VIEWER` |
| `E-06` | `entity_classes` | id, project_id, key, name, description, version | `UNIQUE(project_id, key)` |
| `E-07` | `entity_attributes` | id, class_id, key, label, type, required, default_value, enum_values, ref_class_id, order_index | |
| `E-08` | `entity_records` | id, project_id, class_id, data JSONB, mode, audit | índice GIN em `data` |
| `E-09` | `process_definitions` | id, project_id, key, name, description | |
| `E-10` | `process_versions` | id, definition_id, version, bpmn_xml TEXT, status (`DRAFT`/`PUBLISHED`/`ARCHIVED`), published_at | imutável quando publicada |
| `E-11` | `bpmn_elements` | id, version_id, element_id, element_type, name, source_ref, target_ref | extraído do XML pelo parser |
| `E-12` | `bpmn_element_metadata` | id, version_id, element_id, assignee_type, assignee_ref, form_definition JSONB, input_vars JSONB, output_vars JSONB, documentation | |
| `E-13` | `scripts` | id, project_id, version_id, element_id, purpose (`TASK`/`CONDITION`/`VALIDATION`), language (`JS`), code TEXT, revision | |
| `E-14` | `script_execution_logs` | id, script_id, instance_id, started_at, duration_ms, status, output TEXT, error TEXT | |
| `E-15` | `process_instances` | id, project_id, version_id, status, mode, started_by, started_at, ended_at, business_key | |
| `E-16` | `execution_tokens` | id, instance_id, current_element_id, status (`ACTIVE`/`WAITING`/`CONSUMED`), parent_token_id | |
| `E-17` | `process_variables` | id, instance_id, name, value JSONB, scope | |
| `E-18` | `execution_history` | id, instance_id, element_id, event_type, payload JSONB, occurred_at, actor | append-only |
| `E-19` | `task_instances` | id, instance_id, element_id, name, status, assignee_user_id, assignee_role, due_date, created_at, completed_at, output JSONB | |
| `E-22` | `requirements` | id, project_id, code, title, description, type, priority, status, source | |
| `E-23` | `stakeholders` | id, project_id, name, type, description | |
| `E-24` | `role_definitions` | id, project_id, name, description | papéis de processo |
| `E-25` | `traceability_links` | id, project_id, source_type, source_id, target_type, target_id, relation_type, justification | polimórfico por tipo+id |
| `E-26` | `requirement_checks` | id, requirement_id, task_id, result, note, checked_by, checked_at | |
| `E-27` | `audit_logs` | id, entity_type, entity_id, action, actor, occurred_at, diff JSONB | |

### 7.3 Formato dos dados dinâmicos

`entity_records.data` guarda um objeto plano, com chaves iguais às `entity_attributes.key`:

```json
{
  "numero": "PED-2026-014",
  "cliente": "8f2a...uuid-da-referencia",
  "valorTotal": 1520.75,
  "status": "ABERTO",
  "itens": ["cabo", "fonte"],
  "abertoEm": "2026-09-07T14:22:00Z"
}
```

Índices: `CREATE INDEX idx_records_data ON entity_records USING GIN (data jsonb_path_ops);`
mais índices B-tree parciais para atributos muito consultados, se necessário.

### 7.4 Política de evolução de classes dinâmicas

| Mudança | Política |
|---|---|
| Adicionar atributo opcional | Permitida sempre; registros antigos ficam sem a chave. |
| Adicionar atributo obrigatório | Exige valor padrão; aplicado por *backfill* em batch. |
| Renomear chave de atributo | Bloqueada na v1 (apenas o rótulo é editável). |
| Alterar tipo | Permitida só se não houver registros, ou com conversão explícita registrada. |
| Remover atributo | *Soft delete* — atributo marcado como inativo; dado permanece no JSON. |
| Remover classe | Bloqueada se houver registros ou referências em scripts/processos. |

### 7.5 Isolamento entre produção e simulação

- `process_instances.mode` e `entity_records.mode` assumem `PRODUCTION` ou `SIMULATION`.
- `IF-07 DataService` recebe o modo do contexto de execução e **filtra automaticamente**; um script
  rodando em simulação só enxerga e grava registros de simulação (mais os de produção em modo
  somente leitura, se a opção "usar dados reais" estiver ativa na simulação).
- Encerrar/limpar uma simulação pode descartar seus registros (`DELETE ... WHERE mode='SIMULATION'
  AND instance_id = ?`).

---

## 8. Contratos de API (visão principal)

Prefixo: `/api/v1`. Todas as rotas de projeto embutem `projectId`. Autenticação por `Bearer`.

### 8.1 IAM e projetos

```
POST   /auth/login                      → { token, refreshToken, user }
POST   /auth/refresh
GET    /users/me
GET    /users                           (ADMIN)
POST   /users                           (ADMIN)
GET    /projects
POST   /projects
GET    /projects/{p}
PUT    /projects/{p}
DELETE /projects/{p}
GET    /projects/{p}/members
POST   /projects/{p}/members
```

### 8.2 Modelagem BPMN

```
GET    /projects/{p}/processes
POST   /projects/{p}/processes                       { key, name }
GET    /projects/{p}/processes/{d}/versions
POST   /projects/{p}/processes/{d}/versions          { bpmnXml }         → cria DRAFT
GET    /projects/{p}/processes/{d}/versions/{v}      → { bpmnXml, elements[] }
PUT    /projects/{p}/processes/{d}/versions/{v}      { bpmnXml }         → só DRAFT
POST   /projects/{p}/processes/{d}/versions/{v}/validate → { valid, issues[] }
POST   /projects/{p}/processes/{d}/versions/{v}/publish
GET    /projects/{p}/processes/{d}/versions/{v}/elements/{eid}/metadata
PUT    /projects/{p}/processes/{d}/versions/{v}/elements/{eid}/metadata
```

### 8.3 Metamodelo e dados dinâmicos

```
GET    /projects/{p}/classes
POST   /projects/{p}/classes                 { key, name, attributes[] }
GET    /projects/{p}/classes/{classKey}
PUT    /projects/{p}/classes/{classKey}
DELETE /projects/{p}/classes/{classKey}

GET    /projects/{p}/data/{classKey}?page=&size=&sort=&filter=
POST   /projects/{p}/data/{classKey}         { ...payload }
GET    /projects/{p}/data/{classKey}/{id}?expand=cliente
PUT    /projects/{p}/data/{classKey}/{id}
DELETE /projects/{p}/data/{classKey}/{id}
POST   /projects/{p}/data/{classKey}/query   { filter: {...}, sort: [...], page, size }
```

Formato de filtro estruturado (também usado pela API de script):

```json
{
  "op": "AND",
  "conditions": [
    { "field": "status", "operator": "EQ", "value": "ABERTO" },
    { "field": "valorTotal", "operator": "GT", "value": 100 }
  ]
}
```
Operadores: `EQ, NE, GT, GTE, LT, LTE, IN, NOT_IN, LIKE, IS_NULL, IS_NOT_NULL, BETWEEN`.

### 8.4 Scripts

```
GET    /projects/{p}/versions/{v}/elements/{eid}/script
PUT    /projects/{p}/versions/{v}/elements/{eid}/script   { purpose, code }
POST   /projects/{p}/scripts/validate                     { code } → { valid, errors[] }
POST   /projects/{p}/scripts/test                         { code, mockVariables } → { result, logs, durationMs }
```

### 8.5 Execução, tarefas e simulação

```
POST   /projects/{p}/instances                 { versionId, variables, mode }
GET    /projects/{p}/instances?status=&versionId=
GET    /projects/{p}/instances/{i}             → estado, tokens, variáveis
GET    /projects/{p}/instances/{i}/history
POST   /projects/{p}/instances/{i}/cancel
POST   /projects/{p}/instances/{i}/step        (simulação)
GET    /projects/{p}/instances/{i}/variables
PUT    /projects/{p}/instances/{i}/variables/{name}   (simulação/depuração)

GET    /tasks?assignee=me&status=OPEN
GET    /projects/{p}/tasks
GET    /tasks/{t}                              → contexto completo (RF-32)
POST   /tasks/{t}/claim
POST   /tasks/{t}/complete                     { output: {...}, requirementChecks: [...] }
POST   /tasks/{t}/block                        { reason }
POST   /tasks/{t}/reassign                     { assigneeUserId | assigneeRole }
```

### 8.6 Requisitos, rastreabilidade e relatórios

```
GET/POST/PUT/DELETE  /projects/{p}/requirements
GET/POST/PUT/DELETE  /projects/{p}/stakeholders
GET/POST/PUT/DELETE  /projects/{p}/roles
GET/POST/DELETE      /projects/{p}/traceability-links
GET    /projects/{p}/traceability/matrix
GET    /projects/{p}/traceability/gaps
GET    /projects/{p}/export/json
GET    /projects/{p}/export/traceability.csv
GET    /projects/{p}/processes/{d}/versions/{v}/export/bpmn
```

---

## 9. Fluxos de execução de referência

### 9.1 Início e avanço de uma instância

```
Frontend            API            ProcessEngine        HandlerRegistry       DB
   │  POST /instances  │                 │                     │              │
   ├──────────────────►│  start(v,vars)  │                     │              │
   │                   ├────────────────►│ carrega grafo (M06) │              │
   │                   │                 ├─────────────────────┼─────────────►│
   │                   │                 │ cria instance+token │              │
   │                   │                 │ loop advance():     │              │
   │                   │                 │   resolve handler ──►│             │
   │                   │                 │   handler.execute() │              │
   │                   │                 │   (script → M08)    │              │
   │                   │                 │   registra história ├─────────────►│
   │                   │                 │ até WAITING ou END  │              │
   │◄──────────────────┤ 201 {instance}  │                     │              │
```

Pseudocódigo do laço:

```java
void advance(UUID instanceId) {
  ProcessInstance instance = load(instanceId);
  while (instance.hasActiveToken()) {
    ExecutionToken token = instance.nextActiveToken();
    BpmnElement element = graph.elementOf(token);
    BpmnElementHandler handler = registry.resolve(element.getType());
    HandlerOutcome outcome = handler.execute(new ExecutionContext(instance, token), element);
    history.record(instance, element, outcome);
    if (outcome.isWaiting()) { token.markWaiting(); continue; }
    token.moveTo(outcome.nextElements());   // pode gerar fork
  }
  if (instance.hasNoActiveToken()) instance.complete();
}
```

### 9.2 Conclusão de tarefa humana

1. Usuário abre a tarefa e vê contexto (`IF-14.getContext`): dados do formulário, requisitos
   vinculados, justificativas, posição no diagrama.
2. Preenche o formulário e, se houver requisitos vinculados, marca os *checks* (`RF-18`).
3. `POST /tasks/{t}/complete` → `M10` valida permissão, grava `output` como variáveis
   (`IF-12`), registra `RequirementCheck` (`IF-15`) e chama `signalTaskCompleted`.
4. `M09` reativa o token e continua o avanço.

### 9.3 Avaliação de gateway exclusivo

1. `ExclusiveGatewayHandler` obtém os fluxos de saída ordenados.
2. Para cada fluxo com condição, chama `IF-10.evaluate(script, ctx)`; espera retorno *booleano*.
3. Primeiro `true` vence. Nenhum `true` → usa fluxo `default`. Sem `default` → instância `FAILED`
   com erro "nenhum caminho satisfeito".
4. Toda avaliação vira registro em `script_execution_logs` e `execution_history`.

### 9.4 Modelo de segurança do sandbox de script

```java
Context ctx = Context.newBuilder("js")
    .allowHostAccess(HostAccess.EXPLICIT)     // só métodos anotados @HostAccess.Export
    .allowHostClassLookup(className -> false) // nenhuma classe Java acessível
    .allowIO(false)
    .allowCreateThread(false)
    .allowNativeAccess(false)
    .option("engine.WarnInterpreterOnly", "false")
    .option("sandbox.MaxStatements", String.valueOf(maxStatements))
    .build();
```

- Execução em `ExecutorService` com `Future.get(timeout)`; ao estourar, `ctx.close(true)`
  (cancelamento forçado).
- Bindings injetados: `execution`, `data`, `log`, `util` — objetos Java com métodos marcados
  `@HostAccess.Export`.
- Conversão de tipos: JSON ↔ `Map`/`List` na fronteira; nunca expor entidades JPA ao script.
- Cada execução usa contexto novo (sem estado residual entre instâncias).

---

## 10. Plano de implementação por blocos

Cada bloco é **entregável e verificável de forma independente**. Nenhum bloco começa sem que o
anterior tenha passado nos seus critérios de aceitação (exceto trabalho de frontend que pode
correr em paralelo com *mocks*).

### Bloco 0 — Fundação

| Item | Conteúdo |
|---|---|
| Módulos | `M01`, `M15` |
| Entregas | repositório monorepo (`/backend`, `/frontend`, `/docs`), `docker-compose` (Postgres + backend + frontend), Flyway com migration inicial, tratamento global de erros, OpenAPI, pipeline de testes, README de execução |
| Aceitação | `docker compose up` sobe tudo; `GET /actuator/health` OK; Swagger acessível; teste de arquitetura (ArchUnit) rodando |

### Bloco 1 — Identidade, projetos e shell do frontend

| Item | Conteúdo |
|---|---|
| Módulos | `M02`, `M03`, parte de `M14` |
| Requisitos | `RF-01` a `RF-04` |
| Entregas | login com JWT, CRUD de usuário/papel, CRUD de projeto e membros, layout do app, rota protegida, seletor de projeto |
| Aceitação | usuário faz login, cria projeto, adiciona membro; não-membro recebe 403 |

### Bloco 2 — Modelagem BPMN

| Item | Conteúdo |
|---|---|
| Módulos | `M06`, parte de `M14` |
| Requisitos | `RF-05` a `RF-09` |
| Entregas | editor `bpmn-js` integrado, salvar/carregar XML, versionamento e publicação, parser no backend gerando `bpmn_elements`, validação estrutural |
| Aceitação | criar, salvar, reabrir e publicar um processo; grafo persistido bate com o XML |

### Bloco 3 — Metamodelo e dados dinâmicos

| Item | Conteúdo |
|---|---|
| Módulos | `M04`, `M05` |
| Requisitos | `RF-19` a `RF-22` |
| Entregas | designer de classes no frontend, CRUD genérico, validação por metamodelo, consulta com filtro estruturado, grade de registros |
| Aceitação | criar classe pelo frontend e manipular registros dela sem tocar em código do backend |

### Bloco 4 — Motor de scripts

| Item | Conteúdo |
|---|---|
| Módulos | `M08` |
| Requisitos | `RF-23` a `RF-26`; `RNF-06` |
| Entregas | runtime GraalJS com sandbox, API `execution`/`data`/`log`/`util`, persistência de scripts, editor Monaco com validação e endpoint de teste de script |
| Aceitação | testes de segurança (timeout, acesso Java, I/O) passam; script consulta e cria registro dinâmico |

> Este bloco pode ser desenvolvido **em paralelo** ao Bloco 2, pois depende apenas de `M05`.

### Bloco 5 — Motor de execução

| Item | Conteúdo |
|---|---|
| Módulos | `M09`, `M10` |
| Requisitos | `RF-27` a `RF-33` |
| Entregas | instâncias, tokens, variáveis, handlers (início, fim, tarefa humana, tarefa de script, gateway exclusivo, gateway paralelo), histórico, inbox, formulário dinâmico de tarefa |
| Aceitação | processo com humano + script + gateway executa fim a fim; histórico completo; inbox funcional |

### Bloco 6 — Requisitos e rastreabilidade

| Item | Conteúdo |
|---|---|
| Módulos | `M11`, `M07` |
| Requisitos | `RF-10` a `RF-18` |
| Entregas | CRUD de requisitos/stakeholders/papéis, painel de propriedades com aba de requisitos, vínculos com justificativa, matriz, lacunas, *checks* na conclusão de tarefa |
| Aceitação | matriz reflete os vínculos; conclusão de tarefa registra conformidade |

### Bloco 7 — Simulação e visualização de execução

| Item | Conteúdo |
|---|---|
| Módulos | `M12` |
| Requisitos | `RF-34`, `RF-35` |
| Entregas | modo simulação, `step`, edição de variáveis, destaque de caminho no diagrama, isolamento de dados |
| Aceitação | simular os dois caminhos de um gateway; dados de simulação isolados |

### Bloco 8 — Relatórios, exportação e cenário demonstrativo

| Item | Conteúdo |
|---|---|
| Módulos | `M13` |
| Requisitos | `RF-36`, `RF-37` |
| Entregas | painel do projeto, exportações (BPMN/JSON/CSV), cenário demonstrativo carregável por *seed*, roteiro de demonstração |
| Aceitação | cenário completo executado do zero em menos de 15 minutos seguindo o roteiro |

### Bloco 9 — Integração, V&V e fechamento

| Item | Conteúdo |
|---|---|
| Entregas | testes de integração ponta a ponta (Playwright), verificação de todos os `RF` pela Tabela 12.1, avaliação qualitativa, documentação técnica, monografia e apresentação |

---

## 11. Cronograma (alinhado ao TCC1)

| Período | Blocos | Marcos |
|---|---|---|
| **Ago/2026** | Bloco 0, Bloco 1 | Ambiente pronto, autenticação e projetos funcionando |
| **Set/2026** | Bloco 2, Bloco 3, início do Bloco 4 | Modelagem BPMN persistida e classes dinâmicas operando |
| **Out/2026** | Bloco 4 (fim), Bloco 5, Bloco 6 | **Marco crítico:** processo executa fim a fim com script e rastreabilidade |
| **Nov/2026** | Bloco 7, Bloco 8, início do Bloco 9 | Simulação, exportações e cenário demonstrativo prontos |
| **Dez/2026** | Bloco 9 | V&V concluída, documentação, redação final e apresentação |

**Marco de contingência:** se ao final de outubro o Bloco 5 não estiver aceito, reduzir o escopo
nesta ordem: (1) gateway paralelo → adiado; (2) simulação passo a passo → vira execução simples
em modo simulação; (3) exportação JSON do projeto → apenas CSV e BPMN. Requisitos afetados devem
ser marcados como "escopo reduzido" na monografia, com justificativa.

---

## 12. Verificação e validação

### 12.1 Matriz requisito × módulo × teste

| RF | Módulos | Teste |
|---|---|---|
| RF-01, RF-02, RF-03 | M02 | `TV-01` autenticação, papéis e acesso negado |
| RF-04 | M03 | `TV-02` ciclo de vida de projeto e escopo |
| RF-05–RF-07 | M06 | `TV-03` criar, editar, salvar, reabrir modelo |
| RF-08, RF-09 | M06 | `TV-04` validação e publicação de versão |
| RF-10, RF-11 | M11 | `TV-05` CRUD e classificação de requisitos |
| RF-12 | M11 | `TV-06` stakeholders e papéis |
| RF-13, RF-15 | M11, M07 | `TV-07` vínculo requisito ↔ elemento com justificativa |
| RF-14 | M07, M10 | `TV-08` responsável por atividade |
| RF-16, RF-17 | M11, M13 | `TV-09` visualização e matriz de rastreabilidade |
| RF-18 | M11, M10 | `TV-10` registro de conformidade na conclusão da tarefa |
| RF-19, RF-20 | M04 | `TV-11` criação de classe e atributos, validação |
| RF-21, RF-22 | M05 | `TV-12` CRUD dinâmico e consulta filtrada |
| RF-23 | M08, M07 | `TV-13` edição, validação e persistência de script |
| RF-24, RF-26 | M08, M09 | `TV-14` execução de `scriptTask` com acesso a dados |
| RF-25 | M08, M09 | `TV-15` decisão de gateway por script |
| RF-27 | M09 | `TV-16` variáveis persistidas entre atividades |
| RF-28, RF-29 | M09 | `TV-17` execução fim a fim por tokens |
| RF-30, RF-31 | M10 | `TV-18` inbox, claim, conclusão |
| RF-32 | M10, M11 | `TV-19` contexto completo da tarefa |
| RF-33 | M09, M15 | `TV-20` histórico completo e ordenado |
| RF-34, RF-35 | M12 | `TV-21` simulação passo a passo e destaque no diagrama |
| RF-36 | M13 | `TV-22` exportações BPMN/JSON/CSV |
| RF-37 | todos | `TV-23` cenário demonstrativo completo |
| RNF-06 | M08 | `TV-24` suíte de segurança do sandbox |
| RNF-02 | todos | `TV-25` teste de arquitetura (regras de dependência) |

### 12.2 Estratégia de testes

| Nível | Ferramenta | Alvo |
|---|---|---|
| Unitário (backend) | JUnit 5 + Mockito | handlers, validadores, serviços de domínio |
| Integração (backend) | Spring Boot Test + Testcontainers (Postgres) | repositórios, CRUD dinâmico, motor completo |
| Arquitetura | ArchUnit | regras de dependência entre módulos (`RNF-02`) |
| Segurança de script | JUnit + casos maliciosos | timeout, acesso a classe Java, I/O, memória |
| Unitário (frontend) | Vitest + Testing Library | componentes de formulário dinâmico e painéis |
| Ponta a ponta | Playwright | cenário demonstrativo automatizado |

### 12.3 Cenário demonstrativo de validação

**Processo:** "Solicitação de compra".

1. **Classes dinâmicas:** `Fornecedor` (nome, cnpj, ativo) e `SolicitacaoCompra` (numero,
   solicitante, descricao, valorTotal, fornecedor → referência, status).
2. **Requisitos:** `REQ-01` "Toda solicitação acima de R$ 5.000 deve ter aprovação gerencial"
   (funcional); `REQ-02` "O fornecedor deve estar ativo no cadastro" (restrição); `REQ-03` "O
   solicitante deve ser notificado do resultado" (funcional).
3. **Modelo BPMN:** início → tarefa humana *Registrar solicitação* → tarefa de script *Validar
   fornecedor e calcular total* → gateway exclusivo *Valor > 5000?* → (sim) tarefa humana
   *Aprovar solicitação* / (não) tarefa de script *Aprovar automaticamente* → tarefa de script
   *Registrar resultado* → fim.
4. **Scripts:** o de validação consulta `Fornecedor` por CNPJ e falha se inativo (`REQ-02`); a
   condição do gateway lê `execution.get('valorTotal') > 5000` (`REQ-01`); o script final grava a
   `SolicitacaoCompra` com o status decidido (`REQ-03`).
5. **Rastreabilidade:** `REQ-01` → gateway e tarefa de aprovação (`SATISFIES`); `REQ-02` → tarefa
   de script de validação; `REQ-03` → tarefa de registro; papel *Gerente* → tarefa de aprovação
   (`RESPONSIBLE_FOR`).
6. **Execução:** iniciar duas instâncias (uma acima e outra abaixo do limite), concluir as tarefas
   pelos usuários corretos, registrar os *checks* de requisito e conferir o histórico.
7. **Simulação:** repetir o caminho de aprovação em modo simulação, passo a passo.
8. **Evidências:** matriz de rastreabilidade, linha do tempo das instâncias, exportações.

### 12.4 Definition of Done (por entrega)

- [ ] Requisito(s) atendido(s) identificados e citados no *pull request* / commit.
- [ ] Migration Flyway criada quando houve mudança de schema.
- [ ] Testes automatizados cobrindo o caminho feliz e ao menos um caminho de erro.
- [ ] Endpoint documentado no OpenAPI, com exemplos.
- [ ] Regras de dependência entre módulos respeitadas (ArchUnit verde).
- [ ] Frontend correspondente funcionando contra a API real (não apenas mock).
- [ ] Registro de decisão (ADR) criado se houve escolha arquitetural relevante.
- [ ] Este documento atualizado se algo aqui mudou.

---

## 13. Riscos

| ID | Risco | Impacto | Prob. | Mitigação | Gatilho de ação |
|---|---|---|---|---|---|
| `R-01` | Escopo excessivo para o prazo | Alto | Alta | Blocos independentes e plano de contingência (Seção 11) | Bloco 5 não aceito até 31/out |
| `R-02` | Complexidade do motor BPMN próprio | Alto | Média | Subconjunto fechado da notação; handlers isolados e testáveis | Mais de 2 semanas no Bloco 5 sem fluxo fim a fim |
| `R-03` | Sandbox de script inseguro ou instável | Alto | Média | GraalJS com política restritiva, suíte de testes de segurança, timeout obrigatório | Qualquer teste de `TV-24` falhando |
| `R-04` | Desempenho do modelo `JSONB` em consultas | Médio | Baixa | Índices GIN, filtros estruturados, paginação obrigatória | Consulta > 500 ms com 10k registros |
| `R-05` | Integração `bpmn-js` ↔ metadados | Médio | Média | Vínculo apenas por `id` do elemento; metadados órfãos sinalizados, não apagados | Perda de metadado ao editar diagrama |
| `R-06` | Interface de rastreabilidade confusa | Médio | Média | Começar por lista/tabela; grafo só se sobrar tempo | Feedback negativo na revisão de UI |
| `R-07` | Validação sem usuários reais | Médio | Alta | Cenário demonstrativo formal + critérios qualitativos + roteiro reprodutível | — |
| `R-08` | Mudança de tecnologia (ex.: GraalJS indisponível) | Médio | Baixa | `IF-10` isola o runtime; alternativa: Rhino/Nashorn ou serviço Node isolado | Bloqueio técnico no Bloco 4 |
| `R-09` | Complexidade do gateway paralelo (join) | Médio | Média | Implementar por último dentro do Bloco 5; se necessário, adiar | Bloco 5 atrasado |
| `R-10` | Acoplamento acidental entre módulos | Médio | Média | ArchUnit no pipeline desde o Bloco 0 | Teste de arquitetura vermelho |

---

## 14. Registro de decisões de arquitetura (ADR)

| ID | Decisão | Alternativas | Justificativa |
|---|---|---|---|
| `D-01` | Motor de execução BPMN próprio | Camunda/Flowable embarcado | Contribuição central do trabalho; permite acoplar requisitos e scripts ao ciclo do token |
| `D-02` | Dados dinâmicos em tabela única com `JSONB` | Tabela por classe criada em runtime; EAV | Evita DDL dinâmico; `JSONB` + GIN atende à escala; migração e rollback simples |
| `D-03` | GraalJS como runtime de script | Nashorn (obsoleto); Node externo; Rhino | Sandbox com limites de recurso, suporte moderno de JS, integração nativa com JVM |
| `D-04` | Frontend Next.js com App Router | SPA React pura; SSR completo | Roteamento, organização por *feature* e ergonomia de desenvolvimento |
| `D-05` | Versão publicada imutável | Edição direta do modelo em uso | Instâncias em andamento precisam de definição estável; garante rastreabilidade |
| `D-06` | Vínculo de metadados pelo `id` do elemento BPMN | Cópia do elemento em tabela própria | Mantém o XML como fonte de verdade do desenho |
| `D-07` | Rastreabilidade polimórfica (`type` + `id`) | Tabela de vínculo por par de tipos | Flexibilidade para novos tipos de relação sem migração |
| `D-08` | Spring Boot **4.1.x** + Java **21** (baseline revisto; upgrade p/ Java 25 descartado) | Boot 3.x (Initializr não gera mais); Java 25 | Manter alinhamento com o plano e compatibilidade de libs; Boot 4 suporta Java 21 |
| `D-09` | Contrato de erro HTTP = RFC 9457 `ProblemDetail` + `traceId` (`IF-01`) | DTO de erro próprio | Nativo do Spring, menos código |
| `D-10` | Eventos de domínio (`IF-02`) sobre `ApplicationEventPublisher` via interface `DomainEventPublisher` | Barramento próprio; `@DomainEvents` do Spring Data | Reaproveita infra do Spring; ArchUnit garante o desacoplamento |
| `D-11` | Identificadores = `UUID` puro gerado pela aplicação (`@UuidGenerator`) | Typed IDs (`Id` wrapper) | Menos cerimônia para um MVP; introduzível depois |

> Decisões `D-08`..`D-11` registradas em 2026-09-07 (Bloco 0 / M01). Detalhe em `docs/adr/0001`.
> Novas decisões devem ser acrescentadas nesta tabela com data e contexto.

---

## 15. Instruções operacionais para a IA de apoio

Ao receber uma tarefa de desenvolvimento:

1. **Contextualize:** cite o módulo, os requisitos e o bloco a que a tarefa pertence.
2. **Verifique dependências:** se a tarefa depender de módulo não implementado, proponha o *stub*
   ou o contrato mínimo antes.
3. **Não invente entidade, endpoint ou tipo** fora das Seções 7 e 8. Se for necessário, proponha
   explicitamente a alteração deste documento antes de codificar.
4. **Backend:** siga a estrutura `api/application/domain/infrastructure` do pacote do módulo.
   Entidade JPA nunca sai da camada de API — sempre DTO.
5. **Frontend:** siga `features/<modulo>/`; chamadas de API só por `lib/api/`; nada de `fetch`
   direto em componente.
6. **Toda alteração de schema** vem com migration Flyway numerada e reversível conceitualmente.
7. **Segurança:** qualquer código que toque em script do usuário, consulta dinâmica ou permissão
   exige teste negativo (o que NÃO pode acontecer).
8. **Entregue verificável:** cada entrega termina com como testar (comando ou passos manuais).
9. **Ao concluir**, informe quais itens da Seção 12.4 foram satisfeitos e quais ficaram pendentes.

### Prompt-modelo para abrir uma atividade

```
Contexto: projeto TCC2 — Plataforma BPMN + Engenharia de Sistemas.
Documento de referência: plano_geral_tcc2.md (leia as seções 3, 6, 7, 8 e 15).
Tarefa: <descrição>
Módulo: <Mxx>   Bloco: <n>   Requisitos: <RF-xx, ...>
Restrições: respeitar regras invioláveis (3.3) e regras de dependência (3.2).
Entrega esperada: <código / migration / teste / doc>, com instruções de verificação.
```

---

## 16. Glossário

| Termo | Definição |
|---|---|
| **Definição de processo** | Modelo BPMN salvo e versionado; o "molde" do processo. |
| **Instância de processo** | Execução concreta de uma versão publicada. |
| **Token** | Marcador da posição corrente de um caminho de execução dentro da instância. |
| **Handler** | Componente que sabe executar um tipo específico de elemento BPMN. |
| **Classe dinâmica** | Estrutura de dados definida pelo usuário no frontend e persistida como metadado. |
| **Registro** | Instância de uma classe dinâmica, armazenada em `JSONB`. |
| **Variável de instância** | Dado em memória do processo, vivo durante a execução da instância. |
| **Script** | Trecho de JavaScript do usuário, executado em sandbox pelo backend. |
| **Vínculo de rastreabilidade** | Relação explícita e justificada entre requisito, elemento BPMN e responsável. |
| **Check de requisito** | Registro, em execução, de que um requisito foi (ou não) atendido em uma atividade. |
| **Simulação** | Execução em modo isolado, destinada a verificação pelo modelador. |

---

## 17. Referências do projeto

- INCOSE. *Systems Engineering Handbook*, 5. ed., 2023.
- KOSSIAKOFF, A. et al. *Systems Engineering Principles and Practice*, 3. ed., 2020.
- KOCBEK, M. et al. Business Process Model and Notation: the current state of affairs, 2015.
- ODEH, Y. BPMN in engineering software requirements, 2017.
- ELDIN, A. N. et al. Low-code solutions for business process dataflows, 2026.
- OMG. *Business Process Model and Notation (BPMN) Version 2.0*.
- bpmn.io — `bpmn-js`; Spring Boot; Spring Data JPA; GraalVM JavaScript; PostgreSQL (documentação
  oficial de cada projeto).

---

**Controle de versões deste documento**

| Versão | Data | Alteração |
|---|---|---|
| 1.0 | 2026-09-07 | Baseline inicial do TCC2, derivada da monografia de TCC1 e da definição de plataforma. |
