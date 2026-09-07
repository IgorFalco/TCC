package br.ufmg.plataforma.iam.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do JWT (prefixo {@code security.jwt}).
 *
 * @param secret                   segredo HMAC (mín. 32 bytes); sobrescreva por {@code JWT_SECRET}
 * @param expirationMinutes        validade do <em>access token</em>
 * @param refreshExpirationMinutes validade do <em>refresh token</em>
 * @param issuer                   valor da claim {@code iss}
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String secret,
        long expirationMinutes,
        long refreshExpirationMinutes,
        String issuer) {

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "security.jwt.secret deve ter ao menos 32 caracteres (defina JWT_SECRET)");
        }
        if (expirationMinutes <= 0) {
            expirationMinutes = 120;
        }
        if (refreshExpirationMinutes <= 0) {
            refreshExpirationMinutes = 10080;
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "plataforma-bpmn";
        }
    }
}
