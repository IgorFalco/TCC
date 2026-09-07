package br.ufmg.plataforma.iam.api;

import br.ufmg.plataforma.iam.api.dto.RoleResponse;
import br.ufmg.plataforma.iam.application.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Papéis", description = "Catálogo fixo de papéis globais (RF-03)")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @Operation(summary = "Lista os papéis globais disponíveis")
    public List<RoleResponse> list() {
        return roleService.list().stream().map(RoleResponse::from).toList();
    }
}
