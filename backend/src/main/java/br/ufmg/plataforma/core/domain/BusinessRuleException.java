package br.ufmg.plataforma.core.domain;

/**
 * Violação de uma regra de negócio com o estado atual do recurso
 * (ex.: publicar processo sem evento de fim, concluir tarefa já concluída).
 * Mapeia para HTTP 409 (Conflict).
 */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
