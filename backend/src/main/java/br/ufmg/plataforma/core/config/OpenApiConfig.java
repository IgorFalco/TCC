package br.ufmg.plataforma.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documento OpenAPI da plataforma (Swagger UI em {@code /swagger-ui.html}).
 *
 * <p>Já declara o esquema de segurança {@code bearer-jwt} para que o botão <em>Authorize</em>
 * funcione assim que o M02 (IAM) expuser o login.
 */
@Configuration
class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    OpenAPI plataformaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Plataforma BPMN com Engenharia de Sistemas — API")
                        .description("TCC2 — modelagem BPMN, requisitos, metamodelo dinâmico e execução de processos.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
