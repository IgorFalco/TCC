package br.ufmg.plataforma.core.web;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Envelope de resposta paginada da API (tipo comum, criado no M02).
 *
 * <p>Mantém o corpo de listagem independente do {@code org.springframework.data.domain.Page},
 * que não é um contrato estável para serialização.
 *
 * @param content       itens da página atual (já convertidos para DTO)
 * @param page          índice da página (base 0)
 * @param size          tamanho solicitado da página
 * @param totalElements total de registros que satisfazem a consulta
 * @param totalPages    total de páginas
 */
public record PageResponse<T>(
        List<T> content, int page, int size, long totalElements, int totalPages) {

    /** Converte um {@link Page} de entidades num {@code PageResponse} de DTOs. */
    public static <E, T> PageResponse<T> from(Page<E> source, Function<E, T> mapper) {
        return new PageResponse<>(
                source.getContent().stream().map(mapper).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages());
    }
}
