package com.wowexplorer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient blizzardOauthClient(BlizzardProperties props) {
        return RestClient.builder()
                .baseUrl(props.oauthBaseUrl())
                .build();
    }

    @Bean
    public RestClient blizzardApiClient(BlizzardProperties props) {
        return RestClient.builder()
                .baseUrl(props.apiBaseUrl())
                .build();
    }

    @Bean
    public RestClient raiderIoRestClient(@Value("${raiderio.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
