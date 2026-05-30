package com.wowexplorer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "blizzard.client-id=test-id",
        "blizzard.client-secret=test-secret",
        // H2's PostgreSQL mode doesn't know TIMESTAMPTZ; alias it on connect so the
        // unmodified migration applies (INIT runs before Flyway).
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
    }
}
