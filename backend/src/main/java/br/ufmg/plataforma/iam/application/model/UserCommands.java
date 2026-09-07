package br.ufmg.plataforma.iam.application.model;

import br.ufmg.plataforma.iam.domain.RoleCode;
import java.util.Set;

/** Comandos de aplicação do CRUD de usuário (entrada dos serviços, já validada na borda REST). */
public final class UserCommands {

    private UserCommands() {}

    /**
     * @param username login único
     * @param email    e-mail único
     * @param password senha em texto puro (será cifrada com BCrypt)
     * @param roles    papéis globais a atribuir (pode ser vazio)
     */
    public record CreateUser(String username, String email, String password, Set<RoleCode> roles) {}

    /** Edição de dados básicos do usuário. */
    public record UpdateUser(String username, String email) {}
}
