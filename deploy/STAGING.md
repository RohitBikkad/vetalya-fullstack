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
- Live application commit: 9349385b7d627b6e99be4d7f0a4b4c6bfe8c0b89
- Successful deploy: dep-dbr8f62vcj2c73eko8d0
- Database: existing Supabase staging project, V1–V14 checksums matched; normal Hibernate validation completed.
- Runtime uses IPv4 and TLS 1.2 explicitly for the Supabase session pooler. TLS remains required.
- Public HTTP checks verify UI entry points, logo/font assets, DB readiness, unauthenticated owner denial, and 404 responses for database inspection and privileged demo login.

Native preview builds use EAS profile `staging`; both Android and iOS receive this same backend URL. Native store binaries have not been built or submitted.

Future infrastructure changes should be planned/reviewed through the Blueprint before approval. Documentation/client-only configuration commits may use `[skip render]` to preserve the verified application deploy.

Repeat the credential-free internet smoke checks with `python3 deploy/check-staging.py`. They require non-empty React HTML for direct links, successful JS/CSS/font/logo assets, database readiness UP, owner API 401 without a session, and 404 for public DB inspection and privileged demo login. The browser check verified the rendered sign-in page after the direct-link regression fix. Authentication and clinical writes should be tested using your own staging test account; this release did not recover or guess account passwords.


## Account access update — 13 November 2026

Normal registration no longer depends on the disabled demo-login feature. The demo endpoint stays disabled. Pet-owner sign-in and registration use the existing vet portal’s cream canvas, compact white card, navy controls, and official branding on web and mobile.

Application commit `ced387de84a72a506952d8e7cf4adb70d36ecaaa` is live in deployment `dep-dbr9q6mtlv9c73fsv2qg`. The first rollout timed out before a replacement process started; one retry succeeded. The reduced-overhead JVM settings preserve required TLS and are retained in the Blueprint.

Validation: 29 focused backend tests passed; web production build and mobile TypeScript/lint passed. Live API tests created a dedicated pet-owner test account, accessed its empty pet list, rejected duplicate email and missing phone, and verified web cookie and Android/iOS token login behavior. Credentials are not committed. All public staging smoke checks passed, and the rendered registration page was verified in the browser.

Updated Android staging APK: https://expo.dev/accounts/rohitbikkad/projects/vetalya-pet-owner/builds/b82fafdf-edb1-4dcb-898e-9aecd5644dd2 . This build is complete; physical-device testing is still pending. iOS source is updated, but a signed iOS build still requires Apple signing setup.


## Google and email verification release — 13 November 2026

Live application: `c85cfbc192f9e08a64048db53fb3e4fa19130d24`; successful deploy: `dep-dbrks8ijnfac73ar9hcg`. Google client IDs on main’s deployed web app and staging were compared and match. Android uses the same server web client ID with native Google Sign-In. No iOS authentication build was requested.

Brevo SMTP settings from main were applied only to staging’s email configuration. Port 2525 supports required STARTTLS and certificate hostname checking. The provided database, CORS, port and production-only profile values were not bulk-applied. Mail credentials and Google secrets are not stored in this repository or bundled in public clients.

Flyway V15 applied successfully; OTP RLS remains enabled. Live SMTP dispatch to the user-selected test address succeeded. Codes are stored as hashes; the send API does not expose them. The user deferred entering the code, so manual receipt verification remains pending. Google account selection on a physical Android device also remains a manual release check; do not interpret build success as proof of that interaction.

Validation passed: 36 focused backend tests, 15 shared JavaScript tests, web production build, mobile TypeScript/lint, Android bundle export and native prebuild/autolinking. Live API probes reject missing email proof, invalid codes and forged Google tokens, while preserving existing owner login. Final public staging smoke checks passed.

Google-enabled Android APK build is finished: https://expo.dev/accounts/rohitbikkad/projects/vetalya-pet-owner/builds/f6b7d226-a5a2-4a17-aeed-12274d7a6cc4 . Its native source matches `947fc5b`; subsequent commits affect only server/web behavior or documentation. Install this APK instead of earlier password-only builds. The earlier test email code expires after ten minutes; request a fresh code when testing registration.

Configuration and Google Cloud package/certificate details: `docs/GOOGLE_AND_EMAIL_VERIFICATION.md`. Credentials shared in chat should be rotated through the relevant providers and updated securely in Render; rotation was not performed as part of this release.
