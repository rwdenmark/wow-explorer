package com.wowexplorer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blizzard")
public record BlizzardProperties(
        String clientId,
        String clientSecret,
        String region,
        String locale,
        String oauthBaseUrl,
        String apiBaseUrl
) {
    public String dynamicNamespace() {
        return "dynamic-" + region;
    }

    public String profileNamespace() {
        return "profile-" + region;
    }

    public String staticNamespace() {
        return "static-" + region;
    }
}
