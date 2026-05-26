import type { CharacterSummary, Realm, RecentCharacter } from "./types";

async function getJson<T>(url: string): Promise<T> {
  const res = await fetch(url);
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`${res.status} ${res.statusText}${body ? ` — ${body}` : ""}`);
  }
  return res.json() as Promise<T>;
}

export const fetchRealms = () => getJson<Realm[]>("/api/realms");

export const fetchCharacter = (realmSlug: string, name: string) =>
  getJson<CharacterSummary>(
    `/api/characters/${encodeURIComponent(realmSlug)}/${encodeURIComponent(name)}`
  );

export const fetchRecent = () => getJson<RecentCharacter[]>("/api/characters/recent");

export async function deleteRecent(realmSlug: string, name: string): Promise<void> {
  const res = await fetch(
    `/api/characters/recent/${encodeURIComponent(realmSlug)}/${encodeURIComponent(name)}`,
    { method: "DELETE" }
  );
  if (!res.ok) {
    throw new Error(`Failed to remove ${name}: ${res.status} ${res.statusText}`);
  }
}
