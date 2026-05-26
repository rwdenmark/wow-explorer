package com.wowexplorer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: boots the full Spring context so every bean is created and wired.
 * If it passes, the application can actually start — catching whole classes of
 * bugs the mocked unit tests cannot, e.g. duplicate bean definitions or broken
 * {@code @ConfigurationProperties}.
 *
 * <p>To stay Docker-free (so it runs identically locally and in CI) it uses an
 * in-memory H2 database with Hibernate generating the schema, instead of the
 * real Postgres + Flyway used at runtime. Dummy Blizzard credentials are supplied
 * because they have no defaults; the token service only calls out lazily, so no
 * network access happens during startup.
 */
@SpringBootTest(properties = {
        "blizzard.client-id=test-id",
        "blizzard.client-secret=test-secret",
        "spring.datasource.url=jdbc:h2:mem:wowexplorer;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class WowExplorerApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: success means the application context started.
    }
}
