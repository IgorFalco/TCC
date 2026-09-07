package br.ufmg.plataforma.iam.application.model;

import br.ufmg.plataforma.iam.domain.User;

/**
 * Resultado de um login ou refresh: o par de tokens e o usuário autenticado.
 *
 * <p>Consumido pelo {@code AuthController} (mesmo módulo). O contorno cross-módulo do {@code IF-03}
 * é {@link CurrentUser}, devolvido por {@code AuthService.currentUser()}.
 */
public record AuthResult(String accessToken, String refreshToken, User user) {}
