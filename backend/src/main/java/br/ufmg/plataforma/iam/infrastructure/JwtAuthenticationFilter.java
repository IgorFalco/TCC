package br.ufmg.plataforma.iam.infrastructure;

import br.ufmg.plataforma.iam.domain.User;
import br.ufmg.plataforma.iam.infrastructure.JwtService.ParsedToken;
import br.ufmg.plataforma.iam.infrastructure.JwtService.TokenType;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica a requisição a partir do <em>access token</em> em {@code Authorization: Bearer ...}.
 *
 * <p>Token ausente ou inválido: a cadeia segue sem autenticação e as regras de autorização
 * disparam o {@code AuthenticationEntryPoint} (401). Token válido: recarrega o usuário
 * ({@code active = true}) e popula o {@code SecurityContext} com as authorities atuais.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwt;
    private final UserRepository users;

    public JwtAuthenticationFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = bearerToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(token, request);
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        ParsedToken parsed;
        try {
            parsed = jwt.parse(token);
        } catch (JwtException | IllegalArgumentException ex) {
            return;
        }
        if (parsed.type() != TokenType.ACCESS) {
            return;
        }
        User user = users.findByIdAndActiveTrue(parsed.userId()).orElse(null);
        if (user == null) {
            return;
        }
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.authority()))
                .toList();
        var authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(user.getId(), user.getUsername()), null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static String bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
