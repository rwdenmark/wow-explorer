import type { CharacterSummary, Realm } from "./types";

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
