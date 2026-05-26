package com.wowexplorer.character;

/**
 * The single record returned to the frontend. Every field maps to a line on the UI:
 *   Item Level: {itemLevel}
 *   Raid Progress: {raidProgress}
 *   Raider IO: {raiderIoScore}
 *   Achievements: {achievementPoints}
 *   Maxed Reputations: {maxedReputations}
 *   Total Mounts: {totalMounts}
 *   Total Pets: {totalPets}
 *   Total Toys: {totalToys}
 *   (image box: name, realm, race + class, renderUrl)
 */
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
