package com.wowexplorer.character;

/**
 * The single record returned to the frontend. Every field matches a line on the UI:
 *   Character: {name} {realm}
 *   Item Level: {itemLevel}
 *   Raid Progress: {raidProgress}
 *   Raider IO: {raiderIoScore}
 *   Achievements: {achievementPoints}
 *   Total Mounts: {totalMounts}
 *   (rendered image: renderUrl)
 */
public record CharacterSummary(
        String name,
        String realm,
        String realmSlug,
        String characterClass,
        String race,
        Integer itemLevel,
        String raidProgress,
        Double raiderIoScore,
        Integer achievementPoints,
        Integer totalMounts,
        String renderUrl
) {}
