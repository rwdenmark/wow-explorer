export interface Realm {
  slug: string;
  name: string;
}

export interface CharacterSummary {
  name: string;
  realm: string;
  realmSlug: string;
  characterClass: string | null;
  race: string | null;
  itemLevel: number | null;
  raidProgress: string | null;
  raiderIoScore: number | null;
  achievementPoints: number | null;
  totalMounts: number | null;
  renderUrl: string | null;
}
