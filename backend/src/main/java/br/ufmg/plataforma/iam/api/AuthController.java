package br.ufmg.plataforma.iam.api;

import br.ufmg.plataforma.iam.api.dto.AuthDtos.AuthResponse;
import br.ufmg.plataforma.iam.api.dto.AuthDtos.LoginRequest;
import br.ufmg.plataforma.iam.api.dto.AuthDtos.RefreshRequest;
import br.ufmg.plataforma.iam.api.dto.UserDtos.UserResponse;
import br.ufmg.plataforma.iam.application.AuthService;
import br.ufmg.plataforma.iam.application.model.AuthResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Login e renovação de token (RF-01)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica por login/e-mail e senha e emite o par de tokens JWT")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return toResponse(authService.authenticate(request.usernameOrEmail(), request.password()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Troca um refresh token válido por um novo par de tokens")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return toResponse(authService.refresh(request.refreshToken()));
    }

    private static AuthResponse toResponse(AuthResult result) {
        return new AuthResponse(
                result.accessToken(), result.refreshToken(), UserResponse.from(result.user()));
    }
}
