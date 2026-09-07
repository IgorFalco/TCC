package br.ufmg.plataforma.iam.application;

import br.ufmg.plataforma.iam.application.model.AuthResult;
import br.ufmg.plataforma.iam.application.model.CurrentUser;
import br.ufmg.plataforma.iam.domain.RoleCode;
import br.ufmg.plataforma.iam.domain.User;
import br.ufmg.plataforma.iam.infrastructure.AuthenticatedUser;
import br.ufmg.plataforma.iam.infrastructure.JwtService;
import br.ufmg.plataforma.iam.infrastructure.JwtService.ParsedToken;
import br.ufmg.plataforma.iam.infrastructure.JwtService.TokenType;
import br.ufmg.plataforma.iam.infrastructure.UserRepository;
import io.jsonwebtoken.JwtException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final String INVALID = "Credenciais inválidas";

    private final UserRepository users;
    private final JwtService jwt;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository users, JwtService jwt, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.jwt = jwt;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AuthResult authenticate(String usernameOrEmail, String rawPassword) {
        User user = users.findByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> new BadCredentialsException(INVALID));
        if (!user.isActive() || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException(INVALID);
        }
        return issue(user);
    }

    @Override
    public AuthResult refresh(String refreshToken) {
        ParsedToken parsed;
        try {
            parsed = jwt.parse(refreshToken);
        } catch (JwtException ex) {
            throw new BadCredentialsException("Refresh token inválido");
        }
        if (parsed.type() != TokenType.REFRESH) {
            throw new BadCredentialsException("Token não é um refresh token");
        }
        User user = users.findByIdAndActiveTrue(parsed.userId())
                .orElseThrow(() -> new BadCredentialsException(INVALID));
        return issue(user);
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new NotAuthenticated();
        }
        Set<RoleCode> roles = new LinkedHashSet<>();
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String name = authority.getAuthority();
            if (name.startsWith("ROLE_")) {
                try {
                    roles.add(RoleCode.valueOf(name.substring("ROLE_".length())));
                } catch (IllegalArgumentException ignored) {
                    // authority que não é papel de domínio
                }
            }
        }
        return new CurrentUser(principal.id(), principal.username(), roles);
    }

    private AuthResult issue(User user) {
        return new AuthResult(jwt.issueAccessToken(user), jwt.issueRefreshToken(user), user);
    }

    /** Sem usuário autenticado no contexto (HTTP 401 via {@code GlobalExceptionHandler}). */
    static final class NotAuthenticated extends AuthenticationException {
        NotAuthenticated() {
            super("Nenhum usuário autenticado no contexto");
        }
    }
}
