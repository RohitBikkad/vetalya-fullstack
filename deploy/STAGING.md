# Vetalya persistent staging

Target workspace: vetalya (`tea-dboq70psrm7s73cp8r5g`). Supabase: existing dedicated test project `lihsmcgfxjuldjenjdti` (vetalya-db). No Render Postgres instance is needed.

## Architecture

One Docker web service serves the built React/Vite frontend and Spring Boot `/api` on one HTTPS origin. Web API base remains `/api`; Android/iOS preview builds set `EXPO_PUBLIC_API_BASE_URL` to the returned public staging URL plus `/api`. Core signing keys are environment secrets and persist across redeploys. Source development keys are excluded from the staging image.

`deploy/render.staging.yaml` describes the service. Its `free` compute plan can sleep while idle; it is not an always-on SLA. The URL and Supabase data persist. A paid always-on upgrade requires a separate budget decision.

## Required Render secrets

- `DB_PASSWORD`: Supabase database password, entered in Render's Environment page, never in chat or Git.
- `JWT_PRIVATE_KEY_PEM` / `JWT_PUBLIC_KEY_PEM`: fresh staging RSA key pair stored in Render environment secrets.
- `MANAGEMENT_SECURITY_PASSWORD`: fresh random value.
- `WEB_REDIRECT_URL`: actual Render service origin.

Database host/username use Supabase's session pooler on port 5432. The backend uses JDBC, TLS and a small Hikari pool. Existing Flyway V1–V14 history is inspected before deployment; schema validation remains enabled. No migration repair, checksum rewrite or data reset is permitted.

## Staging protections

`prod,staging` profile serves the UI and uses real database readiness checks. Demo seeding, unauthenticated demo login, public database table inspection, API docs and outgoing SMS/WhatsApp/email providers are disabled. Google sign-in needs environment-specific provider configuration; email/password remains the initial test path. No file upload persistence or payment integrations are claimed.

The user approved `deploy/sql/staging-api-access.sql`; it was applied through Supabase migration `secure_staging_backend_tables`. All 49 public/medshop application tables now have RLS enabled and no API policies. Spring Boot's database-owner connection is retained; direct Supabase anon/authenticated clients cannot access application rows. Supabase auth/storage system schemas are unchanged.

## Release and verification

A dedicated staging branch carries the current app snapshot and deployment files. Existing main-linked services are not changed. After the staging deploy becomes live, check `/`, `/owner/login`, frontend assets, `/api/auth/health`, and `/actuator/health/readiness`. Confirm `/api/public/db-inspect` and `/api/auth/demo-login` return 404. Then use a normal test account to verify pet records, booking and sharing through the public URL.

Changing code, environment or database state is not proof of a successful deploy. Record the returned Render deploy ID and wait for live status and HTTP checks before reporting an internet test URL as ready.

## Verified live deployment

- URL: https://vetalya-staging-z0u9.onrender.com
- Render service: srv-dbr7s4d6e07s73aqsq20
- Blueprint: exs-dbr7k3tg1s2s73fltvlg (automatic infrastructure sync is off; application branch auto-deploy is on).
- Live application commit: b04a70a9e4f96c429382390933c16565f671a8ae
- Successful deploy: dep-dbr85nl6e07s73arqtk0
- Database: existing Supabase staging project, V1–V14 checksums matched; normal Hibernate validation completed.
- Runtime uses IPv4 and TLS 1.2 explicitly for the Supabase session pooler. TLS remains required.
- Public HTTP checks verify UI entry points, logo/font assets, DB readiness, unauthenticated owner denial, and 404 responses for database inspection and privileged demo login.

Native preview builds use EAS profile `staging`; both Android and iOS receive this same backend URL. Native store binaries have not been built or submitted.

Future infrastructure changes should be planned/reviewed through the Blueprint before approval. Documentation/client-only configuration commits may use `[skip render]` to preserve the verified application deploy.
