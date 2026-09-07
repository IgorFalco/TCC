package br.ufmg.plataforma.iam.application;

import java.util.UUID;

/**
 * {@code IF-04} — porta de autorização por projeto, consumida por M03+ para decidir se o usuário
 * autenticado pode agir sobre um projeto.
 *
 * <p><strong>Implementação interina do M02</strong> (ver ADR 0002 / {@code D-13}): sem o M03 ainda
 * não existe {@code project_members}, então as decisões usam apenas os papéis globais e o
 * {@code projectId} é ignorado. O M03 refina com a associação por projeto sem alterar esta
 * assinatura.
 *
 * <p>Os métodos {@code assert*} lançam {@link org.springframework.security.access.AccessDeniedException}
 * (HTTP 403) quando a permissão é negada; os métodos {@code can*} apenas retornam o booleano.
 */
public interface AccessGuard {

    boolean canModel(UUID projectId);

    boolean canExecute(UUID projectId);

    boolean canAdminister(UUID projectId);

    void assertCanModel(UUID projectId);

    void assertCanExecute(UUID projectId);

    void assertCanAdminister(UUID projectId);
}
