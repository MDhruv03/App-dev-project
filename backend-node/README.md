# OpportunityHub Node Backend

Unified backend for the React Native app and migrated Java feature logic.

## Features
- Interview evaluation endpoint with Groq AI + local fallback heuristics
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

Set `EXPO_PUBLIC_API_BASE_URL` in `frontend-rn/.env` to the generated ngrok URL.

4. Start server:

```bash
npm run dev
```

Server defaults to `http://localhost:8080`.

## Core Endpoints
- `GET /health`
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
- Uses `data/store.json` for persistence.
- Reads `GROQ_API_KEY` and runtime settings from `frontend-rn/.env`.
- Safe fallback logic keeps app functional if external APIs are unavailable.
