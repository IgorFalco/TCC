-- Bloco 0 / M01 — Fundação do schema
-- Regra inviolável 7: nenhuma tabela de dados dinâmicos é criada em runtime (D-02).

-- Extensões usadas pelo projeto
CREATE EXTENSION IF NOT EXISTS "pgcrypto";      -- gen_random_uuid()

-- Espaço reservado para objetos comuns de auditoria (M01 / RNF-12).
-- As entidades de cada módulo entram em migrations próprias (V2+).

-- Exemplo de tabela de auditoria genérica (E-27) — pode ser movida para V<n>__core.sql
CREATE TABLE IF NOT EXISTS audit_logs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type  VARCHAR(120) NOT NULL,
    entity_id    VARCHAR(120) NOT NULL,
    action       VARCHAR(40)  NOT NULL,
    actor        VARCHAR(120),
    occurred_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    diff         JSONB
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_entity
    ON audit_logs (entity_type, entity_id);
