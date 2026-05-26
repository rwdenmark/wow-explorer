export interface Realm {
  slug: string;
  name: string;
}

export interface RecentCharacter {
  realmSlug: string;
  name: string;
}

export interface CharacterSummary {
  name: string;
  realm: string;
  realmSlug: string;
  characterClass: string | null;
  race: string | null;
  faction: string | null;
  itemLevel: number | null;
  raidProgress: string | null;
  raiderIoScore: number | null;
  achievementPoints: number | null;
  maxedReputations: number | null;
  totalMounts: number | null;
  totalPets: number | null;
  totalToys: number | null;
  renderUrl: string | null;
  renderBounds: { top: number; height: number } | null;
}
