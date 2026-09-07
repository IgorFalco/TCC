package br.ufmg.plataforma.iam.infrastructure;

import br.ufmg.plataforma.iam.domain.Role;
import br.ufmg.plataforma.iam.domain.RoleCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByCode(RoleCode code);
}
