package br.ufmg.plataforma.core.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Habilita a auditoria do Spring Data JPA (preenche {@code createdBy}/{@code updatedBy} em
 * {@code AuditableEntity}).
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
class PersistenceConfig {

    static final String SYSTEM = "system";

    /**
     * Autor da operação corrente. Até o M02 (IAM) popular o {@code SecurityContext}, retorna
     * {@code "system"}.
     */
    @Bean
    AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.of(SYSTEM);
            }
            return Optional.of(auth.getName());
        };
    }
}
