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
- **JDK:** Temurin 21 instalado lado a lado com o JDK 25 da máquina; o projeto fixa `java.version`
  em 21 para máxima compatibilidade com Spring Boot 3.x.
- **Monorepo:** pastas por módulo (`backend/`, `frontend/`, `database/`, `infra/`, `docs/`).
