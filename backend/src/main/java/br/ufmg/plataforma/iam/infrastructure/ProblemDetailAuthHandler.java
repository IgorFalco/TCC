package br.ufmg.plataforma.iam.infrastructure;

import br.ufmg.plataforma.core.web.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Traduz falhas de segurança que acontecem <em>dentro da cadeia de filtros</em> (portanto fora do
 * alcance do {@code GlobalExceptionHandler}) para o mesmo contrato de erro RFC 9457 (IF-01).
 *
 * <ul>
 *   <li>sem autenticação → 401 {@code unauthorized}</li>
 *   <li>autenticado sem papel → 403 {@code forbidden}</li>
 * </ul>
 *
 * <p>O JSON é montado à mão (poucos campos, estrutura fixa) para não depender de um
 * {@code ObjectMapper} — este bean é criado muito cedo, junto da cadeia de segurança.
 */
@Component
public class ProblemDetailAuthHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED, ErrorType.UNAUTHORIZED, "Autenticação necessária");
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, ErrorType.FORBIDDEN, "Acesso negado");
    }

    private void write(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            ErrorType type,
            String detail)
            throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String json = "{"
                + "\"type\":\"" + escape(type.uri().toString()) + "\","
                + "\"title\":\"" + escape(type.title()) + "\","
                + "\"status\":" + status.value() + ","
                + "\"detail\":\"" + escape(detail) + "\","
                + "\"instance\":\"" + escape(request.getRequestURI()) + "\","
                + "\"traceId\":\"" + UUID.randomUUID() + "\""
                + "}";
        response.getWriter().write(json);
    }

    private static String escape(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
