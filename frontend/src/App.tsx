import { useEffect, useState, type FormEvent } from "react";
import { deleteRecent, fetchCharacter, fetchRealms, fetchRecent } from "./api";
import type { CharacterSummary, Realm, RecentCharacter } from "./types";

const DEFAULT_REALM = "proudmoore";
const DEFAULT_NAME = "Zeuh";

// Render frame geometry (px). The character is fit between the top padding and the
// reserved bottom text zone (race/class + faction) using its measured bounds.
const RENDER_FRAME_HEIGHT = 500;
const RENDER_TOP_PADDING = 16;
const RENDER_TEXT_ZONE = 44;

export default function App() {
  const [realms, setRealms] = useState<Realm[]>([]);
  const [realmSlug, setRealmSlug] = useState(DEFAULT_REALM);
  const [name, setName] = useState(DEFAULT_NAME);
  const [character, setCharacter] = useState<CharacterSummary | null>(null);
  const [recent, setRecent] = useState<RecentCharacter[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchRealms()
      .then(setRealms)
      .catch(err => setError(`Failed to load realms: ${err.message}`));
    // Auto-load the default character so the page isn't blank on first load.
    void loadCharacter(DEFAULT_REALM, DEFAULT_NAME);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadCharacter(slug: string, charName: string) {
    const trimmed = charName.trim();
    if (!trimmed) return;
    setLoading(true);
    setError(null);
    try {
      const result = await fetchCharacter(slug, trimmed);
      setCharacter(result);
      setRealmSlug(slug);
      setName(result.name);
      setRecent(await fetchRecent());
    } catch (err) {
      setCharacter(null);
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  }

  function onSearch(event: FormEvent) {
    event.preventDefault();
    void loadCharacter(realmSlug, name);
  }

  async function forgetRecent(slug: string, charName: string) {
    try {
      await deleteRecent(slug, charName);
      setRecent(await fetchRecent());
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
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

      {character && (
        <RecentlyViewed
          recent={recent}
          current={character}
          realms={realms}
          onSelect={loadCharacter}
          onForget={forgetRecent}
        />
      )}
    </div>
  );
}

function RecentlyViewed({
  recent,
  current,
  realms,
  onSelect,
  onForget,
}: {
  recent: RecentCharacter[];
  current: CharacterSummary;
  realms: Realm[];
  onSelect: (slug: string, name: string) => void;
  onForget: (slug: string, name: string) => void;
}) {
  // The character we're viewing is excluded; show the next 3 distinct ones.
  const items = recent
    .filter(
      r =>
        !(
          r.realmSlug === current.realmSlug &&
          r.name.toLowerCase() === current.name.toLowerCase()
        )
    )
    .slice(0, 3);

  if (items.length === 0) return null;

  const realmName = (slug: string) => realms.find(r => r.slug === slug)?.name ?? slug;
  const titleCase = (s: string) => (s ? s.charAt(0).toUpperCase() + s.slice(1) : s);

  return (
    <section className="max-w-5xl mx-auto mt-6">
      <div className="bg-wow-frame border border-zinc-800 rounded-lg p-4">
        <h2 className="text-xs uppercase tracking-wide text-zinc-400 mb-3">Recently viewed</h2>
        <div className="flex flex-wrap gap-3">
          {items.map(r => (
            <div
              key={`${r.realmSlug}/${r.name}`}
              className="flex items-center rounded border border-zinc-700 hover:border-wow-gold transition-colors"
            >
              <button
                type="button"
                onClick={() => onSelect(r.realmSlug, r.name)}
                className="pl-3 pr-2 py-2 hover:text-wow-gold transition-colors"
              >
                <span className="font-semibold">{titleCase(r.name)}</span>
                <span className="text-zinc-500 text-sm"> · {realmName(r.realmSlug)}</span>
              </button>
              <button
                type="button"
                aria-label={`Remove ${titleCase(r.name)} from recently viewed`}
                title="Remove from recently viewed"
                onClick={() => onForget(r.realmSlug, r.name)}
                className="px-2 self-stretch text-zinc-500 hover:text-red-400 border-l border-zinc-700"
              >
                ×
              </button>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function CharacterCard({ character }: { character: CharacterSummary }) {
  return (
    <section className="max-w-5xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-6">
      <div className="bg-wow-frame border border-zinc-800 rounded-lg px-6 pt-6 pb-3 flex flex-col">
        <h2 className="text-lg font-bold text-wow-gold mb-4">Statistics</h2>

        <div className="space-y-2">
          <Row label="Item Level" value={character.itemLevel} />
          <Row label="Raid Progress" value={character.raidProgress || "—"} />
          <Row label="Raider IO" value={character.raiderIoScore ? character.raiderIoScore.toFixed(1) : "—"} />
        </div>

        <div className="space-y-2 mt-9">
          <Row label="Achievements" value={character.achievementPoints} />
          <Row label="Maxed Reputations" value={character.maxedReputations} />
          <Row label="Total Mounts" value={character.totalMounts} />
          <Row label="Total Pets" value={character.totalPets} />
          <Row label="Total Toys" value={character.totalToys} />
        </div>

        <div className="mt-auto pt-6 flex justify-between text-sm">
          <a
            href={`https://worldofwarcraft.blizzard.com/en-us/character/us/${character.realmSlug}/${character.name.toLowerCase()}/`}
            target="_blank"
            rel="noopener noreferrer"
            className="text-wow-gold hover:underline"
          >
            Armory
          </a>
          <a
            href={`https://www.missingmounts.com/us/${character.realmSlug}/${character.name.toLowerCase()}`}
            target="_blank"
            rel="noopener noreferrer"
            className="text-wow-gold hover:underline"
          >
            Missing Mounts
          </a>
        </div>
      </div>

      <div
        className="relative bg-wow-frame border border-zinc-800 rounded-lg overflow-hidden"
        style={{ height: `${RENDER_FRAME_HEIGHT}px` }}
      >
        <CharacterRender character={character} />

        {(character.race || character.characterClass) && (
          <span className="absolute bottom-3 left-4 z-10 text-sm text-zinc-400">
            {[character.race, character.characterClass].filter(Boolean).join(" ")}
          </span>
        )}

        {character.faction && (
          <span
            className={`absolute bottom-3 right-4 z-10 text-sm ${
              character.faction === "Horde"
                ? "text-red-500"
                : character.faction === "Alliance"
                  ? "text-blue-400"
                  : "text-zinc-400"
            }`}
          >
            {character.faction}
          </span>
        )}
      </div>
    </section>
  );
}

function CharacterRender({ character }: { character: CharacterSummary }) {
  if (!character.renderUrl) {
    return (
      <div className="absolute inset-0 flex items-center justify-center">
        <p className="text-zinc-500">No render available.</p>
      </div>
    );
  }

  const bounds = character.renderBounds;
  if (!bounds || bounds.height <= 0) {
    // No measured bounds — fall back to a contained, centered render.
    return (
      <img
        src={character.renderUrl}
        alt={`${character.name} render`}
        className="absolute inset-0 m-auto max-h-full object-contain"
      />
    );
  }

  // Scale the render so the character's measured height fills the area between the
  // top padding and the bottom text zone, then offset so its top sits at the padding.
  const targetHeight = RENDER_FRAME_HEIGHT - RENDER_TOP_PADDING - RENDER_TEXT_ZONE;
  const imageHeight = targetHeight / bounds.height;
  const imageTop = RENDER_TOP_PADDING - bounds.top * imageHeight;

  return (
    <img
      src={character.renderUrl}
      alt={`${character.name} render`}
      style={{ height: `${imageHeight}px`, top: `${imageTop}px` }}
      className="absolute left-1/2 -translate-x-1/2 max-w-none"
    />
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
