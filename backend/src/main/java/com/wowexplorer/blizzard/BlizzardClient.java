package com.wowexplorer.blizzard;

import com.wowexplorer.config.BlizzardProperties;
import com.wowexplorer.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Thin wrapper around the Blizzard WoW API. Every call attaches a fresh OAuth token
 * and the right namespace + locale. Returns raw JSON maps so the calling service
 * can pull only the fields it needs.
 */
@Component
public class BlizzardClient {

    private static final Logger log = LoggerFactory.getLogger(BlizzardClient.class);

    private final RestClient apiClient;
    private final BlizzardTokenService tokenService;
    private final BlizzardProperties props;

    public BlizzardClient(RestClient blizzardApiClient,
                          BlizzardTokenService tokenService,
                          BlizzardProperties props) {
        this.apiClient = blizzardApiClient;
        this.tokenService = tokenService;
        this.props = props;
    }

    public Map<String, Object> realmIndex() {
        return get("/data/wow/realm/index", props.dynamicNamespace());
    }

    public Map<String, Object> characterProfile(String realmSlug, String name) {
        return get("/profile/wow/character/" + realmSlug + "/" + name, props.profileNamespace());
    }

    public Map<String, Object> characterMedia(String realmSlug, String name) {
        return get("/profile/wow/character/" + realmSlug + "/" + name + "/character-media",
                props.profileNamespace());
    }

    public Map<String, Object> characterAchievements(String realmSlug, String name) {
        return get("/profile/wow/character/" + realmSlug + "/" + name + "/achievements",
                props.profileNamespace());
    }

    public Map<String, Object> characterMounts(String realmSlug, String name) {
        return get("/profile/wow/character/" + realmSlug + "/" + name + "/collections/mounts",
                props.profileNamespace());
    }

    public Map<String, Object> characterPets(String realmSlug, String name) {
        return get("/profile/wow/character/" + realmSlug + "/" + name + "/collections/pets",
                props.profileNamespace());
    }

    public Map<String, Object> characterToys(String realmSlug, String name) {
        return get("/profile/wow/character/" + realmSlug + "/" + name + "/collections/toys",
                props.profileNamespace());
    }

    public Map<String, Object> characterReputations(String realmSlug, String name) {
        return get("/profile/wow/character/" + realmSlug + "/" + name + "/reputations",
                props.profileNamespace());
    }

    /** Static game-data for a reputation faction, including {@code is_renown} and {@code renown_tiers}. */
    public Map<String, Object> reputationFaction(int factionId) {
        return get("/data/wow/reputation-faction/" + factionId, props.staticNamespace());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String path, String namespace) {
        log.debug("GET {} namespace={}", path, namespace);
        return apiClient.get()
                .uri(uri -> uri.path(path)
                        .queryParam("namespace", namespace)
                        .queryParam("locale", props.locale())
                        .build())
                .headers(authHeader())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                    if (resp.getStatusCode().value() == 404) {
                        throw new NotFoundException("Blizzard 404: " + path);
                    }
                    throw new IllegalStateException("Blizzard 4xx (" + resp.getStatusCode() + "): " + path);
                })
                .body(Map.class);
    }

    private Consumer<HttpHeaders> authHeader() {
        return h -> h.setBearerAuth(tokenService.getAccessToken());
    }
}
