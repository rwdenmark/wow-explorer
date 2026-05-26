package com.wowexplorer.character;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/characters")
public class CharacterController {

    private final CharacterService characterService;

    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    @GetMapping("/{realmSlug}/{name}")
    public CharacterSummary getCharacter(@PathVariable String realmSlug,
                                         @PathVariable String name) {
        return characterService.getSummary(realmSlug, name);
    }
}
