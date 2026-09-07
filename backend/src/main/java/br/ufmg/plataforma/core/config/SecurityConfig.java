package br.ufmg.plataforma.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração de segurança <strong>provisória</strong> do M01.
 *
 * <p>O {@code spring-boot-starter-security} bloqueia tudo por padrão, o que impediria o acesso ao
 * Swagger e ao Actuator. Enquanto o <strong>M02 (IAM)</strong> não entrega autenticação JWT e
 * RBAC, esta cadeia deixa a API aberta. O M02 substitui este bean por um
 * {@code SecurityFilterChain} com filtro JWT e regras por papel.
 */
@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
