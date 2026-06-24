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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

@Service
public class CharacterService {

    private static final Logger log = LoggerFactory.getLogger(CharacterService.class);
    private static final int MAX_LOOKUP_HISTORY = 50;

    private final BlizzardClient blizzard;
    private final RaiderIoClient raiderIo;
    private final CharacterLookupRepository lookupRepo;
    private final RenderBoundsService renderBounds;
    private final RenownService renown;

    public CharacterService(BlizzardClient blizzard,
                            RaiderIoClient raiderIo,
                            CharacterLookupRepository lookupRepo,
                            RenderBoundsService renderBounds,
                            RenownService renown) {
        this.blizzard = blizzard;
        this.raiderIo = raiderIo;
        this.lookupRepo = lookupRepo;
        this.renderBounds = renderBounds;
        this.renown = renown;
    }

    @Transactional
    @Cacheable(value = CacheConfig.CHARACTER_CACHE, key = "#realmSlug.toLowerCase() + '/' + #name.toLowerCase()")
    public CharacterSummary getSummary(String realmSlug, String name) {
        String slug = realmSlug.toLowerCase();
        String lowerName = name.toLowerCase();

        // Fan the independent Blizzard and Raider.IO calls out concurrently on virtual
        // threads so a lookup costs roughly one round-trip instead of the sum of them all.
        // Each call hits a different endpoint for the same character and is independent.
        Map<String, Object> profile, media, achievements, mounts, pets, toys, reputations, rio;
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Map<String, Object>> profileF = pool.submit(() -> blizzard.characterProfile(slug, lowerName));
            Future<Map<String, Object>> mediaF = pool.submit(() -> blizzard.characterMedia(slug, lowerName));
            Future<Map<String, Object>> achievementsF = pool.submit(() -> blizzard.characterAchievements(slug, lowerName));
            Future<Map<String, Object>> mountsF = pool.submit(() -> blizzard.characterMounts(slug, lowerName));
            // Collections/reputations can 404 for sparse characters; treat absent as empty.
            Future<Map<String, Object>> petsF = pool.submit(() -> optional(() -> blizzard.characterPets(slug, lowerName)));
            Future<Map<String, Object>> toysF = pool.submit(() -> optional(() -> blizzard.characterToys(slug, lowerName)));
            Future<Map<String, Object>> reputationsF = pool.submit(() -> optional(() -> blizzard.characterReputations(slug, lowerName)));
            Future<Map<String, Object>> rioF = pool.submit(() -> raiderIoOrEmpty(slug, name));

            profile = get(profileF);
            media = get(mediaF);
            achievements = get(achievementsF);
            mounts = get(mountsF);
            pets = get(petsF);
            toys = get(toysF);
            reputations = get(reputationsF);
            rio = get(rioF);
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
        pruneLookupHistory();
        return summary;
    }

    private void pruneLookupHistory() {
        List<Long> cutoff = lookupRepo.findIdsNewestFirst(PageRequest.of(MAX_LOOKUP_HISTORY - 1, 1));
        if (cutoff != null && !cutoff.isEmpty()) {
            lookupRepo.deleteByIdLessThan(cutoff.get(0));
        }
    }

    @Transactional(readOnly = true)
    public List<RecentCharacter> recentLookups(int limit) {
        return lookupRepo.findRecentDistinct(PageRequest.of(0, limit));
    }

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

    /** Treats a 404 or empty body as an empty result. */
    private static Map<String, Object> optional(Supplier<Map<String, Object>> call) {
        try {
            Map<String, Object> result = call.get();
            return result != null ? result : Map.of();
        } catch (NotFoundException e) {
            return Map.of();
        }
    }

    /** Raider.IO has its own coverage; a missing profile is not an error for us. */
    private Map<String, Object> raiderIoOrEmpty(String slug, String name) {
        try {
            return raiderIo.characterProfile(slug, name);
        } catch (NotFoundException nf) {
            log.info("No Raider.IO profile for {}-{}, returning Blizzard-only data", slug, name);
            return Map.of();
        }
    }

    /** Unwraps a future, re-throwing the original RuntimeException (e.g. NotFoundException). */
    private static <T> T get(Future<T> future) {
        try {
            return future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error err) throw err;
            throw new IllegalStateException(cause);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Character lookup interrupted", ie);
        }
    }

    private static int collectionSize(Map<String, Object> source, String key) {
        return source != null && source.get(key) instanceof List<?> list ? list.size() : 0;
    }

    /**
     * Counts reputations the character has fully maxed:
     *   - renown factions whose current renown_level has reached the faction's cap, and
     *   - classic tier / friendship factions at their terminal standing (max == 0).
     */
    @SuppressWarnings("unchecked")
    private int maxedReputationCount(Map<String, Object> reputations) {
        if (reputations == null || !(reputations.get("reputations") instanceof List<?> reps)) return 0;
        // Sequential on the request thread: renown caps are memoized (Caffeine), so this
        // pays a per-faction game-data call only on a cold cache, and concurrent requests
        // for the same faction share a single load rather than starving a shared pool.
        return (int) reps.stream()
                .filter(Map.class::isInstance)
                .map(r -> (Map<String, Object>) r)
                .filter(this::isMaxedReputation)
                .count();
    }

    @SuppressWarnings("unchecked")
    private boolean isMaxedReputation(Map<String, Object> rep) {
        if (!(rep.get("standing") instanceof Map<?, ?> s)) return false;
        Map<String, Object> standing = (Map<String, Object>) s;

        if (isRenown(rep)) {
            int level = standing.get("renown_level") instanceof Number n ? n.intValue() : 0;
            int cap = renown.maxRenownLevel(factionId(rep));
            return cap > 0 && level >= cap;
        }
        // Classic tier / friendship factions report max == 0 at their terminal standing.
        return standing.get("max") instanceof Number max && max.intValue() == 0
                && standing.get("raw") instanceof Number raw && raw.intValue() > 0;
    }

    @SuppressWarnings("unchecked")
    private static boolean isRenown(Map<String, Object> rep) {
        return rep.get("standing") instanceof Map<?, ?> s
                && ((Map<String, Object>) s).get("renown_level") instanceof Number;
    }

    @SuppressWarnings("unchecked")
    private static int factionId(Map<String, Object> rep) {
        return rep.get("faction") instanceof Map<?, ?> f
                && ((Map<String, Object>) f).get("id") instanceof Number n ? n.intValue() : 0;
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
