# Quick Setup

## 1) Install dependencies

```bash
cd backend-node
npm install

cd ../frontend-rn
npm install
```

## 2) Expo (one-time)

```bash
npm install -g expo-cli
npx expo --version
```

If you prefer no global install, just use `npx expo ...` commands.

## 3) Env setup

```powershell
cd frontend-rn
Copy-Item .env.example .env
```

Edit `frontend-rn/.env` and set at least:

```env
EXPO_PUBLIC_API_BASE_URL=http://localhost:8080
EXPO_PUBLIC_API_FALLBACK_URL=http://localhost:8080
```

## 4) Run app

Terminal 1:

```bash
cd backend-node
npm run dev
```

Terminal 2:

```bash
cd frontend-rn
npx expo start
```