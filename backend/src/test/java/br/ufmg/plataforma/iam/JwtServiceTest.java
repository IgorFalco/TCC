package br.ufmg.plataforma.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import br.ufmg.plataforma.iam.domain.Role;
import br.ufmg.plataforma.iam.domain.RoleCode;
import br.ufmg.plataforma.iam.domain.User;
import br.ufmg.plataforma.iam.infrastructure.JwtProperties;
import br.ufmg.plataforma.iam.infrastructure.JwtService;
import br.ufmg.plataforma.iam.infrastructure.JwtService.TokenType;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "segredo-de-teste-com-mais-de-32-caracteres-aqui";
    private final JwtProperties props = new JwtProperties(SECRET, 120, 10080, "plataforma-bpmn");
    private final JwtService jwt = new JwtService(props);

    private User userWithRole(RoleCode code) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(UUID.randomUUID());
        lenient().when(user.getUsername()).thenReturn("alice");
        lenient().when(user.getRoles()).thenReturn(Set.of(new Role(code, code.name())));
        return user;
    }

    @Test
    void access_token_faz_round_trip_das_claims() {
        User user = userWithRole(RoleCode.MODELER);
        var parsed = jwt.parse(jwt.issueAccessToken(user));

        assertThat(parsed.userId()).isEqualTo(user.getId());
        assertThat(parsed.username()).isEqualTo("alice");
        assertThat(parsed.roles()).containsExactly(RoleCode.MODELER);
        assertThat(parsed.type()).isEqualTo(TokenType.ACCESS);
    }

    @Test
    void refresh_token_carrega_o_tipo_refresh() {
        var parsed = jwt.parse(jwt.issueRefreshToken(userWithRole(RoleCode.ADMIN)));
        assertThat(parsed.type()).isEqualTo(TokenType.REFRESH);
    }

    @Test
    void token_assinado_com_outra_chave_e_rejeitado() {
        String foreign = Jwts.builder()
                .issuer("plataforma-bpmn")
                .subject(UUID.randomUUID().toString())
                .signWith(Keys.hmacShaKeyFor(
                        "outra-chave-completamente-diferente-de-32+".getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwt.parse(foreign)).isInstanceOf(JwtException.class);
    }

    @Test
    void token_expirado_e_rejeitado() {
        Instant past = Instant.now().minusSeconds(3600);
        String expired = Jwts.builder()
                .issuer("plataforma-bpmn")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Date.from(past))
                .expiration(Date.from(past.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwt.parse(expired)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void props_rejeita_segredo_curto() {
        assertThatThrownBy(() -> new JwtProperties("curto", 120, 10080, "x"))
                .isInstanceOf(IllegalStateException.class);
    }
}
