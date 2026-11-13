# Owner refinement and shared database setup

## Design / client update

Manrope Regular/Medium/SemiBold/Bold are bundled locally. Official web assets `vetalya-logo.png` and `vetalya-wordmark.png` are reused unchanged in the owner sidebar, Home and native shared screen header. The existing Home layout, live selected-pet imagery, real appointment status and unavailable tracking states are preserved.

Figma target: https://www.figma.com/design/GDyKkUhdVLKu8XCtxtObUJ/Vetalya-App?node-id=14-345

**Figma is not updated yet.** The Figma plugin rejected the write because this account reached the Starter-plan MCP limit. `docs/figma/apply-owner-refinement.js` contains the prepared editable-layer refinement with the official logo bytes embedded. It can be used as the main JavaScript of a local Figma development plugin, or the same changes can be applied through the connector after its quota is available. Its layout still requires a Figma screenshot check after execution; do not treat the prepared script as an applied or verified design.

## One backend and database

Web uses `/api` via Vite; `VITE_API_PROXY_TARGET` selects the development backend. Native uses `EXPO_PUBLIC_API_BASE_URL`. All pet profiles, users, appointments, passports and memberships belong in the same backend database; mobile applications never receive database credentials.

`application.yml` now defaults to a local PostgreSQL database rather than an implicit hosted database. Explicit `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` override it. Non-validating TLS settings were removed; managed production databases should specify certificate-verifying JDBC connection options and trusted CA configuration. Existing deployments must set their intended connection environment explicitly.

Demo account initialization now requires `APP_SEED_DEMO_DATA=true`. Leave false for real data. No new database schema was required for a font/logo refinement. Existing Flyway migrations V1–V14 remain authoritative; normal JPA schema validation must not generate or overwrite production tables.

## Create local PostgreSQL

Docker or a PostgreSQL server is required. Neither runtime is installed on this workstation, so **a PostgreSQL instance has not been started or migrated here**. The existing browser preview uses the disposable H2 QA backend; it is not a PostgreSQL migration check.

1. Copy `config/local-stack.env.example` to ignored `config/local-stack.env` and set a local `DB_PASSWORD`.
2. From `Vetalya_backend`, run `docker compose --env-file ../config/local-stack.env -f compose.local.yml up -d`.
3. Export the server variables from that file into your terminal, then run `./mvnw spring-boot:run` with Java 21. Spring Boot does not automatically load a plain `.env` file. Use `SPRING_PROFILES_ACTIVE=dev,local`, explicit DB credentials, and your auth/provider environment. Flyway creates the application's schema on the fresh `vetalya` database. Verify startup/migration output before pointing clients at it.
4. Web: configure `VITE_API_BASE_URL=/api` and `VITE_API_PROXY_TARGET=http://127.0.0.1:8080`. Run the Vite development server.
5. Android emulator: API host `10.0.2.2:8080`; iOS simulator: `localhost:8080`. Physical devices need a reachable LAN address or HTTPS environment. Configure allowed web origins, registered callback scheme and provider IDs separately; CORS must list explicit origins.

PostgreSQL data persists in the named `vetalya-local-postgres` volume. Do not delete this volume to solve connection errors. Database creation settings apply to first initialization; existing volumes keep their users/passwords and data.

For production, point all clients at the same HTTPS API deployment and set its PostgreSQL environment, migrations, backups and provider credentials. Never use the demo seed in that environment. Native signing and physical-device testing remain release work.

Verified in this update: 24 focused Java tests plus 17 shared-client/domain/routing tests; backend/test compilation, web build, native type/lint and both native bundle exports. PostgreSQL migration execution and Figma write/visual verification remain pending for the stated runtime/quota limitations.
