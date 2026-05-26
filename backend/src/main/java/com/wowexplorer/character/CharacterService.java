package com.wowexplorer.character;

import com.wowexplorer.blizzard.BlizzardClient;
import com.wowexplorer.config.CacheConfig;
import com.wowexplorer.error.NotFoundException;
import com.wowexplorer.raiderio.RaiderIoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class CharacterService {

    private static final Logger log = LoggerFactory.getLogger(CharacterService.class);

    private final BlizzardClient blizzard;
    private final RaiderIoClient raiderIo;
    private final CharacterLookupRepository lookupRepo;
    private final RenderBoundsService renderBounds;

    public CharacterService(BlizzardClient blizzard,
                            RaiderIoClient raiderIo,
                            CharacterLookupRepository lookupRepo,
                            RenderBoundsService renderBounds) {
        this.blizzard = blizzard;
        this.raiderIo = raiderIo;
        this.lookupRepo = lookupRepo;
        this.renderBounds = renderBounds;
    }

    /**
     * Pulls the full character summary used by the single-page UI.
     * Sequential by design — easy to read. Parallelize later with CompletableFuture
     * if the ~1s response time becomes a problem.
     */
    @Transactional
    @Cacheable(value = CacheConfig.CHARACTER_CACHE, key = "#realmSlug + '/' + #name.toLowerCase()")
    public CharacterSummary getSummary(String realmSlug, String name) {
        String slug = realmSlug.toLowerCase();
        String lowerName = name.toLowerCase();

        Map<String, Object> profile = blizzard.characterProfile(slug, lowerName);
        Map<String, Object> media = blizzard.characterMedia(slug, lowerName);
        Map<String, Object> achievements = blizzard.characterAchievements(slug, lowerName);
        Map<String, Object> mounts = blizzard.characterMounts(slug, lowerName);
        // Collections/reputations can 404 for sparse characters; treat absent as empty.
        Map<String, Object> pets = optional(() -> blizzard.characterPets(slug, lowerName));
        Map<String, Object> toys = optional(() -> blizzard.characterToys(slug, lowerName));
        Map<String, Object> reputations = optional(() -> blizzard.characterReputations(slug, lowerName));

        Map<String, Object> rio;
        try {
            rio = raiderIo.characterProfile(slug, name);
        } catch (NotFoundException nf) {
            log.info("No Raider.IO profile for {}-{}, returning Blizzard-only data", slug, name);
            rio = Map.of();
        }

        String renderUrl = pickRenderUrl(media);
        CharacterSummary summary = new CharacterSummary(
                (String) profile.get("name"),
                nested(profile, "realm", "name"),
                slug,
                nested(profile, "character_class", "name"),
                nested(profile, "race", "name"),
                nested(profile, "faction", "name"),
                asInt(profile.get("equipped_item_level")),
                raidProgressionSummary(rio),
                raiderIoScore(rio),
                asInt(achievements.get("total_points")),
                maxedReputationCount(reputations),
                collectionSize(mounts, "mounts"),
                collectionSize(pets, "pets"),
                collectionSize(toys, "toys"),
                renderUrl,
                renderBounds.analyze(renderUrl)
        );

        lookupRepo.save(new CharacterLookup(slug, lowerName));
        return summary;
    }

    /** Recently looked-up distinct characters, newest first, for the "Recently viewed" panel. */
    @Transactional(readOnly = true)
    public List<RecentCharacter> recentLookups(int limit) {
        return lookupRepo.findRecentDistinct(PageRequest.of(0, limit));
    }

    /** Removes a character from lookup history so it no longer appears in "recently viewed". */
    @Transactional
    public void forgetLookup(String realmSlug, String name) {
        lookupRepo.deleteAllForCharacter(realmSlug.toLowerCase(), name.toLowerCase());
    }

    @SuppressWarnings("unchecked")
    private static String nested(Map<String, Object> source, String... path) {
        Object cursor = source;
        for (String key : path) {
            if (!(cursor instanceof Map<?, ?> m)) return null;
            cursor = ((Map<String, Object>) m).get(key);
        }
        return cursor == null ? null : cursor.toString();
    }

    private static Integer asInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String pickRenderUrl(Map<String, Object> media) {
        List<Map<String, Object>> assets = (List<Map<String, Object>>) media.get("assets");
        if (assets == null) return null;
        // Prefer the full main-raw render; fall back to main, then avatar.
        return findAsset(assets, "main-raw")
                .or(() -> findAsset(assets, "main"))
                .or(() -> findAsset(assets, "avatar"))
                .orElse(null);
    }

    private static java.util.Optional<String> findAsset(List<Map<String, Object>> assets, String key) {
        return assets.stream()
                .filter(a -> key.equals(a.get("key")))
                .map(a -> (String) a.get("value"))
                .findFirst();
    }

    /** Runs an optional Blizzard call, treating a 404 (or empty body) as an empty result. */
    private static Map<String, Object> optional(Supplier<Map<String, Object>> call) {
        try {
            Map<String, Object> result = call.get();
            return result != null ? result : Map.of();
        } catch (NotFoundException e) {
            return Map.of();
        }
    }

    private static int collectionSize(Map<String, Object> source, String key) {
        return source != null && source.get(key) instanceof List<?> list ? list.size() : 0;
    }

    /**
     * Counts reputations the character has fully maxed. A standing is maxed when there's
     * nothing left to earn — {@code max == 0} — which holds across both the classic tier
     * system (Exalted, terminal friendship ranks) and capped Renown factions.
     */
    @SuppressWarnings("unchecked")
    private static Integer maxedReputationCount(Map<String, Object> reputations) {
        if (reputations == null || !(reputations.get("reputations") instanceof List<?> reps)) return 0;
        return (int) reps.stream()
                .filter(Map.class::isInstance)
                .map(r -> ((Map<String, Object>) r).get("standing"))
                .filter(Map.class::isInstance)
                .map(s -> (Map<String, Object>) s)
                .filter(s -> s.get("max") instanceof Number max && max.intValue() == 0
                        && s.get("raw") instanceof Number raw && raw.intValue() > 0)
                .count();
    }

    @SuppressWarnings("unchecked")
    private static String raidProgressionSummary(Map<String, Object> rio) {
        Map<String, Object> progression = (Map<String, Object>) rio.get("raid_progression");
        if (progression == null || progression.isEmpty()) return null;
        // Pick whichever raid has the highest total bosses killed across difficulties.
        return progression.values().stream()
                .map(v -> (Map<String, Object>) v)
                .max((a, b) -> Integer.compare(totalKills(a), totalKills(b)))
                .map(m -> (String) m.get("summary"))
                .orElse(null);
    }

    private static int totalKills(Map<String, Object> raid) {
        return asIntDefault(raid.get("normal_bosses_killed"))
                + asIntDefault(raid.get("heroic_bosses_killed"))
                + asIntDefault(raid.get("mythic_bosses_killed"));
    }

    private static int asIntDefault(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }

    @SuppressWarnings("unchecked")
    private static Double raiderIoScore(Map<String, Object> rio) {
        List<Map<String, Object>> seasons =
                (List<Map<String, Object>>) rio.get("mythic_plus_scores_by_season");
        if (seasons == null || seasons.isEmpty()) return null;
        Map<String, Object> scores = (Map<String, Object>) seasons.get(0).get("scores");
        if (scores == null) return null;
        Object all = scores.get("all");
        return all instanceof Number n ? n.doubleValue() : null;
    }
}
