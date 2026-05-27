# Deploying for free (Render + Neon)

This app deploys as a **single service**: the `Dockerfile` builds the React SPA, bakes it
into the Spring Boot jar, and Spring serves both the UI (`/`) and the API (`/api/**`) from
one origin. Hosting plan:

- **App** → [Render](https://render.com) free Web Service (Docker).
- **Database** → [Neon](https://neon.tech) free serverless Postgres (stores the
  "Recently viewed" history; the rest is in-memory cache).

Both are free. Expect a **cold start** (~30–60 s) when the app wakes after ~15 min idle —
normal for Render's free tier.

---

## 1. Create the database (Neon)

1. Sign up at neon.tech and create a project (any region near you).
2. On the dashboard, open **Connection Details** and copy the connection string. It looks like:
   ```
   postgresql://USER:PASSWORD@ep-xxxx.us-east-2.aws.neon.tech/neondb?sslmode=require
   ```
3. You'll split this into three values for Render below. The JDBC URL is the same string
   with a `jdbc:` prefix and the credentials removed:
   ```
   jdbc:postgresql://ep-xxxx.us-east-2.aws.neon.tech/neondb?sslmode=require
   ```

Flyway creates the `character_lookup` table automatically on first boot (`V1__init.sql`).

## 2. Deploy the app (Render)

1. Push this repo to GitHub (Render deploys from a repo).
2. In Render: **New → Web Service** → connect the repo.
3. Settings:
   - **Runtime:** Docker
   - **Dockerfile Path:** `./Dockerfile`
   - **Docker Build Context Directory:** `.` (repo root — the build needs both `frontend/`
     and `backend/`)
   - **Instance Type:** Free
4. Add **Environment Variables** (Render → Environment):

   | Key | Value |
   |---|---|
   | `BLIZZARD_CLIENT_ID` | your Battle.net client id |
   | `BLIZZARD_CLIENT_SECRET` | your Battle.net client secret |
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://ep-xxxx...neon.tech/neondb?sslmode=require` |
   | `SPRING_DATASOURCE_USERNAME` | Neon user |
   | `SPRING_DATASOURCE_PASSWORD` | Neon password |

   `PORT` and `SPRING_DOCKER_COMPOSE_ENABLED` are handled for you (Render sets `PORT`; the
   Dockerfile disables the local Postgres auto-start).
5. **Create Web Service.** First build takes a few minutes (Maven + npm). When it's live,
   open the Render URL — the page should load and Search should work.

## 3. Restrict the Battle.net app (optional but recommended)

In the [Battle.net developer portal](https://develop.battle.net), the Client Credentials
grant doesn't use redirect URLs, so there's nothing to whitelist. Just keep the secret in
Render's env vars — never commit it.

---

## Notes & limits

- **Cold starts:** free Render services sleep after ~15 min idle. The first request after
  that waits for the JVM to boot. To keep it warm you'd need a paid instance or an external
  pinger (against Render's free-tier terms if abused — fine for occasional use).
- **Memory:** the free instance is 512 MB; the Dockerfile caps the heap at 75% of that.
- **Neon free tier:** ~0.5 GB storage and autosuspend on idle — plenty for a lookup log.
- **Rotating the Battle.net secret:** if your secret has ever been shared or committed,
  regenerate it in the Battle.net portal and update the Render env var.

## Updating

Push to the deployed branch → Render rebuilds and redeploys automatically.
