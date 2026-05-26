package com.wowexplorer.character;

/**
 * A previously looked-up character, for the "Recently viewed" panel.
 * {@code realmSlug} and {@code name} are stored lowercased; the UI title-cases
 * the name and resolves the realm slug to its display name.
 */
public record RecentCharacter(String realmSlug, String name) {}
