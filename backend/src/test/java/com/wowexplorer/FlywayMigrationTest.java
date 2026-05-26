package com.wowexplorer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Migration guard: runs the real Flyway migrations against an in-memory H2 (in
 * PostgreSQL-compatibility mode) and then has Hibernate {@code validate} the JPA
 * entities against the resulting schema.
 *
 * <p>If this passes, {@code db/migration} applies cleanly and the entity mappings
 * match the migrated tables — i.e. {@code ddl-auto: validate} would also pass
 * against real Postgres at runtime. It catches migration/entity drift without
 * needing Docker. (The companion {@link WowExplorerApplicationTests} checks bean
 * wiring with Hibernate generating the schema instead.)
 */
@SpringBootTest(properties = {
        "blizzard.client-id=test-id",
        "blizzard.client-secret=test-secret",
        // H2 (PostgreSQL mode) doesn't know the `TIMESTAMPTZ` shorthand used by the
        // migration, so define it as a domain alias on connect (INIT runs before Flyway).
        // This lets the real, unmodified migration apply without touching its checksum.
        "spring.datasource.url=jdbc:h2:mem:flyway;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "INIT=CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class FlywayMigrationTest {

    @Test
    void migrationsApplyAndSchemaValidates() {
        // Intentionally empty: success means Flyway migrated and the JPA schema validated.
    }
}
