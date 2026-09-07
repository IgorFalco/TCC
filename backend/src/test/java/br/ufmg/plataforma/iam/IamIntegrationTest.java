package br.ufmg.plataforma.iam;

import static org.assertj.core.api.Assertions.assertThat;

import br.ufmg.plataforma.TestcontainersConfiguration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * TV-01 — autenticação, papéis e acesso negado (RF-01, RF-02, RF-03; RNF-05).
 *
 * <p>Exerce a API real por HTTP contra Postgres (Testcontainers). O usuário {@code admin} vem
 * semeado por {@code V2__iam.sql}. As respostas são pequenas e planas; os campos são extraídos
 * por regex para não depender de um mapeador JSON.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class IamIntegrationTest {

    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = "admin12345";

    @LocalServerPort
    int port;

    private RestClient rest;

    private RestClient client() {
        if (rest == null) {
            rest = RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .defaultStatusHandler(status -> true, (req, res) -> {})
                    .build();
        }
        return rest;
    }

    // --- caminho feliz -------------------------------------------------------

    @Test
    void login_admin_retorna_token_e_usuario() {
        ResponseEntity<String> resp = login(ADMIN, ADMIN_PASSWORD);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(field(resp.getBody(), "token")).isNotBlank();
        assertThat(field(resp.getBody(), "refreshToken")).isNotBlank();
        assertThat(field(resp.getBody(), "username")).isEqualTo(ADMIN);
        assertThat(resp.getBody()).contains("\"ADMIN\"");
    }

    @Test
    void me_retorna_o_usuario_do_token() {
        String token = accessToken(ADMIN, ADMIN_PASSWORD);
        ResponseEntity<String> resp = client().get().uri("/api/v1/users/me")
                .header("Authorization", "Bearer " + token)
                .retrieve().toEntity(String.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(field(resp.getBody(), "username")).isEqualTo(ADMIN);
    }

    @Test
    void admin_cria_usuario_que_passa_a_logar_e_a_auditoria_registra_o_autor() {
        String adminToken = accessToken(ADMIN, ADMIN_PASSWORD);
        String username = "modeler-" + suffix();

        ResponseEntity<String> created = client().post().uri("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "username", username,
                        "email", username + "@ex.com",
                        "password", "senha-forte-123",
                        "roles", List.of("MODELER")))
                .retrieve().toEntity(String.class);

        assertThat(created.getStatusCode().value()).isEqualTo(201);
        assertThat(created.getBody()).contains("\"MODELER\"");
        assertThat(field(created.getBody(), "createdBy")).isEqualTo(ADMIN);

        assertThat(accessToken(username, "senha-forte-123")).isNotBlank();
    }

    @Test
    void refresh_troca_o_token_por_um_novo_par() {
        String refresh = field(login(ADMIN, ADMIN_PASSWORD).getBody(), "refreshToken");

        ResponseEntity<String> resp = client().post().uri("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("refreshToken", refresh))
                .retrieve().toEntity(String.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(field(resp.getBody(), "token")).isNotBlank();
    }

    @Test
    void desativar_usuario_impede_novo_login() {
        String adminToken = accessToken(ADMIN, ADMIN_PASSWORD);
        String username = "temp-" + suffix();
        String userId = field(createUser(adminToken, username, "PARTICIPANT").getBody(), "id");

        assertThat(accessToken(username, "senha-forte-123")).isNotBlank();

        ResponseEntity<String> deactivate = client().patch().uri("/api/v1/users/" + userId + "/active")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("active", false))
                .retrieve().toEntity(String.class);
        assertThat(deactivate.getStatusCode().value()).isEqualTo(200);

        assertThat(login(username, "senha-forte-123").getStatusCode().value()).isEqualTo(401);
    }

    // --- acesso negado (RNF-05) -------------------------------------------

    @Test
    void endpoint_protegido_sem_token_retorna_401_problem_detail() {
        ResponseEntity<String> resp =
                client().get().uri("/api/v1/users").retrieve().toEntity(String.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(401);
        assertThat(resp.getHeaders().getContentType().toString())
                .startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(field(resp.getBody(), "type"))
                .isEqualTo("https://plataforma.ufmg.br/errors/unauthorized");
    }

    @Test
    void usuario_sem_papel_admin_recebe_403() {
        String adminToken = accessToken(ADMIN, ADMIN_PASSWORD);
        String username = "viewer-" + suffix();
        createUser(adminToken, username, "PARTICIPANT");

        String userToken = accessToken(username, "senha-forte-123");
        ResponseEntity<String> resp = client().get().uri("/api/v1/users")
                .header("Authorization", "Bearer " + userToken)
                .retrieve().toEntity(String.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(403);
        assertThat(field(resp.getBody(), "type"))
                .isEqualTo("https://plataforma.ufmg.br/errors/forbidden");
    }

    @Test
    void login_com_senha_errada_retorna_401() {
        assertThat(login(ADMIN, "senha-errada").getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void catalogo_de_papeis_exige_autenticacao_mas_nao_papel_especifico() {
        assertThat(client().get().uri("/api/v1/roles").retrieve().toBodilessEntity()
                .getStatusCode().value()).isEqualTo(401);

        ResponseEntity<String> resp = client().get().uri("/api/v1/roles")
                .header("Authorization", "Bearer " + accessToken(ADMIN, ADMIN_PASSWORD))
                .retrieve().toEntity(String.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).contains("ADMIN", "MODELER", "DEVELOPER", "MANAGER", "PARTICIPANT");
    }

    // --- helpers ----------------------------------------------------------

    private ResponseEntity<String> login(String username, String password) {
        return client().post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usernameOrEmail", username, "password", password))
                .retrieve().toEntity(String.class);
    }

    private ResponseEntity<String> createUser(String adminToken, String username, String role) {
        return client().post().uri("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", username, "email", username + "@ex.com",
                        "password", "senha-forte-123", "roles", List.of(role)))
                .retrieve().toEntity(String.class);
    }

    private String accessToken(String username, String password) {
        ResponseEntity<String> resp = login(username, password);
        assertThat(resp.getStatusCode().value()).as("login de %s", username).isEqualTo(200);
        return field(resp.getBody(), "token");
    }

    /** Extrai o valor string de {@code "name":"..."} do JSON plano da resposta. */
    private static String field(String json, String name) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(json);
        assertThat(m.find()).as("campo '%s' presente em %s", name, json).isTrue();
        return m.group(1);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
