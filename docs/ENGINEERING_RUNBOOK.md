# Engineering and release runbook

## Local development

Use Node 22, Java 21, Git and Docker Compose. Run these commands from the repository root in a shell that supports `set -a`. Choose a local-only database password in the copied environment file.

```sh
cp config/local-stack.env.example config/local-stack.env
docker compose --env-file config/local-stack.env -f Vetalya_backend/compose.local.yml up -d
set -a
. config/local-stack.env
set +a
(cd Vetalya_backend && ./mvnw spring-boot:run)
```

Start Vite in a separate terminal. The environment values below route browser traffic through the local Vite proxy.

```sh
cd Vetalaya_frontend
npm ci
VITE_API_BASE_URL=/api VITE_API_PROXY_TARGET=http://127.0.0.1:8080 npm run dev
```

For mobile, install dependencies in `apps/pet-owner-mobile`, use its env example, and start Expo. Android Emulator normally reaches the host through `10.0.2.2`. The iOS simulator can use `localhost`. Physical devices need a reachable host. Build-time web and Expo public settings can be read by clients. Store database passwords, signing keys and provider secrets in the backend environment.

- [Backend client settings](../config/backend-clients.env.example)
- [Web settings](../Vetalaya_frontend/.env.example)
- [Mobile settings](../apps/pet-owner-mobile/.env.example)

## Verification

```sh
(cd Vetalya_backend && ./mvnw test)
node --test Vetalaya_frontend/tests/*.test.mjs packages/api-client/tests/*.test.js packages/owner-domain/tests/*.test.js
(cd Vetalaya_frontend && npm ci && npm run build)
(cd apps/pet-owner-mobile && npm ci && npm run typecheck && npm run lint)
python3 deploy/check-staging.py
```

The latest recorded backend run for application commit `94de3a0` passed 114 tests. Staging readiness and public smoke checks passed for `93c9911`. Authenticated acceptance remains separate. Exercise OTP and resend, owner-only reads, cross-hospital denial, appointment contention, check-in, encounter signing, matching Passport shares and revoked grants with synthetic fixtures. Test new Flyway migrations on PostgreSQL as well as running the Java suite.

## Release workflow

| Environment | Current status |
| --- | --- |
| Local | Vite and Spring Boot against Compose PostgreSQL. Expo can point at the same API |
| Staging | Render deploys `staging/vetalya-internet-test` through [the staging Blueprint](../deploy/render.staging.yaml) and uses its dedicated Supabase database. The staging Spring configuration validates the schema, disables demo routes, and sets the JDBC pool maximum to four |
| Production | Review actual service configuration and schema state before a release. No production revision is asserted by this audit |
| Native | EAS profiles include `development`, `preview`, `staging` and `production`. Staging build settings point to the staging API. Native builds are on demand |

For staging, confirm the target branch, compare the intended commit with the previous live revision, and review migrations before triggering deployment. Monitor the intended Render deployment until live, then run the smoke script. Record the revision, deployment ID, test results and remaining acceptance checks. The recorded staging deployment for `93c9911` is `dep-dbruurh5efls73e5o9dg`.

For rollback, identify the last healthy deployment and check database compatibility before restoring the application revision. An application rollback does not reverse a Flyway migration or undo data writes. Re-run the staging smoke script and review application logs after recovery.

The proposed branch model is one protected `main`, short-lived feature branches, CI before automatic staging, and intentional promotion of a tested commit to production. Reconcile the staging and main histories and add CI before switching the deploy branch.

## Operational checks

- Readiness failing during rollout. Check Render build status, runtime logs, database connectivity and Flyway validation before recycling the service.
- Signup or OTP failure. Check server logs for the delivery provider and configured sender. Test using authorized staging recipients.
- Google sign-in failure. Verify the browser origin matches the registered web client. On Android, also verify the installed variant's package name and signing fingerprint.
- Hospital or Passport access denied. Check the account role, hospital assignment and recipient before checking the record category and expiry. Preserve the audit trail when changing access.

Use the [staging history](../deploy/STAGING.md), [Google and OTP notes](GOOGLE_AND_EMAIL_VERIFICATION.md), and [technical handbook](TECHNICAL_HANDBOOK.md) for details.

## Configuration ownership

| Setting | Where it belongs |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Spring Boot environment for the selected database |
| `JWT_PRIVATE_KEY_PEM`, `JWT_PUBLIC_KEY_PEM` | Backend signing configuration for the environment |
| `GOOGLE_CLIENT_ID` | Backend allowed Google web audience |
| `VITE_GOOGLE_CLIENT_ID`, `EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID` | Public client build configuration |
| `WEB_REDIRECT_URL`, `CORS_ALLOWED_ORIGINS` | Backend browser origin configuration. Check the effective Spring profile for overrides |
| `VITE_API_BASE_URL`, `VITE_API_PROXY_TARGET` | Web API prefix and local proxy target |
| `EXPO_PUBLIC_API_BASE_URL`, `APP_IDENTIFIER`, `APP_SCHEME`, `APP_VARIANT` | Mobile API target, app identity and callback configuration |
| `BREVO_API_KEY`, `SPRING_MAIL_FROM`, `SPRING_MAIL_*` | Backend delivery provider, sender and SMTP configuration |

Use one database per environment. Supply test credentials through the configured environment or designated test accounts.
