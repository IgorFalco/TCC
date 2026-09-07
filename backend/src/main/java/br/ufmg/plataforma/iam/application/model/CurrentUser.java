package br.ufmg.plataforma.iam.application.model;

import br.ufmg.plataforma.iam.domain.RoleCode;
import java.util.Set;
import java.util.UUID;

/**
 * Identidade do usuário autenticado na requisição corrente — tipo público do {@code IF-03}.
 *
 * <p>Não é uma entidade JPA: é o que os demais módulos recebem de
 * {@code AuthService.currentUser()}.
 *
 * @param id       identificador do usuário
 * @param username login do usuário
 * @param roles    papéis globais efetivos
 */
public record CurrentUser(UUID id, String username, Set<RoleCode> roles) {

    public boolean hasRole(RoleCode role) {
        return roles.contains(role);
    }
}
