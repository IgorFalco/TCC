package br.ufmg.plataforma.core;

import static org.assertj.core.api.Assertions.assertThat;

import br.ufmg.plataforma.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

/**
 * Smoke test da fundação (M01 / Bloco 0): o contexto sobe, o Flyway aplicou o baseline,
 * o Actuator responde e o contrato OpenAPI é servido. Precisa de Docker (Testcontainers).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class CoreSmokeTest {

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    @Test
    void flyway_aplicou_o_baseline_v1() {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success", Integer.class);
        assertThat(count).isEqualTo(1);

        Boolean pgcrypto = jdbc.queryForObject(
                "SELECT exists(SELECT 1 FROM pg_extension WHERE extname = 'pgcrypto')", Boolean.class);
        assertThat(pgcrypto).isTrue();
    }

    @Test
    void actuator_health_esta_up_sem_autenticacao() {
        String body = client().get().uri("/actuator/health").retrieve().body(String.class);
        assertThat(body).contains("\"status\":\"UP\"");
    }

    @Test
    void contrato_openapi_e_servido() {
        String body = client().get().uri("/v3/api-docs").retrieve().body(String.class);
        assertThat(body).contains("\"openapi\"").contains("Plataforma BPMN");
    }
}
