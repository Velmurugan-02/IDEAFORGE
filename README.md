# IdeaForge

**Tagline:** Protect, validate, and battle startup ideas — with plagiarism workflows, live battles, and gamified reputation.

IdeaForge is a full-stack platform where founders post ideas, the community votes and comments, ideas enter weekly **Hall of Fame** and **Battle Arena** matchups, and **admins** moderate plagiarism reports and schedule battles.

---

## Features

| Area | Highlights |
|------|------------|
| **Ideas** | Public / private / invite-only ideas, traction score, duplicate check, certificates |
| **Social** | Votes, threaded comments, collaboration requests, WebSocket notifications |
| **Battles** | Admin-created head-to-head public battles, live vote totals, winners & XP |
| **Trust** | Plagiarism reports, admin review (confirm / dismiss), flagged ideas |
| **Gamification** | XP, levels, streaks, badges, leaderboard |
| **Admin** | Pending reports, battle creation, platform stats dashboard |

---

## Tech stack

| Layer | Technology |
|-------|------------|
| **Frontend** | React 18, React Router, Axios, SockJS + STOMP, react-hot-toast |
| **Backend** | Java 17, Spring Boot 3, Spring Security (JWT), Spring WebSocket |
| **Database** | MySQL 8 |
| **Deploy** | Docker (API on Render), static SPA (Vercel) |

---

## Screenshots

_Add screenshots here after deployment (e.g. feed, battle arena, admin console, profile)._

```text
docs/screenshots/
  ├── feed.png
  ├── battle.png
  ├── admin.png
  └── profile.png
```

---

## Live demo

- **Frontend:** _add your Vercel URL after deploy_  
- **API:** _add your Render URL after deploy_  

---

## Local development

### Prerequisites

- **JDK 17**, **Maven 3.9+**
- **Node.js 18+** and npm  
- **MySQL 8** with an empty database (e.g. `ideaforge_db`)

### Backend (`ideaforge-backend`)

1. Create the database and set env vars (or edit `src/main/resources/application.properties` for local only):

   | Variable | Purpose |
   |----------|---------|
   | `DB_URL` | JDBC URL, e.g. `jdbc:mysql://localhost:3306/ideaforge_db` |
   | `DB_USERNAME` | MySQL user |
   | `DB_PASSWORD` | MySQL password |
   | `JWT_SECRET` | Long random string (used for signing JWTs) |
   | `CLAUDE_API_KEY` | Optional, for future AI features |
   | `CORS_ORIGINS` | Optional comma-separated extra origins |

2. Run:

   ```bash
   cd ideaforge-backend
   mvn spring-boot:run
   ```

   API defaults to **http://localhost:8080** (`/api/...`).

3. **Docker image** (after a local JAR build):

   ```bash
   mvn -q -DskipTests package
   docker build -t ideaforge-api .
   docker run -p 8080:8080 \
     -e DB_URL=jdbc:mysql://host.docker.internal:3306/ideaforge_db \
     -e DB_USERNAME=root \
     -e DB_PASSWORD=secret \
     -e JWT_SECRET=change-me-to-a-long-random-string-at-least-32-chars \
     ideaforge-api
   ```

### Frontend (`ideaforge-frontend`)

1. Install and start:

   ```bash
   cd ideaforge-frontend
   npm install
   npm start
   ```

2. Optional `.env.development.local`:

   ```env
   REACT_APP_API_URL=http://localhost:8080/api
   REACT_APP_WS_URL=http://localhost:8080/ws
   ```

3. Production build:

   ```bash
   npm run build
   ```

   Uses `.env.production` for `REACT_APP_*` values when present.

---

## Deploying

### Backend (Render.com)

1. Connect the repo; **Root directory:** `ideaforge-backend`.
2. **Build:** `mvn -q -DskipTests package`  
   **Start:** `java -jar target/*.jar` (or use the provided `Dockerfile` with **Docker** runtime).
3. Set environment variables: **`DB_URL`**, **`DB_USERNAME`**, **`DB_PASSWORD`**, **`JWT_SECRET`**, **`CLAUDE_API_KEY`** (optional), **`PORT`** (Render sets this automatically).
4. Ensure MySQL is reachable from Render (managed DB or allowlisted host).

### Frontend (Vercel)

1. Push to GitHub; import the repo in Vercel with **root** `ideaforge-frontend`.
2. Set **Environment variables** to match production API:

   - `REACT_APP_API_URL` = `https://<your-render-service>.onrender.com/api`  
   - `REACT_APP_WS_URL` = `wss://<your-render-service>.onrender.com/ws`

3. Redeploy after changing env vars.

4. Add your Vercel origin to backend CORS (`CORS_ORIGINS` or rely on built-in `https://*.vercel.app` pattern in `SecurityConfig`).

---

## First admin

With no admins in the database, call **`POST /api/admin/setup`** once while authenticated to promote the current user to `ROLE_ADMIN`. After that, use **`/admin`** in the app (visible in the profile menu).

---

## License

Proprietary / your choice — update this section for your team.
