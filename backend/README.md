# Vasan AI Studio — Node.js + Express Backend

Secure server-side proxy for **Kling AI**, **MiniMax Hailuo AI**, **Google Veo**, and **Runway AI**.
API keys are strictly kept on this backend and never embedded in the Android APK.

## Setup

1. Install dependencies:
   ```bash
   cd backend
   npm install
   ```

2. Configure environment variables:
   ```bash
   cp .env.example .env
   ```
   Fill in the API keys for the video models you want to enable:
   - `KLING_ACCESS_KEY` & `KLING_SECRET_KEY`
   - `MINIMAX_API_KEY`
   - `GOOGLE_VEO_API_KEY`
   - `RUNWAY_API_KEY`
   - `FIREBASE_PROJECT_ID=gen-lang-client-0693753268`

3. Run locally:
   ```bash
   npm start
   ```
   For Android Emulator testing, set the Backend URL in the app's **Settings** screen to:
   `http://10.0.2.2:3000`

4. Deploy to Cloud Run (for physical Android devices):
   ```bash
   gcloud run deploy vasan-ai-studio-backend --source . --region asia-northeast1 --allow-unauthenticated
   ```
   Then paste your HTTPS Cloud Run URL into **Settings -> Video Backend Server URL** inside the Android app.
