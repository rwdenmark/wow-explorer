package com.wowexplorer.character;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CharacterLookupRepository extends JpaRepository<CharacterLookup, Long> {

    /**
     * Most-recently looked-up distinct characters, newest first. Repeat lookups of
     * the same character collapse to one entry, ordered by their latest row id.
     */
    @Query("""
            select new com.wowexplorer.character.RecentCharacter(c.realmSlug, c.characterName)
            from CharacterLookup c
            group by c.realmSlug, c.characterName
            order by max(c.id) desc
            """)
    List<RecentCharacter> findRecentDistinct(Pageable pageable);

    /**
     * Removes every lookup row for one character so it drops out of "recently viewed"
     * entirely (not just its most recent row).
     */
    @Modifying
    @Query("delete from CharacterLookup c where c.realmSlug = :realmSlug and c.characterName = :name")
    int deleteAllForCharacter(@Param("realmSlug") String realmSlug, @Param("name") String name);

    @Query("select c.id from CharacterLookup c order by c.id desc")
    List<Long> findIdsNewestFirst(Pageable pageable);

    @Modifying
    @Query("delete from CharacterLookup c where c.id < :minId")
    int deleteByIdLessThan(@Param("minId") Long minId);
}
