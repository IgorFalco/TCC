package br.ufmg.plataforma.core.event;

import java.time.Instant;

/**
 * Marcador de um evento de domínio (IF-02).
 *
 * <p>Implementações devem ser imutáveis e carregar apenas identificadores e dados primitivos —
 * nunca entidades JPA. Publique via {@link DomainEventPublisher}; consuma com
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} para efeitos colaterais ou
 * {@code @EventListener} para lógica na mesma transação.
 */
public interface DomainEvent {

    /** Momento em que o fato ocorreu. */
    Instant occurredOn();
}
