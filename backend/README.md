# Módulo: Backend

Java 21 · Spring Boot 4.1.x · Spring Data JPA · Spring Security · Flyway · PostgreSQL 16.

> O plano (Seção 1.4) previa Spring Boot 3.x. O Spring Initializr não gera mais projetos 3.x;
> adotou-se **Spring Boot 4.1.1** (suporta Java 21). Ver `docs/adr/0001`.

## Estrutura por módulo (Seção 3.2 do plano)

Cada módulo é um pacote de primeiro nível sob `br.ufmg.plataforma`, com as camadas:

```
<modulo>/
├── api/              controllers REST + DTOs + validação
├── application/      serviços de caso de uso, transações, orquestração
├── domain/           entidades JPA, regras de negócio, eventos de domínio
└── infrastructure/   repositórios, adaptadores, integrações
```

| Pacote | Módulo | Responsabilidade |
|---|---|---|
| `core` | M01 | config, erros, auditoria, tipos comuns, barramento de eventos |
| `iam` | M02 | usuários, papéis, autenticação JWT, autorização |
| `project` | M03 | projetos, membros, escopo de dados |
| `metamodel` | M04 | classes dinâmicas e atributos |
| `datastore` | M05 | registros das classes dinâmicas (CRUD genérico, JSONB) |
| `bpmn` | M06 | definições, versões, parser, elementos |
| `scripting` | M08 | runtime GraalJS, sandbox, API exposta ao script |
| `engine` | M09 | tokens, instâncias, handlers de elemento |
| `task` | M10 | tarefas humanas, atribuição, inbox |
| `requirements` | M11 | requisitos, vínculos, conformidade |
| `simulation` | M12 | execução em modo simulação, passo a passo |
| `reporting` | M13 | consultas consolidadas, matriz, exportação |
| `observability` | M15 | logs estruturados, auditoria, métricas (transversal) |

**Regra de dependência** (verificada por ArchUnit — a adicionar no Bloco 0): um módulo só
depende de módulos acima dele nesta lista, mais `core`.

## Migrations

O schema é governado pelo Flyway. Os scripts ficam em [`../database/migrations`](../database/migrations)
e são copiados para `classpath:db/migration` pelo build (ver `<resources>` no `pom.xml`).
`spring.jpa.hibernate.ddl-auto=none` — o Hibernate nunca altera o schema.

## Executar

```bash
# Requer JAVA_HOME apontando para o JDK 21 e um Postgres em localhost:5432
# (suba com:  cd ../infra && docker compose up db)
./mvnw spring-boot:run                 # perfil default
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

./mvnw test                            # testes (usa Testcontainers → precisa de Docker)
./mvnw clean package                   # gera target/plataforma-*.jar
```

JDK 21 instalado em: `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`
(a máquina também tem JDK 25; fixe `JAVA_HOME` para builds reprodutíveis).
