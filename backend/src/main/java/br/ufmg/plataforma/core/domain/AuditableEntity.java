package br.ufmg.plataforma.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Entidade com auditoria de criação/alteração (RNF-12).
 *
 * <p>Os campos {@code createdBy}/{@code updatedBy} são preenchidos pelo
 * {@code AuditorAware} configurado em {@code core.config} — {@code "system"} até o M02
 * popular o {@code SecurityContext} com o usuário autenticado.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity extends BaseEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(nullable = false, updatable = false, length = 120)
    private String createdBy;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @LastModifiedBy
    @Column(nullable = false, length = 120)
    private String updatedBy;

    @Version
    private long version;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public long getVersion() {
        return version;
    }
}
