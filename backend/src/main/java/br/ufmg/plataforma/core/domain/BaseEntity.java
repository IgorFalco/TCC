package br.ufmg.plataforma.core.domain;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.proxy.HibernateProxy;

/**
 * Superclasse de toda entidade JPA da plataforma (M01).
 *
 * <p>Fornece a chave primária {@link UUID} gerada pela aplicação (Hibernate {@code @UuidGenerator}),
 * disponível antes do flush, e um {@code equals}/{@code hashCode} baseado no id que é seguro com
 * proxies do Hibernate.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @UuidGenerator
    private UUID id;

    public UUID getId() {
        return id;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> thisType = this instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : getClass();
        Class<?> otherType = o instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        if (thisType != otherType) {
            return false;
        }
        BaseEntity other = (BaseEntity) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}
