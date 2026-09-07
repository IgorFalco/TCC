# ADR 0002 — IAM: JWT, endpoints públicos e AccessGuard interino

- **Data:** 2026-09-07
- **Status:** aceito (Bloco 1 / `M02`)
- **Contexto:** implementação do módulo `M02` (identidade e acesso). Complementa a Seção 14 do
  [plano geral](../../geral_porject_plan.md) com as decisões `D-12` e `D-13`.

## D-12 — JWT via jjwt, HS256, refresh stateless

**Decisão.** A autenticação usa **JSON Web Tokens** assinados com **HS256** e um segredo
simétrico (`security.jwt.secret`, sobrescrito por `JWT_SECRET`). A biblioteca é
**`io.jsonwebtoken:jjwt`** (`jjwt-api` + `jjwt-impl` + `jjwt-jackson`, 0.12.x).

- Dois tipos de token, distinguidos pela claim `typ`: `access` (validade
  `security.jwt.expiration-minutes`, padrão 120) e `refresh` (validade
  `security.jwt.refresh-expiration-minutes`, padrão 7 dias).
- `POST /api/v1/auth/login` devolve o par; `POST /api/v1/auth/refresh` troca um refresh válido
  por um **novo par** (rotação).
- O `JwtAuthenticationFilter` (um `OncePerRequestFilter`) valida assinatura, emissor e expiração,
  **recarrega o usuário** (`active = true`) a cada requisição e popula o `SecurityContext` com as
  authorities atuais (`ROLE_<code>`). Assim, desativar um usuário tem efeito imediato.

**Alternativas consideradas.**
- *Spring Security OAuth2 Resource Server + Nimbus.* Mais "Spring nativo", mas exige mais
  configuração e Nimbus direto para emitir o token no login; ganho pequeno nesta escala.
- *Sessão de servidor.* Contraria o baseline stateless e o front desacoplado.

**Consequências aceitas.**
- **Sem revogação de token** (nem blacklist, nem `jti`): um access token roubado vale até
  expirar. Mitigação: validade curta do access, recarga do usuário no filtro (pega desativação),
  e o refresh pode ser trocado/rotacionado. Uma lista de revogação entra se/quando necessário.
- `jjwt-jackson` traz Jackson 2 ao classpath ao lado do Jackson 3 do Spring Boot 4; os dois
  coexistem (Jackson 2 fica restrito ao uso interno da jjwt).

## D-13 — AccessGuard (IF-04) com implementação interina por papel global

**Decisão.** A interface `IF-04 AccessGuard`
(`canModel/canExecute/canAdminister(UUID projectId)` + variantes `assert*`) é entregue já no
`M02`, mas a implementação **decide apenas por papel global** e **ignora o `projectId`**:

| Método | Papéis globais que satisfazem |
|---|---|
| `canAdminister` | `ADMIN` |
| `canModel` | `ADMIN`, `MODELER` |
| `canExecute` | `ADMIN`, `MODELER`, `DEVELOPER`, `PARTICIPANT` |

**Motivo.** A associação usuário–projeto (`project_members`, `E-05`) pertence ao `M03`. Expor a
assinatura definitiva agora permite que M03+ programem contra `IF-04` sem retrabalho; o `M03`
substitui a lógica interna para consultar a membresia e o papel no projeto, sem alterar a
assinatura nem os chamadores.

## Endpoints públicos (whitelist do SecurityFilterChain)

`RNF-05` exige autenticação em todo endpoint. As exceções, e o porquê:

| Caminho | Motivo |
|---|---|
| `/api/v1/auth/**` | é onde se obtém o token |
| `/actuator/health`, `/actuator/info` | sondagem de disponibilidade (health já era público no `M01`) |
| `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | documentação da API em desenvolvimento |

Falhas de segurança dentro da cadeia de filtros (sem token → 401; sem papel → 403) são
serializadas pelo `ProblemDetailAuthHandler` no **mesmo formato RFC 9457** do `IF-01`
(`type`/`title`/`status`/`detail`/`instance`/`traceId`).

## Outras decisões táticas (sem número de ADR)

- **Papéis globais = catálogo fixo.** As 5 linhas de `roles` são semeadas por `V2__iam.sql` e
  espelham o enum `RoleCode`. Há apenas `GET /api/v1/roles` (leitura); não há CRUD de papéis —
  as authorities dependem de o conjunto ser conhecido em código.
- **Usuário `admin` semeado.** `admin` / `admin12345` (hash BCrypt força 10), papel `ADMIN`. O
  hash é travado pelo teste `PasswordSeedTest`. Trocar a senha fora de desenvolvimento.
- **`PageResponse<T>`** criado em `core.web` (tipo comum; o plano atribuiu sua criação ao `M02`).
