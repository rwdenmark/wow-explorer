package com.wowexplorer.blizzard;

import com.wowexplorer.config.BlizzardProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Fetches and caches a Battle.net OAuth Client Credentials token.
 * Tokens are valid 24h. We refresh ~60s before expiry.
 */
@Service
public class BlizzardTokenService {

    private static final Logger log = LoggerFactory.getLogger(BlizzardTokenService.class);

    private final RestClient oauthClient;
    private final BlizzardProperties props;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public BlizzardTokenService(RestClient blizzardOauthClient, BlizzardProperties props) {
        this.oauthClient = blizzardOauthClient;
        this.props = props;
    }

    public String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt)) {
            return cachedToken;
        }
        lock.lock();
        try {
            if (cachedToken != null && Instant.now().isBefore(expiresAt)) {
                return cachedToken;
            }
            refresh();
            return cachedToken;
        } finally {
            lock.unlock();
        }
    }

    private void refresh() {
        log.info("Fetching new Blizzard access token");
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        TokenResponse response = oauthClient.post()
                .uri("/token")
                .headers(h -> h.setBasicAuth(props.clientId(), props.clientSecret()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Blizzard returned an empty token response");
        }
        this.cachedToken = response.accessToken();
        this.expiresAt = Instant.now().plusSeconds(Math.max(60, response.expiresIn() - 60));
        log.debug("Cached Blizzard token, expires at {}", expiresAt);
    }
}
