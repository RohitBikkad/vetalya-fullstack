# Technical handbook

## Runtime architecture

| Layer | Implementation and boundaries |
| --- | --- |
| Web | React 19, Vite 6 and Tailwind 3 in `Vetalaya_frontend`. Browser routes select owner and staff entry points. The dashboard router chooses a portal from the normalized role |
| Mobile | Expo SDK 58 and Expo Router in `apps/pet-owner-mobile`. Device storage uses SecureStore. Bundle identifiers vary by app variant |
| Shared client logic | `packages/owner-domain` holds pet owner API operations and validation. `packages/api-client` provides fetch transport and token refresh for native clients. Web uses its existing request integration |
| API | Java 21 and Spring Boot 4.1.1 per Maven configuration. Spring Security, Spring Data JPA, Hibernate, JdbcTemplate, Flyway and Actuator |
| Data | PostgreSQL on Supabase for staging. Local development has persistent PostgreSQL Compose. Core application and MedShop use distinct data structures in the same database deployment |
| Deployment | Staging builds React and embeds the output in the Spring Boot container. Web uses same-origin `/api`; mobile uses the configured HTTPS API origin |

The backend is a modular monolith. Owner, vet, clinic, clinical, authentication and MedShop are code boundaries. Some services read shared tables directly. Local commit `94de3a0` moved clinic check-in and encounter-related appointment updates into `AppointmentWorkflow`. Other appointment entry points still have their own paths.

## Data model and migrations

The [Flyway directory](../Vetalya_backend/src/main/resources/db/migration/) is the executable schema source.

| Versions | Data model |
| --- | --- |
| V1–V4 | User identity, provider links, tokens, vet and hospital profiles, pet owners, pets, appointments, reminders, vaccinations, reviews and audit |
| V5–V9 | MedShop tenants, catalog, inventory, invoices and compliance, OTP records and incremental core schema changes |
| V10 | Passport numbers, identifiers, Passport details, sharing requests and document metadata |
| V11 | Clinical encounters, amendments, prescriptions |
| V12 | Appointment slots, queue timestamps and optimistic concurrency support for appointments |
| V13 | Diagnostic catalog, orders and result items |
| V14 | Clinic staff assignments, operational tasks and events, role and appointment status constraints |
| V15 | Email verification receipt support |

A pet belongs to an owner and may have a Passport. Appointments link the pet, owner, veterinarian and optionally hospital and slot. Clinical encounters link the pet and appointment and can carry prescriptions. Diagnostic orders link to a patient, vet, hospital and optionally an encounter. Hospital membership lives in `clinic_staff_assignment`, with existing hospital owner access represented by the hospital's veterinarian unless an explicit inactive assignment overrides it.

Preserve applied migration bytes. Add a new Flyway version and test it against fresh and previously migrated PostgreSQL. Review schema changes for compatibility during app rollback.

## Authentication and authorization

- Web refresh uses an HttpOnly cookie. Native uses refresh tokens in JSON protected by SecureStore. Access tokens travel as Bearer JWTs. Client platform changes token delivery. Effective authority comes from the account and server checks.
- Registration sends email OTP on owner form submission. The server issues an email verification receipt after a correct code. Registration consumes it when creating the account. The staging profile disables demo login and database inspection.
- Clinic access checks active account and hospital affiliation. Staff permissions are role-specific. Reception, nursing, clinic staff and clinic managers have operational queue access. Accounts and pharmacy roles have operational tasks within their scope. Platform provisioning is separate from clinic access and clinical authority.
- Clinical writes require the relevant vet relationship and active access where a hospital is attached. Signing and amendments use transaction and row-lock controls on the encounter. Appointment ownership, encounter identity and diagnostic links are validated in their respective paths.
- Non-owner Passport reads require a matching `hospitalId`, an approved grant naming the requester, a valid time window and vet access at that hospital. Vaccination scope returns vaccination timeline entries. Emergency scope returns a limited overview. Full timeline scope permits record and document metadata reads. Shared responses omit contact details, QR tokens and document URLs.
- Supabase RLS isolation for staging is documented in the [reviewed SQL](../deploy/sql/staging-api-access.sql). The backend database owner connection can bypass ordinary RLS, so server authorization is essential.

## API contracts and observability

Use the [route catalog](api/ROUTE_CATALOG.md) to find controllers. Check source DTOs, validation and service checks for each endpoint. Prefixes vary across feature groups. Preserve existing field names, response shapes and authorization semantics for installed apps. API docs and Swagger are disabled on staging. The staging readiness endpoint is `/actuator/health/readiness`. Runtime and request logs are available in Render. Monitor HTTP status, timing, JVM resource usage, and DB pool health when investigating slow requests.

The highest completed recorded staging load stage was 150 successful authenticated read requests at a target of 5 per second over about 31 seconds. The p95 was 1.22 seconds. Tests using connection reuse also observed transport errors. This was one synthetic account with light data and does not establish capacity for writes or production traffic.

## Clinic role matrix

These permissions describe the clinic workspace. Each operation still checks hospital access and its own record constraints.

| Role | Daily queue | Check-in | Staff directory and assignment | Clinical authority |
| --- | --- | --- | --- | --- |
| Veterinarian | Yes | Yes | Hospital owner only, subject to active access | Assigned clinical workflow |
| Veterinary nurse | Yes | — | — | Operational tasks |
| Receptionist | Yes | Yes | — | Operational tasks |
| Clinic staff | Yes | — | — | Operational tasks |
| Clinic administrator or manager | Yes | Yes | Yes | Operational tasks |
| Lab technician, pharmacist or accountant | — | — | — | Role-scoped operational tasks |
| Platform administrator | Outside the clinic workspace | — | Administrative provisioning path | — |
| Pet owner | Own appointments in owner portal | — | — | Own pet records and sharing actions |

Unknown roles fail closed in portal routing. Assignment changes identify a pre-existing account and record the actor in clinic operation events. Appointment queues and task access use the selected hospital on the server.
