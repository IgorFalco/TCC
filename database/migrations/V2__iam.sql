-- Bloco 1 / M02 — IAM (identidade e acesso)
-- Entidades E-01 users, E-02 roles, E-03 user_roles. Requisitos RF-01, RF-02, RF-03; RNF-05.

CREATE TABLE roles (
    id   uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(30)  NOT NULL UNIQUE,
    name varchar(100) NOT NULL
);

CREATE TABLE users (
    id            uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    username      varchar(60)  NOT NULL UNIQUE,
    email         varchar(180) NOT NULL UNIQUE,
    password_hash varchar(100) NOT NULL,
    active        boolean      NOT NULL DEFAULT true,
    created_at    timestamptz  NOT NULL,
    created_by    varchar(120) NOT NULL,
    updated_at    timestamptz  NOT NULL,
    updated_by    varchar(120) NOT NULL,
    version       bigint       NOT NULL DEFAULT 0
);

CREATE TABLE user_roles (
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id uuid NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);

-- Catálogo fixo de papéis globais (espelha br.ufmg.plataforma.iam.domain.RoleCode).
INSERT INTO roles (code, name) VALUES
    ('ADMIN',       'Administrador'),
    ('MODELER',     'Modelador'),
    ('DEVELOPER',   'Desenvolvedor'),
    ('MANAGER',     'Gestor'),
    ('PARTICIPANT', 'Participante');

-- Usuário administrador inicial.
--   login: admin / senha: admin12345  (BCrypt, força 10)
--   TROCAR a senha fora de ambiente de desenvolvimento.
-- O hash é travado pelo teste br.ufmg.plataforma.iam.PasswordSeedTest.
INSERT INTO users (username, email, password_hash, active, created_at, created_by, updated_at, updated_by)
VALUES ('admin', 'admin@plataforma.ufmg.br',
        '$2a$10$swLUqtisBuVi0VdrmIZiTue/cSDO2oVRlecBNhEmtHyNTFbpGNX5.',
        true, now(), 'system', now(), 'system');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'ADMIN'
WHERE u.username = 'admin';
