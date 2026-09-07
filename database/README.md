# Módulo: Banco de dados

PostgreSQL 16 com colunas `JSONB` para os dados dinâmicos (Seção 7 do plano).

## Organização

| Pasta | Conteúdo |
|---|---|
| `migrations/` | Scripts Flyway versionados (`V<n>__<descrição>.sql`). Fonte única do schema. |
| `seed/` | Dados de carga: papéis globais, e o cenário demonstrativo (Seção 12.3). |
| `docs/` | Diagrama conceitual, dicionário de dados, notas de indexação. |

## Como as migrations são aplicadas

O backend (Spring Boot + Flyway) executa as migrations no start. A propriedade
`spring.flyway.locations` aponta para estes arquivos:

- Em execução local do backend: `filesystem:../database/migrations`
- No container Docker: o diretório é copiado/montado em `/app/db/migration`

Regras invioláveis relacionadas (Seção 3.3 do plano):

- **7** — o modelo de dados dinâmico nunca cria/altera tabelas em runtime (`D-02`).
- Toda alteração de schema entra como **nova** migration numerada; migrations aplicadas nunca
  são editadas.

## Convenção de numeração

```
V1__baseline.sql              -- core: auditoria, extensões
V2__iam.sql                   -- M02: users, roles, user_roles
V3__project.sql               -- M03: projects, project_members
V4__metamodel.sql             -- M04: entity_classes, entity_attributes
V5__datastore.sql             -- M05: entity_records (+ índice GIN)
...
```
