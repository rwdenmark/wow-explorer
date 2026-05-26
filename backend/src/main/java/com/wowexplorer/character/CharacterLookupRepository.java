package com.wowexplorer.character;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterLookupRepository extends JpaRepository<CharacterLookup, Long> {
}
