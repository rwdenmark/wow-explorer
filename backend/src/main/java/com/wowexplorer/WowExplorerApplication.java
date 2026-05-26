package com.wowexplorer;

import com.wowexplorer.config.BlizzardProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties(BlizzardProperties.class)
public class WowExplorerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WowExplorerApplication.class, args);
    }
}
