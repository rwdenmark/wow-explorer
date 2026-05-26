package com.wowexplorer.character;

import com.wowexplorer.blizzard.BlizzardClient;
import com.wowexplorer.error.NotFoundException;
import com.wowexplorer.raiderio.RaiderIoClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CharacterServiceTest {

    @Mock BlizzardClient blizzard;
    @Mock RaiderIoClient raiderIo;
    @Mock CharacterLookupRepository lookupRepo;
    @InjectMocks CharacterService service;

    @Test
    void buildsSummaryFromAllSources() {
        givenBlizzardProfile();
        givenBlizzardMedia();
        givenBlizzardAchievements();
        givenBlizzardMounts();
        givenRaiderIo();

        CharacterSummary summary = service.getSummary("proudmoore", "Zeuh");

        assertThat(summary.name()).isEqualTo("Zeuh");
        assertThat(summary.realm()).isEqualTo("Proudmoore");
        assertThat(summary.itemLevel()).isEqualTo(636);
        assertThat(summary.achievementPoints()).isEqualTo(28310);
        assertThat(summary.totalMounts()).isEqualTo(3);
        assertThat(summary.raidProgress()).isEqualTo("8/8 H");
        assertThat(summary.raiderIoScore()).isEqualTo(2450.0);
        assertThat(summary.renderUrl()).isEqualTo("https://render/main-raw.png");
        verify(lookupRepo).save(any(CharacterLookup.class));
    }

    @Test
    void tolerates_missing_raiderio_profile() {
        givenBlizzardProfile();
        givenBlizzardMedia();
        givenBlizzardAchievements();
        givenBlizzardMounts();
        given(raiderIo.characterProfile(anyString(), anyString()))
                .willThrow(new NotFoundException("no raider.io"));

        CharacterSummary summary = service.getSummary("proudmoore", "Zeuh");

        assertThat(summary.raiderIoScore()).isNull();
        assertThat(summary.raidProgress()).isNull();
        assertThat(summary.itemLevel()).isEqualTo(636);
    }

    private void givenBlizzardProfile() {
        given(blizzard.characterProfile("proudmoore", "zeuh")).willReturn(Map.of(
                "name", "Zeuh",
                "equipped_item_level", 636,
                "realm", Map.of("name", "Proudmoore", "slug", "proudmoore"),
                "character_class", Map.of("name", "Mage"),
                "race", Map.of("name", "Troll")
        ));
    }

    private void givenBlizzardMedia() {
        given(blizzard.characterMedia("proudmoore", "zeuh")).willReturn(Map.of(
                "assets", List.of(
                        Map.of("key", "avatar", "value", "https://render/avatar.png"),
                        Map.of("key", "main-raw", "value", "https://render/main-raw.png")
                )
        ));
    }

    private void givenBlizzardAchievements() {
        given(blizzard.characterAchievements("proudmoore", "zeuh"))
                .willReturn(Map.of("total_points", 28310, "total_quantity", 1900));
    }

    private void givenBlizzardMounts() {
        given(blizzard.characterMounts("proudmoore", "zeuh")).willReturn(Map.of(
                "mounts", List.of(Map.of("id", 1), Map.of("id", 2), Map.of("id", 3))
        ));
    }

    private void givenRaiderIo() {
        given(raiderIo.characterProfile("proudmoore", "Zeuh")).willReturn(Map.of(
                "raid_progression", Map.of(
                        "liberation-of-undermine", Map.of(
                                "summary", "8/8 H",
                                "normal_bosses_killed", 8,
                                "heroic_bosses_killed", 8,
                                "mythic_bosses_killed", 0
                        )
                ),
                "mythic_plus_scores_by_season", List.of(
                        Map.of("scores", Map.of("all", 2450.0))
                )
        ));
    }
}
