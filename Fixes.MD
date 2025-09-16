## Terms & Conditions Analyser — Fixes and Deployment Guide

This document summarizes all fixes we implemented, why they were needed, and how the frontend and backend are now deployed and working together. It also documents the expected 404 page at the backend root URL.

### What was fixed

- **Docker build error (missing static copy target)**
  - Removed the extra `COPY` step that attempted to copy `target/classes/static/dist/` into the runtime image because that path does not exist at build time in the final stage.
  - File edited: `backend/Dockerfile`
  - Effect: Backend image builds cleanly on Render using the Spring Boot fat JAR, which already contains any static resources packaged under `BOOT-INF/`.

- **Frontend calling localhost in production**
  - Ensured the frontend points to the Render backend in production without requiring extra env overrides.
  - File edited: `frontend/src/App.jsx`
  - Logic now:
    - Uses `import.meta.env.VITE_API_BASE_URL` if provided.
    - Otherwise, in production, defaults to `https://terms-conditions-analyser-zrfr.onrender.com`.
    - In local dev, defaults to `http://localhost:8080`.

- **CORS hardcoding removed and made environment-driven**
  - Removed hardcoded `@CrossOrigin` from `HealthController` to avoid conflicting with global CORS policy.
  - File edited: `backend/src/main/java/com/example/demo/controller/HealthController.java`
  - Centralized CORS configuration in `WebConfig` and switched to `allowedOriginPatterns` so wildcard patterns work.
  - File edited: `backend/src/main/java/com/example/demo/config/WebConfig.java`
  - Environment variable used on Render: `FRONTEND_URL`
    - Supports comma-separated values and wildcard patterns (e.g., `https://*.vercel.app`).

- **Vercel build/deploy alignment**
  - `frontend/vercel.json` sets the dist directory and can optionally define `VITE_API_BASE_URL` at build time. We also used CLI `--build-env` for explicit overrides when needed.

### Current architecture and URLs

- **Frontend (Vercel)**
  - Latest deployed URL (example):
    - `https://terms-conditions-frontend-n63hvk861.vercel.app`
  - Frontend calls backend API using the base URL resolved in `App.jsx`:
    - `VITE_API_BASE_URL` if set
    - else Render URL in prod
    - else `http://localhost:8080` in dev

- **Backend (Render)**
  - Primary URL:
    - `https://terms-conditions-analyser-zrfr.onrender.com`
  - API endpoints (examples):
    - Health: `/api/health/status`
    - Stats: `/api/documents/stats`
    - Upload: `/api/documents/upload`
    - Paste: `/api/documents/paste`
    - Clauses: `/api/documents/{documentId}/clauses`

### Why you see a 404 at backend root

- The backend is a JSON API service and does not serve a web index page at `/`. Visiting the backend root URL directly returns the Spring Boot Whitelabel 404 page. This is normal and expected.
  - Example:
    - Visiting: `https://terms-conditions-analyser-zrfr.onrender.com/` → Whitelabel Error Page (404)
    - Visiting: `https://terms-conditions-analyser-zrfr.onrender.com/api/health/status` → JSON status (expected)

### Required environment variables

- **Render (backend)**
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
  - `SPRING_JPA_HIBERNATE_DDL_AUTO=update`
  - `FRONTEND_URL` (comma-separated; supports wildcards via `allowedOriginPatterns`), e.g.:
    - `https://terms-conditions-frontend-n63hvk861.vercel.app,https://*.vercel.app`

- **Vercel (frontend)**
  - Optional: `VITE_API_BASE_URL` to explicitly point to a backend.
  - If not set, the app falls back to the Render URL in production and `http://localhost:8080` during local development.

### Deployment workflows

- **Frontend (Vercel CLI)**
  - From `frontend/`:
    ```bash
    npx vercel deploy --prod --yes
    ```
  - Optional override during build:
    ```bash
    npx vercel deploy --prod --yes --build-env VITE_API_BASE_URL=https://terms-conditions-analyser-zrfr.onrender.com
    ```

- **Backend (Render)**
  - Service type: Web (Docker)
  - Root dir: `backend`
  - Trigger “Clear build cache & Deploy” after changing Dockerfile or env vars.

### Local development

- Start backend (from `backend/`):
  ```bash
  ./mvnw spring-boot:run
  ```

- Start frontend (from `frontend/`):
  ```bash
  npm install
  npm run dev
  ```
  The frontend will use `http://localhost:8080` by default when running in dev.

### Troubleshooting tips

- **CORS 403 with Vercel preview/production URLs**
  - Ensure `FRONTEND_URL` on Render includes the current Vercel deployment URL(s) and optionally `https://*.vercel.app`.
  - Global CORS is configured in `WebConfig` using `allowedOriginPatterns`.

- **Frontend still hitting localhost in prod**
  - Confirm the new deployment is loaded (hard refresh, clear cache).
  - Optionally provide `VITE_API_BASE_URL` at build time on Vercel.

- **Root 404 on backend**
  - Expected; use the `/api/...` endpoints to test backend functionality.

### Summary of key edits

- `backend/Dockerfile`: removed invalid static `COPY` step.
- `backend/src/main/java/com/example/demo/controller/HealthController.java`: removed `@CrossOrigin`.
- `backend/src/main/java/com/example/demo/config/WebConfig.java`: centralized CORS; switched to `allowedOriginPatterns` with env-driven origins.
- `frontend/src/App.jsx`: production fallback to Render backend; respects `VITE_API_BASE_URL`.
- `frontend/vercel.json`: keeps Vercel static build config; can carry envs.

The application is now stable for both local development and cloud deployment (Vercel + Render), with predictable API routing and environment-driven CORS.


