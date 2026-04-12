# Frontend RN Production Guide

This Expo app is now structured with persisted state, service-layer API boundaries, and runtime recovery safeguards.

## Stack
- Expo SDK 54
- React Native 0.81
- React Navigation (bottom tabs)
- AsyncStorage persistence
- Camera + audio interview capture
- Login/signup auth gate with bearer session handling

## Environment
Copy `.env.example` to `.env` (or export env vars in CI):

```bash
EXPO_PUBLIC_APP_ENV=production
EXPO_PUBLIC_USE_MOCKS=false
EXPO_PUBLIC_API_BASE_URL=http://YOUR_LAN_IP:8080
EXPO_PUBLIC_API_TIMEOUT_MS=12000
```

This file is also the backend env source for the unified stack. Add backend keys in the same file:

```bash
PORT=8080
CORS_ORIGIN=*
GROQ_API_KEY=your_groq_api_key_here
GROQ_MODEL=llama-3.1-8b-instant
```

Notes:
- `EXPO_PUBLIC_USE_MOCKS=true` keeps local fallback behavior for coding sync and interview scoring.
- Set `EXPO_PUBLIC_USE_MOCKS=false` with a valid API base URL for real backend mode.
- This `.env` file is the single source for both frontend and backend in unified mode.

Run the unified Node backend from the workspace root:

```bash
cd backend-node
npm install
npm run dev
```

## ngrok (Recommended When Network Is Unstable)

1. Authenticate ngrok once:

```bash
npx ngrok config add-authtoken YOUR_NGROK_AUTHTOKEN
```

2. Tunnel backend port 8080:

```bash
npx ngrok http 8080
```

If Expo tunnel is already running, you can also auto-create the backend tunnel and sync `.env` in one command:

```bash
npm run api:prepare-ngrok
```

3. Sync `.env` automatically to the active backend tunnel:

```bash
npm run api:sync-ngrok
```

This updates:
- `EXPO_PUBLIC_API_BASE_URL` to the active tunnel bound to `localhost:8080`
- `EXPO_PUBLIC_API_FALLBACK_URL` to `http://localhost:8080`

If you prefer manual update, set `.env` to the generated public URL:

```bash
EXPO_PUBLIC_API_BASE_URL=https://your-tunnel.ngrok-free.app
```

4. Restart backend and Expo.

Avoid relying on `expo --tunnel` when ngrok infra is unstable; use Expo LAN + backend ngrok tunnel.

## Local Development
```bash
npm install
npm run typecheck
npm run start
```

## iOS / Android
```bash
npm run ios
npm run android
```

## Production Builds (EAS)
`eas.json` is included with development, preview, and production profiles.

```bash
npm install -g eas-cli
eas login
eas build --platform ios --profile production
eas build --platform android --profile production
```

## Backend Contract
Expected endpoints:
- `POST /auth/signup`
  - request: `{ name, email, password }`
  - response: `{ token, user }`
- `POST /auth/login`
  - request: `{ email, password }`
  - response: `{ token, user }`
- `GET /auth/me`
  - headers: `Authorization: Bearer <token>`
  - response: `{ user }`
- `POST /coding/sync`
  - request: `{ leetCodeHandle, codeforcesHandle }`
  - response: `{ solved, mediumHard, rating, depth, status }`
- `POST /interview/evaluate`
  - request: `{ domain, difficulty, prompt, durationSec, audioUri }`
  - response: `{ score, feedback, rubric, strengths, improvements }`
- `POST /opportunities/feed`
  - request: `{ skills }`
  - response: `OpportunityRecord[]`
- `GET /state/bootstrap`
  - response: `{ profile, coding, opportunities, applications, interview, activityLog, analytics, readiness, roadmapTasks }`
- `POST /state/save`
  - request: `{ profile, coding, opportunities, applications, interview, activityLog }`

## Reliability Features Added
- App-wide runtime fallback with error boundary.
- Startup hydration gate for persisted state/theme before navigation renders.
- Theme preference persistence.
- Profile/coding/interview state persistence.
- Async interview evaluation pipeline with fallback scoring when API is unavailable.

## Release Checklist
- Set production bundle IDs/package names in `app.json`.
- Configure real API URL and disable mocks.
- Run `npm run typecheck` in CI.
- Build and test both iOS and Android release profiles.
- Verify camera/mic permission prompts and recording flow on physical devices.
