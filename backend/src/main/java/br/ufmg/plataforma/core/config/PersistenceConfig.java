package br.ufmg.plataforma.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Habilita a auditoria do Spring Data JPA (preenche {@code createdBy}/{@code updatedBy} em
 * {@code AuditableEntity}).
 *
 * <p>O bean {@code AuditorAware<String>} de nome {@code auditorAware} é fornecido pelo M02 (IAM):
 * {@code br.ufmg.plataforma.iam.infrastructure.SecurityAuditorAware}, que resolve o usuário
 * autenticado no {@code SecurityContext} (ou {@code "system"} para operações sem usuário).
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
class PersistenceConfig {}
