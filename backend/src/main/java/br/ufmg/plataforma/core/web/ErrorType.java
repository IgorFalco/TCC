package br.ufmg.plataforma.core.web;

import java.net.URI;

/**
 * Catálogo dos valores de {@code type} do contrato de erro (IF-01, RFC 9457).
 * A URI é apenas um identificador estável — não precisa resolver para uma página.
 */
public enum ErrorType {
    VALIDATION("validation", "Requisição inválida"),
    NOT_FOUND("not-found", "Recurso não encontrado"),
    BUSINESS_RULE("business-rule", "Regra de negócio violada"),
    UNAUTHORIZED("unauthorized", "Não autenticado"),
    FORBIDDEN("forbidden", "Acesso negado"),
    INTERNAL("internal", "Erro interno");

    private static final String BASE = "https://plataforma.ufmg.br/errors/";

    private final String slug;
    private final String title;

    ErrorType(String slug, String title) {
        this.slug = slug;
        this.title = title;
    }

    public URI uri() {
        return URI.create(BASE + slug);
    }

    public String title() {
        return title;
    }
}
