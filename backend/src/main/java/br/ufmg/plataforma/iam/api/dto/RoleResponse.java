package br.ufmg.plataforma.iam.api.dto;

import br.ufmg.plataforma.iam.domain.Role;
import java.util.UUID;

/** Representação de leitura de um papel global. */
public record RoleResponse(UUID id, String code, String name) {

    public static RoleResponse from(Role role) {
        return new RoleResponse(role.getId(), role.getCode().name(), role.getName());
    }
}
