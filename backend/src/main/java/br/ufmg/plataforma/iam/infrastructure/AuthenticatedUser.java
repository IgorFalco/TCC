package br.ufmg.plataforma.iam.infrastructure;

import java.util.UUID;
import org.springframework.security.core.AuthenticatedPrincipal;

/**
 * Principal colocado no {@code SecurityContext} pelo {@link JwtAuthenticationFilter}.
 *
 * <p>Implementa {@link AuthenticatedPrincipal} para que {@code Authentication.getName()} devolva o
 * {@code username} — é isso que o {@link SecurityAuditorAware} grava em {@code createdBy}/{@code updatedBy}.
 */
public record AuthenticatedUser(UUID id, String username) implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return username;
    }
}
