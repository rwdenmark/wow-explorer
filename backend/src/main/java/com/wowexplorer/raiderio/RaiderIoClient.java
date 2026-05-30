package com.wowexplorer.raiderio;

import com.wowexplorer.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/** Current-season Mythic+ score and raid progression — fields Blizzard's API doesn't expose cleanly. */
@Component
public class RaiderIoClient {

    private static final Logger log = LoggerFactory.getLogger(RaiderIoClient.class);
    private static final String FIELDS = "mythic_plus_scores_by_season:current,raid_progression";

    private final RestClient restClient;

    public RaiderIoClient(RestClient raiderIoRestClient) {
        this.restClient = raiderIoRestClient;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> characterProfile(String realmSlug, String name) {
        log.debug("Raider.IO GET realm={} name={}", realmSlug, name);
        return restClient.get()
                .uri(uri -> uri.path("/characters/profile")
                        .queryParam("region", "us")
                        .queryParam("realm", realmSlug)
                        .queryParam("name", name)
                        .queryParam("fields", FIELDS)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                    if (resp.getStatusCode().value() == 400 || resp.getStatusCode().value() == 404) {
                        // Raider.IO returns 400 for unknown characters with a JSON body.
                        throw new NotFoundException("Raider.IO has no record for " + realmSlug + "/" + name);
                    }
                    throw new IllegalStateException("Raider.IO " + resp.getStatusCode());
                })
                .body(Map.class);
    }
}
