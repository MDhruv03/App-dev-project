# OpportunityHub Node Backend

Unified backend for the React Native app and migrated Java feature logic.

## Features
- Email/password signup + login with bearer sessions
- User-specific state isolation (profile, coding, applications, interview, activity)
- Interview evaluation endpoint with Groq AI + local fallback heuristics
- Real-time vision validation proxy endpoint for interview camera checks
- Coding profile sync (LeetCode + Codeforces) with resilient fallback
- Opportunity feed ranking and personalization
- Profile, applications, and activity persistence
- Analytics and roadmap generation endpoints
- Full app state bootstrap/save APIs for frontend sync

## Run
1. Install dependencies:

```bash
cd backend-node
npm install
```

2. Configure env:

```bash
cd ../frontend-rn
cp .env.example .env
cd ../backend-node
```

Backend and frontend now share a single env file: `frontend-rn/.env`.

3. Optional (recommended for unstable networks): tunnel backend with ngrok

```bash
npx ngrok config add-authtoken YOUR_NGROK_AUTHTOKEN
npx ngrok http 8080
```

From `frontend-rn`, run:

```bash
npm run api:prepare-ngrok
```

or, if tunnel already exists:

```bash
npm run api:sync-ngrok
```

This updates `EXPO_PUBLIC_API_BASE_URL` in `frontend-rn/.env` to the active tunnel URL.

4. Start server:

```bash
npm run dev
```

Server defaults to `http://localhost:8080`.

`npm run dev` now runs through a managed launcher that:
- frees port `8080` before start (kills stale listeners on that port)
- cleans up the watcher process tree on Ctrl+C

If you ever need to force-clear the backend port manually:

```bash
npm run stop
```

For raw watch mode without port management:

```bash
npm run dev:raw
```

## Core Endpoints
- `GET /health`
- `POST /vision/validate`
- `POST /auth/signup`
- `POST /auth/login`
- `GET /auth/me`
- `POST /auth/logout`
- `POST /coding/sync`
- `POST /interview/evaluate`
- `POST /opportunities/feed`
- `GET /state/bootstrap`
- `POST /state/save`
- `GET/PATCH /profile`
- `GET/POST/PATCH/DELETE /applications`
- `GET /analytics`
- `GET /roadmap`
- `GET /activity`

## Notes
- Uses SQLite at `data/app.db` for persistence.
- Reads `GROQ_API_KEY` and runtime settings from `frontend-rn/.env`.
- Safe fallback logic keeps app functional if external APIs are unavailable.
- Vision validation is proxied to `VISION_SERVICE_URL` (default: `http://localhost:5001/validate-face`).
