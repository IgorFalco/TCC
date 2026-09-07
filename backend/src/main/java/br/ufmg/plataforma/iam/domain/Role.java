package br.ufmg.plataforma.iam.domain;

import br.ufmg.plataforma.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Papel global (E-02). Conjunto fechado e semeado — ver {@link RoleCode} e {@code V2__iam.sql}.
 */
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private RoleCode code;

    @Column(nullable = false, length = 100)
    private String name;

    protected Role() {
        // JPA
    }

    public Role(RoleCode code, String name) {
        this.code = code;
        this.name = name;
    }

    public RoleCode getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String authority() {
        return code.authority();
    }
}
