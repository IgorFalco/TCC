package br.ufmg.plataforma.iam.api;

import br.ufmg.plataforma.core.web.PageResponse;
import br.ufmg.plataforma.iam.api.dto.UserDtos.AssignRolesRequest;
import br.ufmg.plataforma.iam.api.dto.UserDtos.CreateUserRequest;
import br.ufmg.plataforma.iam.api.dto.UserDtos.SetActiveRequest;
import br.ufmg.plataforma.iam.api.dto.UserDtos.UpdateUserRequest;
import br.ufmg.plataforma.iam.api.dto.UserDtos.UserResponse;
import br.ufmg.plataforma.iam.application.AuthService;
import br.ufmg.plataforma.iam.application.UserService;
import br.ufmg.plataforma.iam.application.model.UserCommands.CreateUser;
import br.ufmg.plataforma.iam.application.model.UserCommands.UpdateUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuários", description = "Cadastro, edição e atribuição de papéis (RF-02, RF-03)")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário autenticado")
    public UserResponse me() {
        return UserResponse.from(userService.get(authService.currentUser().id()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista paginada de usuários")
    public PageResponse<UserResponse> list(Pageable pageable) {
        return PageResponse.from(userService.list(pageable), UserResponse::from);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cria um usuário")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        var user = userService.create(new CreateUser(
                request.username(), request.email(), request.password(), request.rolesOrEmpty()));
        var body = UserResponse.from(user);
        return ResponseEntity.created(URI.create("/api/v1/users/" + body.id())).body(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Edita login e e-mail de um usuário")
    public UserResponse update(
            @PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(
                userService.update(id, new UpdateUser(request.username(), request.email())));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ativa ou desativa um usuário")
    public UserResponse setActive(
            @PathVariable UUID id, @Valid @RequestBody SetActiveRequest request) {
        return UserResponse.from(userService.setActive(id, request.active()));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Substitui o conjunto de papéis globais de um usuário")
    public UserResponse assignRoles(
            @PathVariable UUID id, @Valid @RequestBody AssignRolesRequest request) {
        return UserResponse.from(userService.assignRoles(id, request.roles()));
    }
}
