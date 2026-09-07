package br.ufmg.plataforma.core.web;

import br.ufmg.plataforma.core.domain.BusinessRuleException;
import br.ufmg.plataforma.core.domain.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Tradução central de exceções para o contrato de erro HTTP da plataforma (IF-01),
 * no formato RFC 9457 ({@link ProblemDetail}).
 *
 * <p>Cada resposta carrega a propriedade extra {@code traceId} para correlação com os logs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ErrorType.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ProblemDetail> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ErrorType.BUSINESS_RULE, ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ErrorType.UNAUTHORIZED, "Autenticação necessária", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ErrorType.FORBIDDEN, "Acesso negado", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        String traceId = newTraceId();
        log.error("Erro não tratado [traceId={}] em {} {}", traceId, request.getMethod(), request.getRequestURI(), ex);
        ProblemDetail body = problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorType.INTERNAL,
                "Ocorreu um erro inesperado. Use o traceId para suporte.", request, traceId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        List<FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos");
        body.setType(ErrorType.VALIDATION.uri());
        body.setTitle(ErrorType.VALIDATION.title());
        body.setProperty("errors", errors);
        body.setProperty("traceId", newTraceId());
        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<ProblemDetail> build(
            HttpStatus status, ErrorType type, String detail, HttpServletRequest request) {
        return ResponseEntity.status(status).body(problem(status, type, detail, request, newTraceId()));
    }

    private ProblemDetail problem(
            HttpStatus status, ErrorType type, String detail, HttpServletRequest request, String traceId) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setType(type.uri());
        body.setTitle(type.title());
        body.setInstance(java.net.URI.create(request.getRequestURI()));
        body.setProperty("traceId", traceId);
        return body;
    }

    private static String newTraceId() {
        String fromMdc = org.slf4j.MDC.get("correlationId");
        return fromMdc != null ? fromMdc : UUID.randomUUID().toString();
    }

    /** Detalhe de erro de validação por campo. */
    public record FieldErrorDetail(String field, String message) {}
}
