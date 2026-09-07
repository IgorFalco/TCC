package br.ufmg.plataforma.iam.api.dto;

import jakarta.validation.constraints.NotBlank;

/** DTOs dos endpoints {@code /api/v1/auth/**}. */
public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank String usernameOrEmail,
            @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    /** Resposta de login/refresh (contrato da Seção 8.1: {@code { token, refreshToken, user }}). */
    public record AuthResponse(String token, String refreshToken, UserDtos.UserResponse user) {}
}
