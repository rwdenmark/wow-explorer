package com.wowexplorer.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    public static final String REALMS_CACHE = "realms";
    public static final String CHARACTER_CACHE = "characters";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(REALMS_CACHE, CHARACTER_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(1_000));
        // Realm list rarely changes — override its TTL.
        manager.registerCustomCache(REALMS_CACHE, Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(24))
                .maximumSize(10)
                .build());
        return manager;
    }
}
