# ADR 0001 — Registro inicial de decisões de arquitetura

- **Data:** 2026-09-07
- **Status:** aceito (baseline do TCC2)

Este ADR consolida as decisões `D-01`..`D-07` já registradas na Seção 14 do
[plano geral](../../geral_porject_plan.md). ADRs seguintes (`0002+`) registram
decisões novas, com data e contexto.

| ID | Decisão | Justificativa resumida |
|---|---|---|
| `D-01` | Motor de execução BPMN próprio (não Camunda/Flowable) | Contribuição central; acopla requisitos e scripts ao ciclo do token |
| `D-02` | Dados dinâmicos em tabela única com `JSONB` | Evita DDL em runtime; GIN atende à escala; rollback simples |
| `D-03` | GraalVM JavaScript como runtime de script | Sandbox com limites de recurso; JS moderno; integração com a JVM |
| `D-04` | Frontend Next.js com App Router | Roteamento e organização por *feature* |
| `D-05` | Versão publicada imutável | Instâncias em andamento precisam de definição estável |
| `D-06` | Vínculo de metadados pelo `id` do elemento BPMN | Mantém o XML como fonte de verdade do desenho |
| `D-07` | Rastreabilidade polimórfica (`type` + `id`) | Novos tipos de relação sem migração |

## Decisões de scaffolding (2026-09-07)

- **Build do backend:** Maven (com Maven Wrapper `mvnw`), por legibilidade do `pom.xml` e
  familiaridade. Sem Maven global obrigatório.
- **Spring Boot 4.1.1:** o plano (Seção 1.4) previa "3.x", mas o Spring Initializr não gera mais
  projetos 3.x. Adotado o Boot 4.1.1, que suporta Java 21. Consequência: nomes de starter no
  formato novo (`spring-boot-starter-webmvc`, starters de teste modularizados).
- **JDK 21:** Temurin 21 instalado lado a lado com o JDK 25 da máquina; `java.version` fixado em
  **21**. Um upgrade automático para Java 25 foi testado e **descartado** (mantém alinhamento com
  o plano e maior compatibilidade com libs que ainda miram 21, como GraalJS).
- **Monorepo:** pastas por módulo (`backend/`, `frontend/`, `database/`, `infra/`, `docs/`).
  Os módulos `M01`–`M15` são pacotes Java no mesmo backend (um processo), não serviços separados.

## Decisões do M01 — Core (2026-09-07)

Numeração alinhada à Seção 14 do plano geral.

- **`D-08` — Spring Boot 4.1.x + Java 21** (baseline revisto). Ver seção de scaffolding acima.
- **`D-09` — Contrato de erro HTTP (`IF-01`) = RFC 9457 `ProblemDetail`** (nativo do Spring), com a
  propriedade extra `traceId` e, em validação, `errors: [{field, message}]`. Alternativa
  (DTO de erro próprio) rejeitada por gerar mais código sem ganho.
- **`D-10` — Barramento de eventos de domínio (`IF-02`) sobre `ApplicationEventPublisher`** do
  Spring, exposto pela interface própria `DomainEventPublisher`. Permite
  `@TransactionalEventListener` e mantém o ArchUnit como guardião do desacoplamento.
- **`D-11` — Identificadores = `java.util.UUID` puro** (sem *wrapper* `Id` tipado), gerados pela
  aplicação via Hibernate `@UuidGenerator`. Typed IDs podem ser introduzidos depois se necessário.

Sem número de ADR (decisões táticas do MVP, reversíveis):

- **Segurança provisória no M01:** um `SecurityFilterChain` que libera toda a API (necessário
  porque o `starter-security` bloqueia tudo por padrão, incluindo Swagger/Actuator). O **M02**
  substitui por autenticação JWT + RBAC.
- **`audit_logs` (E-27)** e o filtro de correlação/MDC saem do baseline e entram com o **M15**.
