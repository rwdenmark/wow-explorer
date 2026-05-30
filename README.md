# wow-explorer

[![Backend CI](https://github.com/rwdenmark/wow-explorer/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/rwdenmark/wow-explorer/actions/workflows/backend-ci.yml)
[![Frontend CI](https://github.com/rwdenmark/wow-explorer/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/rwdenmark/wow-explorer/actions/workflows/frontend-ci.yml)

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

## IDE setup (IntelliJ)

The Maven project lives in `backend/`, not at the repo root. If you open the **root**
`wow-explorer/` folder, IntelliJ creates a bare generic module and never imports
`backend/pom.xml` — so no dependencies are on the classpath, symbols don't resolve, and the
editor shows methods/classes/variables in plain white (only keywords/strings get colored).
You may also see a "Cannot load N facets" error.

Fix it one of two ways:

- **Monorepo (keeps `frontend/` visible):** right-click `backend/pom.xml` in the Project tree
  → **Add as Maven Project**.
- **Backend only:** open IntelliJ directly on `backend/pom.xml` → **Open as Project**.

Verify under **Project Structure → Modules**: you should see `wow-explorer-backend` with
libraries attached, not an empty module.

## Prereqs

- Docker Desktop
- JDK 21 (use `java -version` to confirm)
- Maven 3.9+ (or use IntelliJ's bundled one)
- Node 20+

## Run it

First time only: copy `.env.example` to `.env` and fill in your Battle.net Client ID + Secret.

The backend starts Postgres for you: on startup it runs `docker compose up`, waits for the
healthcheck, loads `.env`, and connects — via Spring Boot's docker-compose support
(`spring-boot-docker-compose`) plus `spring.config.import` of the repo-root `.env`. So you
do **not** need a separate `docker compose up -d` or to export `.env` by hand.

**One command (Windows):**

```powershell
.\start.ps1
```

Brings up Postgres, the backend (`mvn spring-boot:run`), and the frontend, each in its own
window. Flags: `-SkipFrontend`, `-SkipBackend`.

**Or run the pieces yourself:**

```bash
# Backend — from your IDE (run WowExplorerApplication) or the CLI.
# Postgres + .env are handled automatically; Docker Desktop must be running.
cd backend
mvn spring-boot:run

# Frontend (separate terminal)
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The page lands pre-populated with Zeuh / Proudmoore. Hit Search.

> Postgres is left running when the backend stops (`lifecycle-management: start-only`). Stop it
> with `docker compose down`. To opt out of the auto-start, set `spring.docker.compose.enabled: false`.

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

## Tests

```bash
cd backend
mvn test
```

`CharacterServiceTest` mocks `BlizzardClient` and `RaiderIoClient` and asserts the summary is assembled correctly, including the Raider.IO-missing fallback.

## Deploying

See [DEPLOY.md](DEPLOY.md) for the Render + Neon free-tier setup.
