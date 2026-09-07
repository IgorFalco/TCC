package br.ufmg.plataforma.core.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração de CORS (prefixo {@code app.cors}).
 *
 * @param allowedOrigins origens permitidas a chamar {@code /api/**} (default: front local)
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = List.of("http://localhost:3000");
        }
    }
}
