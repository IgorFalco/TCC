# Plataforma BPMN com Engenharia de Sistemas — TCC2

Monólito modular: plataforma web para **modelar processos em BPMN**, enriquecê-los com
requisitos, stakeholders e responsáveis, definir comportamento em JavaScript, criar classes de
dados dinâmicas pelo frontend e executar/simular instâncias desses processos.

> A fonte única de verdade sobre escopo, arquitetura, módulos, contratos e modelo de dados é
> [`geral_porject_plan.md`](geral_porject_plan.md). Leia-o antes de qualquer atividade de
> desenvolvimento.

## Estrutura do repositório

| Pasta | Módulo do plano | Conteúdo |
|---|---|---|
| [`backend/`](backend/) | `M01`–`M13`, `M15` | API REST, domínio, motor de execução, motor de script (Java 21 + Spring Boot 4.1.x + Maven) |
| [`frontend/`](frontend/) | `M14`, `M07` | Aplicação Next.js (App Router) + TypeScript + `bpmn-js` + Monaco |
| [`database/`](database/) | apoio a `M01` | Migrations Flyway, seeds e documentação do schema PostgreSQL |
| [`infra/`](infra/) | apoio a `M01`/`M15` | `docker-compose`, Dockerfiles, variáveis de ambiente |
| [`docs/`](docs/) | Seção 3/14 do plano | ADRs, contratos de API exportados, notas de arquitetura |
| [`scripts/`](scripts/) | — | Utilitários de desenvolvimento |

Regra de dependência entre módulos (Seção 3.2 do plano): um módulo só depende de módulos abaixo
dele na ordem `core → iam → project → metamodel → datastore → bpmn → scripting → engine → task →
requirements → simulation → reporting`, verificada por ArchUnit.

## Stack

Next.js · TypeScript · React · bpmn-js · Monaco · Tailwind · shadcn/ui · TanStack Query ·
Java 21 · Spring Boot 4.1.x · Spring Data JPA · Spring Security + JWT · PostgreSQL 16 (JSONB) ·
Flyway · GraalVM JavaScript · springdoc-openapi · JUnit 5 · Testcontainers · Vitest · Playwright.

## Pré-requisitos

- **JDK 21** (Temurin) — build e execução do backend
- **Node.js 24** — build do frontend
- **Docker Desktop** — `docker compose` para Postgres + backend + frontend
- **Git**

## Como executar (ambiente de desenvolvimento)

```bash
# 1. Sobe tudo (Postgres + backend + frontend) via Docker
cd infra
cp .env.example .env
docker compose up --build

# 2. Ou rodar cada módulo isoladamente:
#    Backend
cd backend && ./mvnw spring-boot:run
#    Frontend
cd frontend && npm install && npm run dev
```

| Serviço | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend (API) | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| PostgreSQL | localhost:5432 |

## Autenticação (M02 / IAM)

Todos os endpoints sob `/api/v1` exigem `Authorization: Bearer <token>`, exceto
`/api/v1/auth/**`, o Swagger e `/actuator/health`. Obtenha um token com:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"usernameOrEmail":"admin","password":"admin12345"}'
```

O usuário `admin` (papel `ADMIN`) é semeado por `V2__iam.sql`. **Troque a senha fora de
desenvolvimento.**

## Plano de implementação

Ver Seção 10 do plano. Estado atual: **Bloco 1** — `M02` (IAM) concluído; `M03` (Projetos) e
shell do `M14` a seguir.
