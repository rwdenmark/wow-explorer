package com.wowexplorer.character;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wowexplorer.blizzard.BlizzardClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Resolves a renown faction's maximum renown level from static game data, so we can
 * tell whether a character's {@code renown_level} is actually capped (= maxed).
 *
 * <p>Faction renown caps are static game data (they change only on a patch), so each
 * faction is fetched once and cached. The cache expires after 24h so a new patch's caps
 * are picked up without restarting the app. A failed lookup caches 0 (re-attempted after
 * the TTL) to avoid hammering the API on every request.
 */
@Service
public class RenownService {

    private static final Logger log = LoggerFactory.getLogger(RenownService.class);
    private static final Duration CAP_TTL = Duration.ofHours(24);

    private final BlizzardClient blizzard;
    private final Cache<Integer, Integer> capCache = Caffeine.newBuilder()
            .expireAfterWrite(CAP_TTL)
            .maximumSize(1_000)
            .build();

    public RenownService(BlizzardClient blizzard) {
        this.blizzard = blizzard;
    }

    /** Highest renown level for the faction, or 0 if it isn't a renown faction or lookup fails. */
    public int maxRenownLevel(int factionId) {
        return capCache.get(factionId, this::fetchMaxRenownLevel);
    }

    @SuppressWarnings("unchecked")
    private int fetchMaxRenownLevel(int factionId) {
        try {
            Map<String, Object> faction = blizzard.reputationFaction(factionId);
            if (!(faction.get("renown_tiers") instanceof List<?> tiers)) return 0;
            return tiers.stream()
                    .filter(Map.class::isInstance)
                    .map(t -> ((Map<String, Object>) t).get("level"))
                    .filter(Number.class::isInstance)
                    .mapToInt(n -> ((Number) n).intValue())
                    .max()
                    .orElse(0);
        } catch (Exception e) {
            log.info("Could not resolve renown cap for faction {}: {}", factionId, e.toString());
            return 0;
        }
    }
}
