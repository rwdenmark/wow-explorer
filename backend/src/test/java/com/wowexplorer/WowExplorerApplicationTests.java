package com.wowexplorer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

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
    }
}
