-- Bloco 0 / M01 — Fundação do schema
-- Regra inviolável 7: nenhuma tabela de dados dinâmicos é criada em runtime (D-02).

-- Extensões usadas pelo projeto
CREATE EXTENSION IF NOT EXISTS "pgcrypto";      -- gen_random_uuid()

-- As tabelas de cada módulo entram em migrations próprias (V2+):
--   V2__iam.sql          (M02)  users, roles, user_roles
--   V3__project.sql      (M03)  projects, project_members
--   ...
-- A auditoria detalhada (tabela audit_logs, E-27) entra com o módulo M15.
