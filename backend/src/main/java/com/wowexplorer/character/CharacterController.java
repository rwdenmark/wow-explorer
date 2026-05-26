package com.wowexplorer.character;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
public class CharacterController {

    private final CharacterService characterService;

    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    @GetMapping("/recent")
    public List<RecentCharacter> recent(@RequestParam(defaultValue = "10") int limit) {
        return characterService.recentLookups(limit);
    }

    @DeleteMapping("/recent/{realmSlug}/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forget(@PathVariable String realmSlug, @PathVariable String name) {
        characterService.forgetLookup(realmSlug, name);
    }

    @GetMapping("/{realmSlug}/{name}")
    public CharacterSummary getCharacter(@PathVariable String realmSlug,
                                         @PathVariable String name) {
        return characterService.getSummary(realmSlug, name);
    }
}
