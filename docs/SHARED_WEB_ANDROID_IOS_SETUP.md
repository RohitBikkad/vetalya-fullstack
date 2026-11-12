# One backend for web, Android and iOS

All clients use the existing Spring Boot API and database. Pet profiles, passports, appointments, users, roles and hospital memberships remain shared. No mobile-only database or duplicate backend was introduced. The React/Vite web portal remains in place.

## Implemented configuration

- Password login, registration, OTP login and Google sign-in accept `clientType`: `web`, `android` or `ios`. Omission preserves web behavior. This controls token delivery, not roles or hospital access.
- Browser refresh tokens are sent through HttpOnly cookies. Native clients receive rotated refresh tokens in JSON and submit them in refresh/logout bodies without browser cookies. Access tokens are Bearer JWTs on every platform.
- Native Google sign-in records the actual client platform. Native Apple sign-in is iOS. `deviceId` remains metadata, not proof of device identity.
- Authorization-code exchange locks the code while consuming it, rejects replay and sets no cookie for native clients. Mobile redirects carry short-lived codes rather than session tokens.
- Public social registration cannot create staff/admin accounts. Existing provisioned users retain their database role when signing in.
- The main API and MedShop use an exact browser-origin allowlist. Broad hosting-provider wildcard origins were removed. Mobile schemes and package IDs are not browser CORS origins.
- Vite uses the configured API URL or same-origin `/api`. OpenAPI uses the current server or `API_PUBLIC_URL`; neither guesses a production backend hostname.
- `packages/api-client` provides optional framework-independent fetch transport with platform-specific token handling, shared concurrent refresh, backend error details, empty success responses, offline session preservation and local logout clearing. Existing web API functions remain intact.

## Environment settings

Start with [backend client settings](../config/backend-clients.env.example), [web settings](../Vetalaya_frontend/.env.example) and [mobile settings](../apps/pet-owner-mobile/.env.example).

| Setting | Location | Value / purpose |
| --- | --- | --- |
| `API_PUBLIC_URL` | Backend | `https://api.example.com` for OpenAPI |
| `VITE_API_BASE_URL` | Web build | `https://api.example.com/api`, or `/api` with a same-origin reverse proxy |
| `EXPO_PUBLIC_API_BASE_URL` | Mobile build | The same `https://api.example.com/api` |
| `CORS_ALLOWED_ORIGINS` | Backend | Comma-separated exact WEB origins, e.g. `https://app.example.com,http://localhost:3000` |
| `WEB_REDIRECT_URL` | Backend | Existing web application's callback origin |
| `MOBILE_REDIRECT_SCHEME` | Backend | Must match the mobile `APP_SCHEME`; default `myvetapp` |
| `APP_IDENTIFIER` | Mobile build | Your registered base application identifier; starter `com.vetalya.petowner` |
| `GOOGLE_CLIENT_ID` | Backend | Registered Google web client ID |
| `GOOGLE_ANDROID_CLIENT_ID` | Backend | Registered Android client ID |
| `GOOGLE_IOS_CLIENT_ID` | Backend | Registered iOS client ID |
| `APPLE_BUNDLE_ID` | Backend | Actual iOS bundle ID for this environment |
| `APPLE_SERVICES_ID` | Backend | Apple web services ID |

`CORS_ALLOWED_ORIGIN` remains a compatible fallback; the plural variable takes precedence. Configure every existing hosted web origin explicitly before deploying: no trailing slash, paths or wildcards. Native apps do not require CORS entries. CORS does not replace token, ownership or hospital authorization.

Keep DB credentials, OAuth secrets, signing keys and messaging/service credentials in the backend environment. Existing repository development credential defaults must not be used for production. Public web/mobile build variables are bundled into client applications and must contain no secrets.

Deploy backend and frontend together. Continue the existing Flyway process and apply V14 if the hospital workspace migration has not run. This cross-platform configuration adds no new database tables. [Hospital release notes](VET_STAFF_RELEASE_NOTES.md) cover initial clinic-admin provisioning, active staff assignments and the remaining hospital roadmap.

## Android and iOS application

[apps/pet-owner-mobile](../apps/pet-owner-mobile) now contains a runnable Expo SDK 58 / Expo Router owner application, SDK-compatible dependencies and lockfile, native screens, shared API contracts and a SecureStore session adapter. Use its [run instructions](../apps/pet-owner-mobile/README.md).

Set the API URL, registered app identifier, callback scheme and provider IDs. Rebuild when native IDs or schemes change. `APP_VARIANT` distinguishes development/preview/production install IDs; coexisting variants need distinct schemes and matching backend environment settings.

Email/password sign-in and registration are implemented. Native Google/Apple provider UI and callback lifecycle remain pending. Before integrating them, register provider console IDs/signing fingerprints and validate scheme, host and path before exchanging callback codes. Signing credentials and store submissions have not been created.

Local addresses: Android emulator `http://10.0.2.2:8080/api`; iOS simulator `http://localhost:8080/api`; physical phone uses the computer's reachable LAN IP or an HTTPS development endpoint. A phone's localhost is the phone itself. Local HTTP may require development-only OS transport settings. The shared client rejects HTTP in production; production ATS/network policies should remain strict.

Preserve existing Vetalya logo assets and palette when implementing native screens. No visual brand assets were changed.

## Verification and remaining work

Run `node --test packages/api-client/tests/*.test.js Vetalaya_frontend/tests/*.test.mjs`. Backend focused classes: `ClinicWorkspaceIsolationTest`, `ClientConfigurationTest`, `MultiClientAuthTest`. Build the Vite frontend and compile backend/test sources with Java 21.

Local Vite build, backend compilation and focused tests were verified. Java tests used the cached JUnit Platform 6 launcher because the standard offline Maven Surefire command requires an uncached launcher. No live Google/Apple sign-in, native build, production deployment, real PostgreSQL migration or full existing regression suite was completed.

Existing roadmap work remains: native pet-owner screens, dedicated inpatient/nursing records, laboratory staff worklists, MedShop integration, unified finance/payment workflows, professional verification, production authentication/OTP hardening, record-sharing authorization and infrastructure validation. Operational tasks do not implement those ledgers or clinical workflows. This configuration does not certify the platform production-ready.

Official references: [Expo environment variables](https://docs.expo.dev/guides/environment-variables/), [application configuration](https://docs.expo.dev/versions/latest/config/app/), [SecureStore](https://docs.expo.dev/versions/latest/sdk/securestore/), [project creation](https://docs.expo.dev/get-started/create-a-project/).
