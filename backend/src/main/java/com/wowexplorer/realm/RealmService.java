package com.wowexplorer.realm;

import com.wowexplorer.blizzard.BlizzardClient;
import com.wowexplorer.config.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class RealmService {

    private final BlizzardClient blizzard;

    public RealmService(BlizzardClient blizzard) {
        this.blizzard = blizzard;
    }

    @SuppressWarnings("unchecked")
    @Cacheable(CacheConfig.REALMS_CACHE)
    public List<Realm> listRealms() {
        Map<String, Object> response = blizzard.realmIndex();
        List<Map<String, Object>> realms = (List<Map<String, Object>>) response.get("realms");
        if (realms == null) {
            return List.of();
        }
        return realms.stream()
                .map(r -> new Realm((String) r.get("slug"), (String) r.get("name")))
                .sorted(Comparator.comparing(Realm::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
