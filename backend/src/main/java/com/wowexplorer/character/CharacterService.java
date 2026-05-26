package com.wowexplorer.character;

import com.wowexplorer.blizzard.BlizzardClient;
import com.wowexplorer.config.CacheConfig;
import com.wowexplorer.error.NotFoundException;
import com.wowexplorer.raiderio.RaiderIoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class CharacterService {

    private static final Logger log = LoggerFactory.getLogger(CharacterService.class);

    private final BlizzardClient blizzard;
    private final RaiderIoClient raiderIo;
    private final CharacterLookupRepository lookupRepo;

    public CharacterService(BlizzardClient blizzard,
                            RaiderIoClient raiderIo,
                            CharacterLookupRepository lookupRepo) {
        this.blizzard = blizzard;
        this.raiderIo = raiderIo;
        this.lookupRepo = lookupRepo;
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

        Map<String, Object> rio;
        try {
            rio = raiderIo.characterProfile(slug, name);
        } catch (NotFoundException nf) {
            log.info("No Raider.IO profile for {}-{}, returning Blizzard-only data", slug, name);
            rio = Map.of();
        }

        CharacterSummary summary = new CharacterSummary(
                (String) profile.get("name"),
                nested(profile, "realm", "name"),
                slug,
                nested(profile, "character_class", "name"),
                nested(profile, "race", "name"),
                asInt(profile.get("equipped_item_level")),
                raidProgressionSummary(rio),
                raiderIoScore(rio),
                asInt(achievements.get("total_points")),
                mountCount(mounts),
                pickRenderUrl(media)
        );

        lookupRepo.save(new CharacterLookup(slug, lowerName));
        return summary;
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

    @SuppressWarnings("unchecked")
    private static Integer mountCount(Map<String, Object> mounts) {
        List<?> mountList = (List<?>) mounts.get("mounts");
        return mountList == null ? 0 : mountList.size();
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
