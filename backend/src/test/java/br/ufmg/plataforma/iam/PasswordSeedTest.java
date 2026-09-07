package br.ufmg.plataforma.iam;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Trava o hash BCrypt do usuário {@code admin} semeado por {@code V2__iam.sql}: se alguém trocar
 * o hash na migration sem trocar a senha documentada (ou vice-versa), este teste quebra.
 */
class PasswordSeedTest {

    /** Senha do usuário {@code admin} inicial (documentada em {@code V2__iam.sql}). */
    private static final String SEED_PASSWORD = "admin12345";

    private static final Pattern BCRYPT_IN_INSERT =
            Pattern.compile("'(\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{50,60})'");

    @Test
    void hash_semeado_na_migration_confere_com_a_senha_documentada() throws IOException {
        String sql = readMigration();
        Matcher matcher = BCRYPT_IN_INSERT.matcher(sql);
        assertThat(matcher.find()).as("hash BCrypt presente em V2__iam.sql").isTrue();
        String hash = matcher.group(1);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        assertThat(encoder.matches(SEED_PASSWORD, hash)).isTrue();
        assertThat(encoder.matches("senha-errada", hash)).isFalse();
    }

    private static String readMigration() throws IOException {
        try (InputStream in =
                PasswordSeedTest.class.getResourceAsStream("/db/migration/V2__iam.sql")) {
            assertThat(in).as("V2__iam.sql no classpath").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
