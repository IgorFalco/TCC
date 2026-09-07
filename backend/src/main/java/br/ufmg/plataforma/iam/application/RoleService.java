package br.ufmg.plataforma.iam.application;

import br.ufmg.plataforma.core.domain.ResourceNotFoundException;
import br.ufmg.plataforma.iam.domain.Role;
import br.ufmg.plataforma.iam.domain.RoleCode;
import br.ufmg.plataforma.iam.infrastructure.RoleRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Leitura do catálogo fixo de papéis globais (RF-03). Papéis são semeados por {@code V2__iam.sql}
 * e não têm CRUD — o conjunto é fechado ({@link RoleCode}).
 */
@Service
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roles;

    public RoleService(RoleRepository roles) {
        this.roles = roles;
    }

    public List<Role> list() {
        return roles.findAll(Sort.by("code"));
    }

    /** Resolve os códigos para entidades {@link Role}, falhando se algum não existir. */
    public Set<Role> resolve(Set<RoleCode> codes) {
        Set<Role> resolved = new LinkedHashSet<>();
        for (RoleCode code : codes) {
            resolved.add(roles.findByCode(code)
                    .orElseThrow(() -> new ResourceNotFoundException("Papel", code)));
        }
        return resolved;
    }
}
