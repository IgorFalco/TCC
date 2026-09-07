package br.ufmg.plataforma.iam.infrastructure;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Autor das operações auditadas (RNF-12): o {@code username} do usuário autenticado, ou
 * {@code "system"} para operações sem usuário (bootstrap, migrations, jobs).
 *
 * <p>Substitui o {@code AuditorAware} provisório do M01. O nome do bean ({@code auditorAware})
 * casa com o {@code auditorAwareRef} de {@code core.config.PersistenceConfig}.
 */
@Component("auditorAware")
public class SecurityAuditorAware implements AuditorAware<String> {

    static final String SYSTEM = "system";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.of(SYSTEM);
        }
        return Optional.of(auth.getName());
    }
}
