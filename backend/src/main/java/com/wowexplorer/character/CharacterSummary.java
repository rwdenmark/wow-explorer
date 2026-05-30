package com.wowexplorer.character;

public record CharacterSummary(
        String name,
        String realm,
        String realmSlug,
        String characterClass,
        String race,
        String faction,
        Integer itemLevel,
        String raidProgress,
        Double raiderIoScore,
        Integer achievementPoints,
        Integer maxedReputations,
        Integer totalMounts,
        Integer totalPets,
        Integer totalToys,
        String renderUrl,
        RenderBounds renderBounds
) {}
