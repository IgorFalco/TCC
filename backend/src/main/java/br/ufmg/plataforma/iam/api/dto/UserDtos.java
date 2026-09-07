package br.ufmg.plataforma.iam.api.dto;

import br.ufmg.plataforma.iam.domain.RoleCode;
import br.ufmg.plataforma.iam.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** DTOs dos endpoints {@code /api/v1/users/**}. */
public final class UserDtos {

    private UserDtos() {}

    public record CreateUserRequest(
            @NotBlank @Size(max = 60) String username,
            @NotBlank @Email @Size(max = 180) String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            Set<RoleCode> roles) {

        public Set<RoleCode> rolesOrEmpty() {
            return roles == null ? Set.of() : roles;
        }
    }

    public record UpdateUserRequest(
            @NotBlank @Size(max = 60) String username,
            @NotBlank @Email @Size(max = 180) String email) {}

    public record SetActiveRequest(@NotNull Boolean active) {}

    public record AssignRolesRequest(@NotNull Set<RoleCode> roles) {}

    /** Representação de leitura de um usuário. */
    public record UserResponse(
            UUID id,
            String username,
            String email,
            boolean active,
            List<String> roles,
            Instant createdAt,
            String createdBy,
            Instant updatedAt,
            String updatedBy) {

        public static UserResponse from(User user) {
            List<String> roleCodes = user.getRoles().stream()
                    .map(r -> r.getCode().name())
                    .sorted(Comparator.naturalOrder())
                    .toList();
            return new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.isActive(),
                    roleCodes,
                    user.getCreatedAt(),
                    user.getCreatedBy(),
                    user.getUpdatedAt(),
                    user.getUpdatedBy());
        }
    }
}
