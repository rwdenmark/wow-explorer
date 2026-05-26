# wow-explorer

Local WoW character explorer. Spring Boot backend pulls from the Battle.net (Blizzard) API and Raider.IO; React frontend renders a single page with a name input, a US realm dropdown, and a character summary card with the official character render.

## Stack

- Backend: Java 21, Spring Boot 3.5, Maven, Spring Data JPA, Flyway, Caffeine cache
- Database: Postgres 16 (via docker-compose)
- Frontend: Vite + React 18 + TypeScript + Tailwind
- External APIs: Battle.net (OAuth Client Credentials), Raider.IO (no auth)

## Layout

```
wow-explorer/
├── docker-compose.yml         Postgres only
├── .env                       Local credentials (gitignored)
├── .env.example               Template
├── backend/                   Spring Boot
└── frontend/                  Vite + React + TS
```

## Prereqs

- Docker Desktop
- JDK 21 (use `java -version` to confirm)
- Maven 3.9+ (or use IntelliJ's bundled one)
- Node 20+

## Run it

```bash
# 1. From the project root: bring up Postgres
docker compose up -d

# 2. Backend (separate terminal)
cd backend
mvn spring-boot:run

# 3. Frontend (separate terminal)
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The page lands pre-populated with Zeuh / Proudmoore. Hit Search.

## How auth works

The backend exchanges your Client ID + Secret for a Battle.net OAuth access token (Client Credentials grant), caches it until ~60 seconds before expiry, and attaches it as a Bearer token on every API call. The token never reaches the frontend. To rotate, change the values in `.env` and restart the backend.

## What the page shows

- Character: Name Realm
- Item Level (equipped)
- Raid Progress (current-tier summary from Raider.IO, e.g. `8/8 H`)
- Raider IO (current-season Mythic+ overall score)
- Achievements (total points)
- Total Mounts
- Official Blizzard render image (the high-res `main-raw` asset)

If Raider.IO has no profile for the character (common for low-played characters), the page falls back to Blizzard-only data and shows `—` for the two Raider.IO fields.

## API endpoints

- `GET /api/realms` — sorted list of US realms (`{slug, name}`). Cached 24 h.
- `GET /api/characters/{realmSlug}/{name}` — `CharacterSummary` JSON. Cached 5 min per character.

## Cache + rate limit notes

Blizzard allows 100 req/sec and 36 000 req/hour per Client ID. Caffeine fronts every call so repeated lookups stay free. If you start running batch jobs against this, watch the rate ceiling and add Resilience4j retries.

## True 3D character viewer

The render currently shown is Blizzard's official 2D `main-raw` PNG — high resolution, official, no extra deps. If you want a rotatable 3D model later, embed Wowhead's Model Viewer (`https://wowhead.github.io/modelviewer.js/`) and feed it the character's `appearance` block from the Blizzard profile response. That swap touches `CharacterCard` in `frontend/src/App.tsx` only.

## Tests

```bash
cd backend
mvn test
```

`CharacterServiceTest` mocks `BlizzardClient` and `RaiderIoClient` and asserts the summary is assembled correctly, including the Raider.IO-missing fallback.

## Next steps (good learning exercises)

1. Parallelize the four Blizzard calls in `CharacterService.getSummary` with `CompletableFuture` (currently sequential, ~1 s).
2. Add a `/api/characters/recent` endpoint backed by `CharacterLookup`. Wire it to a "Recently viewed" panel.
3. Add user login with Battle.net (Authorization Code + PKCE) so a user can see their own characters.
4. Swap the static render for the Wowhead 3D Model Viewer.
5. Add Resilience4j circuit breakers around the Blizzard and Raider.IO clients.
