package br.ufmg.plataforma.iam.domain;

/**
 * Catálogo fixo de papéis globais da plataforma (Seção 6.3 do plano geral).
 *
 * <p>Cada valor corresponde a uma linha semeada em {@code roles} (migration {@code V2__iam.sql})
 * e a uma <em>authority</em> {@code ROLE_<code>} no Spring Security.
 */
public enum RoleCode {

    /** Administra a plataforma: usuários, papéis e configuração. */
    ADMIN,
    /** Cria e edita modelos BPMN. */
    MODELER,
    /** Escreve o comportamento programável (scripts) dos processos. */
    DEVELOPER,
    /** Acompanha e aprova; visão gerencial. */
    MANAGER,
    /** Participa da execução: recebe e conclui tarefas. */
    PARTICIPANT;

    /** Nome da authority correspondente no Spring Security ({@code ROLE_ADMIN}, ...). */
    public String authority() {
        return "ROLE_" + name();
    }
}
