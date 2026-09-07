package br.ufmg.plataforma.core.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** CORS para o frontend (M14). Demais aspectos de MVC ficam nos defaults do Spring Boot. */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
class WebConfig implements WebMvcConfigurer {

    private final CorsProperties cors;

    WebConfig(CorsProperties cors) {
        this.cors = cors;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(cors.allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
