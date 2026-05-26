package com.wowexplorer.character;

import com.wowexplorer.blizzard.BlizzardClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a renown faction's maximum renown level from static game data, so we can
 * tell whether a character's {@code renown_level} is actually capped (= maxed).
 *
 * <p>Faction renown caps are static game data (they change only on a patch), so each
 * faction is fetched once and memoized for the lifetime of the app. A failed lookup
 * is cached as 0 to avoid hammering the API on every request.
 */
@Service
public class RenownService {

    private static final Logger log = LoggerFactory.getLogger(RenownService.class);

    private final BlizzardClient blizzard;
    private final Map<Integer, Integer> capCache = new ConcurrentHashMap<>();

    public RenownService(BlizzardClient blizzard) {
        this.blizzard = blizzard;
    }

    /** Highest renown level for the faction, or 0 if it isn't a renown faction or lookup fails. */
    public int maxRenownLevel(int factionId) {
        Integer cached = capCache.get(factionId);
        if (cached != null) return cached;
        int cap = fetchMaxRenownLevel(factionId);
        capCache.putIfAbsent(factionId, cap);
        return cap;
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
