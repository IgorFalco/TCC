package br.ufmg.plataforma.iam.infrastructure;

import br.ufmg.plataforma.iam.domain.RoleCode;
import br.ufmg.plataforma.iam.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Emissão e verificação de tokens JWT (HS256) para o M02 (ver {@code D-12}).
 *
 * <p>Dois tipos de token, distinguidos pela claim {@code typ}: {@code access} (curto, usado no
 * cabeçalho {@code Authorization}) e {@code refresh} (longo, trocável em {@code /auth/refresh}).
 */
@Component
public class JwtService {

    /** Tipo do token, carregado na claim {@code typ}. */
    public enum TokenType {
        ACCESS,
        REFRESH
    }

    /** Conteúdo verificado de um token. */
    public record ParsedToken(UUID userId, String username, Set<RoleCode> roles, TokenType type) {}

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE = "typ";

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(JwtProperties props) {
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.issuer = props.issuer();
        this.accessTtl = Duration.ofMinutes(props.expirationMinutes());
        this.refreshTtl = Duration.ofMinutes(props.refreshExpirationMinutes());
    }

    public String issueAccessToken(User user) {
        return build(user, TokenType.ACCESS, accessTtl);
    }

    public String issueRefreshToken(User user) {
        return build(user, TokenType.REFRESH, refreshTtl);
    }

    private String build(User user, TokenType type, Duration ttl) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream().map(r -> r.getCode().name()).toList();
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TYPE, type.name().toLowerCase())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /**
     * Verifica assinatura, emissor e expiração e devolve o conteúdo.
     *
     * @throws JwtException se o token for inválido, expirado ou malformado
     */
    public ParsedToken parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID userId = UUID.fromString(claims.getSubject());
        String username = claims.get(CLAIM_USERNAME, String.class);
        TokenType type = "refresh".equals(claims.get(CLAIM_TYPE, String.class))
                ? TokenType.REFRESH
                : TokenType.ACCESS;

        Set<RoleCode> roles = new LinkedHashSet<>();
        Object raw = claims.get(CLAIM_ROLES);
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                try {
                    roles.add(RoleCode.valueOf(String.valueOf(item)));
                } catch (IllegalArgumentException ignored) {
                    // papel desconhecido no token — ignora
                }
            }
        }
        return new ParsedToken(userId, username, roles, type);
    }
}
