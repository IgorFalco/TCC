package br.ufmg.plataforma.iam.application;

import br.ufmg.plataforma.iam.application.model.CurrentUser;
import br.ufmg.plataforma.iam.domain.RoleCode;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Implementação interina do {@code IF-04} (ver ADR 0002 / {@code D-13}).
 *
 * <p>Decide apenas por papel global; o {@code projectId} é aceito na assinatura mas ainda não
 * consultado — o M03 adiciona a associação por projeto ({@code project_members}).
 */
@Component
public class AccessGuardImpl implements AccessGuard {

    private static final Set<RoleCode> CAN_MODEL = Set.of(RoleCode.ADMIN, RoleCode.MODELER);
    private static final Set<RoleCode> CAN_EXECUTE =
            Set.of(RoleCode.ADMIN, RoleCode.MODELER, RoleCode.DEVELOPER, RoleCode.PARTICIPANT);
    private static final Set<RoleCode> CAN_ADMIN = Set.of(RoleCode.ADMIN);

    private final AuthService authService;

    public AccessGuardImpl(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean canModel(UUID projectId) {
        return hasAny(CAN_MODEL);
    }

    @Override
    public boolean canExecute(UUID projectId) {
        return hasAny(CAN_EXECUTE);
    }

    @Override
    public boolean canAdminister(UUID projectId) {
        return hasAny(CAN_ADMIN);
    }

    @Override
    public void assertCanModel(UUID projectId) {
        require(canModel(projectId), "modelar");
    }

    @Override
    public void assertCanExecute(UUID projectId) {
        require(canExecute(projectId), "executar");
    }

    @Override
    public void assertCanAdminister(UUID projectId) {
        require(canAdminister(projectId), "administrar");
    }

    private boolean hasAny(Set<RoleCode> allowed) {
        CurrentUser user = authService.currentUser();
        return user.roles().stream().anyMatch(allowed::contains);
    }

    private void require(boolean allowed, String action) {
        if (!allowed) {
            throw new AccessDeniedException("Sem permissão para " + action + " neste projeto");
        }
    }
}
