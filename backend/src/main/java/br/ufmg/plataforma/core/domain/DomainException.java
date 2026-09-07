package br.ufmg.plataforma.core.domain;

/**
 * Base de todas as exceções de regra de negócio da plataforma.
 *
 * <p>Cada subclasse mapeia para um status HTTP no {@code GlobalExceptionHandler} (IF-01).
 * Não use para erros técnicos/infraestrutura — deixe esses propagarem para o fallback 500.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
