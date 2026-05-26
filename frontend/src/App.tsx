import { useEffect, useState, type FormEvent } from "react";
import { fetchCharacter, fetchRealms } from "./api";
import type { CharacterSummary, Realm } from "./types";

const DEFAULT_REALM = "proudmoore";
const DEFAULT_NAME = "Zeuh";

export default function App() {
  const [realms, setRealms] = useState<Realm[]>([]);
  const [realmSlug, setRealmSlug] = useState(DEFAULT_REALM);
  const [name, setName] = useState(DEFAULT_NAME);
  const [character, setCharacter] = useState<CharacterSummary | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchRealms()
      .then(setRealms)
      .catch(err => setError(`Failed to load realms: ${err.message}`));
  }, []);

  async function onSearch(event: FormEvent) {
    event.preventDefault();
    if (!name.trim()) return;
    setLoading(true);
    setError(null);
    try {
      const result = await fetchCharacter(realmSlug, name.trim());
      setCharacter(result);
    } catch (err) {
      setCharacter(null);
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen p-8">
      <header className="max-w-5xl mx-auto mb-6">
        <h1 className="text-3xl font-bold text-wow-gold">WoW Explorer</h1>
        <p className="text-zinc-400 text-sm">Battle.net character lookup, US region.</p>
      </header>

      <form
        onSubmit={onSearch}
        className="max-w-5xl mx-auto flex flex-wrap gap-3 items-end mb-8"
      >
        <label className="flex-1 min-w-[240px]">
          <span className="block text-xs uppercase tracking-wide text-zinc-400 mb-1">
            Character name
          </span>
          <input
            type="text"
            value={name}
            onChange={e => setName(e.target.value)}
            placeholder="Zeuh"
            className="w-full px-3 py-2 rounded bg-wow-frame border border-zinc-700 focus:border-wow-gold focus:outline-none"
          />
        </label>

        <label className="flex-1 min-w-[240px]">
          <span className="block text-xs uppercase tracking-wide text-zinc-400 mb-1">
            Realm
          </span>
          <select
            value={realmSlug}
            onChange={e => setRealmSlug(e.target.value)}
            className="w-full px-3 py-2 rounded bg-wow-frame border border-zinc-700 focus:border-wow-gold focus:outline-none"
          >
            {realms.length === 0 && <option value={DEFAULT_REALM}>Loading realms…</option>}
            {realms.map(r => (
              <option key={r.slug} value={r.slug}>{r.name}</option>
            ))}
          </select>
        </label>

        <button
          type="submit"
          disabled={loading}
          className="px-5 py-2 rounded bg-wow-gold text-black font-semibold disabled:opacity-50"
        >
          {loading ? "Loading…" : "Search"}
        </button>
      </form>

      {error && (
        <div className="max-w-5xl mx-auto mb-6 px-4 py-3 rounded bg-red-900/40 border border-red-700 text-red-100">
          {error}
        </div>
      )}

      {character && <CharacterCard character={character} />}
    </div>
  );
}

function CharacterCard({ character }: { character: CharacterSummary }) {
  return (
    <section className="max-w-5xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-6">
      <div className="bg-wow-frame border border-zinc-800 rounded-lg p-6 space-y-2">
        <Row label="Achievements" value={character.achievementPoints} />
        <Row label="Total Mounts" value={character.totalMounts} />
        <Row label="Item Level" value={character.itemLevel} />
        <Row label="Raid Progress" value={character.raidProgress ?? "—"} />
        <Row label="Raider IO" value={character.raiderIoScore?.toFixed(1) ?? "—"} />

        {(character.characterClass || character.race) && (
          <p className="text-xs text-zinc-500 pt-4 border-t border-zinc-800 mt-4">
            {[character.race, character.characterClass].filter(Boolean).join(" ")}
          </p>
        )}
      </div>

      <div className="bg-wow-frame border border-zinc-800 rounded-lg p-4 flex items-center justify-center min-h-[400px] overflow-hidden">
        {character.renderUrl ? (
          <img
            src={character.renderUrl}
            alt={`${character.name} render`}
            className="max-h-[600px] object-contain scale-[1.75] origin-center"
          />
        ) : (
          <p className="text-zinc-500">No render available.</p>
        )}
      </div>
    </section>
  );
}

function Row({ label, value }: { label: string; value: string | number | null | undefined }) {
  return (
    <div className="flex justify-between border-b border-zinc-800 pb-1">
      <span className="text-zinc-400">{label}:</span>
      <span className="font-semibold text-wow-gold">{value ?? "—"}</span>
    </div>
  );
}
