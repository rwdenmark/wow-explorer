package com.wowexplorer.character;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "character_lookup")
public class CharacterLookup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "realm_slug", nullable = false, length = 100)
    private String realmSlug;

    @Column(name = "character_name", nullable = false, length = 100)
    private String characterName;

    @Column(name = "looked_up_at", nullable = false)
    private OffsetDateTime lookedUpAt;

    protected CharacterLookup() {}

    public CharacterLookup(String realmSlug, String characterName) {
        this.realmSlug = realmSlug;
        this.characterName = characterName;
        this.lookedUpAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getRealmSlug() { return realmSlug; }
    public String getCharacterName() { return characterName; }
    public OffsetDateTime getLookedUpAt() { return lookedUpAt; }
}
