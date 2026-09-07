package br.ufmg.plataforma.core.domain;

/** Recurso inexistente ou fora do escopo do usuário/projeto. Mapeia para HTTP 404. */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /** Ex.: {@code new ResourceNotFoundException("Project", id)} -&gt; "Project 42 não encontrado". */
    public ResourceNotFoundException(String resource, Object id) {
        super("%s %s não encontrado".formatted(resource, id));
    }
}
