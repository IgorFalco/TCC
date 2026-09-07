package br.ufmg.plataforma.iam.application;

import br.ufmg.plataforma.iam.application.model.AuthResult;
import br.ufmg.plataforma.iam.application.model.CurrentUser;

/**
 * {@code IF-03} — serviço de autenticação do módulo IAM.
 *
 * <p>É a interface pelo qual os demais módulos resolvem o usuário autenticado
 * ({@link #currentUser()}). A emissão de tokens ({@link #authenticate} / {@link #refresh}) é usada
 * apenas pelo próprio módulo (endpoints {@code /api/v1/auth/**}).
 */
public interface AuthService {

    /**
     * Valida credenciais e emite o par de tokens.
     *
     * @param usernameOrEmail login ou e-mail
     * @param rawPassword     senha em texto puro
     * @return tokens + usuário autenticado
     * @throws org.springframework.security.authentication.BadCredentialsException se as credenciais
     *     forem inválidas ou o usuário estiver inativo (mapeado para HTTP 401)
     */
    AuthResult authenticate(String usernameOrEmail, String rawPassword);

    /**
     * Troca um <em>refresh token</em> válido por um novo par de tokens (rotação).
     *
     * @throws org.springframework.security.authentication.BadCredentialsException se o token for
     *     inválido, expirado, do tipo errado, ou o usuário estiver inativo
     */
    AuthResult refresh(String refreshToken);

    /**
     * Usuário autenticado na requisição corrente.
     *
     * @throws org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
     *     se não houver usuário autenticado no contexto
     */
    CurrentUser currentUser();
}
