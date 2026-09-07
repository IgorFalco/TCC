package br.ufmg.plataforma.core.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Implementação de {@link DomainEventPublisher} sobre o {@link ApplicationEventPublisher} do
 * Spring. Mantém os consumidores desacoplados do publicador e permite usar
 * {@code @TransactionalEventListener} / {@code @EventListener}.
 */
@Component
class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher delegate;

    SpringDomainEventPublisher(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(DomainEvent event) {
        delegate.publishEvent(event);
    }
}
