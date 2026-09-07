package br.ufmg.plataforma.iam.application;

import br.ufmg.plataforma.core.domain.BusinessRuleException;
import br.ufmg.plataforma.core.domain.ResourceNotFoundException;
import br.ufmg.plataforma.iam.application.model.UserCommands.CreateUser;
import br.ufmg.plataforma.iam.application.model.UserCommands.UpdateUser;
import br.ufmg.plataforma.iam.domain.RoleCode;
import br.ufmg.plataforma.iam.domain.User;
import br.ufmg.plataforma.iam.infrastructure.UserRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD de usuário e atribuição de papéis (RF-02). Somente {@code ADMIN} chega aqui — a checagem de
 * papel é feita nos controllers com {@code @PreAuthorize}.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository users;
    private final RoleService roles;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, RoleService roles, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<User> list(Pageable pageable) {
        return users.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public User get(UUID id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
    }

    public User create(CreateUser cmd) {
        if (users.existsByUsername(cmd.username())) {
            throw new BusinessRuleException("Já existe um usuário com o login '%s'".formatted(cmd.username()));
        }
        if (users.existsByEmail(cmd.email())) {
            throw new BusinessRuleException("Já existe um usuário com o e-mail '%s'".formatted(cmd.email()));
        }
        User user = new User(cmd.username(), cmd.email(), passwordEncoder.encode(cmd.password()));
        user.replaceRoles(roles.resolve(cmd.roles()));
        return users.save(user);
    }

    public User update(UUID id, UpdateUser cmd) {
        User user = get(id);
        if (users.existsByUsernameAndIdNot(cmd.username(), id)) {
            throw new BusinessRuleException("Já existe um usuário com o login '%s'".formatted(cmd.username()));
        }
        if (users.existsByEmailAndIdNot(cmd.email(), id)) {
            throw new BusinessRuleException("Já existe um usuário com o e-mail '%s'".formatted(cmd.email()));
        }
        user.setUsername(cmd.username());
        user.setEmail(cmd.email());
        return user;
    }

    public User setActive(UUID id, boolean active) {
        User user = get(id);
        user.setActive(active);
        return user;
    }

    public User assignRoles(UUID id, Set<RoleCode> codes) {
        User user = get(id);
        user.replaceRoles(roles.resolve(codes));
        return user;
    }
}
