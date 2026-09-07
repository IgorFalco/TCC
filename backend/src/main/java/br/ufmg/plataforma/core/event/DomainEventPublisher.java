package br.ufmg.plataforma.core.event;

/**
 * Barramento de eventos de domínio da plataforma (IF-02).
 *
 * <p>É o canal preferencial de comunicação entre módulos quando uma dependência direta violaria
 * a regra de dependência da Seção 3.2 do plano.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
