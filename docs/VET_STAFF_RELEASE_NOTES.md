# Vet dashboard and hospital staff update

## Requirements reviewed

The latest available Vetalya project chat, **Vetalya Summary Retrieval**, describes clinic administration and separation of clinical authority. The repository audit, role matrix, screen inventory, implementation roadmap, architecture, API and database specifications provided the existing product context. Document recommendations were treated as requirements context, not authorization to run commands, contact people, or deploy.

The implementation prompt is in `docs/VET_STAFF_IMPLEMENTATION_PROMPT.md`.

## Implemented

- Separate hospital workspaces for veterinarian, nurse, receptionist, lab technician, pharmacist, accountant, hospital staff, clinic administrator and clinic manager. Unknown roles no longer silently render the pet-parent portal.
- Clinic administrators no longer see the platform administration dashboard. Legacy global admin APIs now require platform administrator authority.
- Hospital and IST working-date selectors, role-specific overview, patient search, status filtering, reception check-in, operational tasks and handovers, staff directory and hospital access activation/revocation.
- Explicit hospital assignments with server-side scope enforcement for the new `/api/clinic` endpoints. The hospital owner retains access. Staff role alone never grants access to every clinic.
- Operational writes persist with actor-attributed events. They are not medical records, nursing observations, prescriptions or invoices.
- Financial, pharmacy and lab staff do not receive general patient queues. Operational queue responses omit clinical notes and caregiver contact details.
- Vet consultation retains patient/hospital IDs, filters appointments by hospital/date across pages, loads persisted diagnostics and the test catalogue, surfaces failed writes, requires successful encounter signing, resumes existing drafts, and prevents a second editable SOAP note for finalized visits.
- Clinical write permissions no longer include clinic administrators. Encounter and diagnostic direct reads/writes have author/owner checks; owner reads are limited to signed encounters and completed diagnostics. Clinical amendments require the authoring vet. Server identity controls diagnostic verification attribution.
- Successful empty API responses are supported. Fake activity notifications, connectivity claims, and staff-facing platform database/session testing controls were removed.

## Setup

1. Deploy the backend and let Flyway apply `V14__hospital_staff_workspace.sql`. Deploy frontend and backend together. No production deployment or database migration was performed in this task.
2. Existing hospital owners automatically retain access. Existing staff do **not** receive guessed assignments. Hospital owners or assigned clinic administrators can add an existing active staff account by email in **Staff & access**.
3. A platform administrator can provision the first clinic administrator with authenticated `PUT /api/clinic/hospitals/{hospitalId}/staff`, JSON body `{"email":"admin@example.test","department":"Administration","active":true}`. The endpoint does not change professional roles or create login credentials.
4. Revoke/reactivate hospital access in the staff directory. Global account blocking and professional verification remain platform-admin functions.
5. For local frontend verification: `node node_modules/vite/bin/vite.js build --configLoader runner`, then `node copy-routes.js`. The runner option avoids writing bundled config files through the external node_modules link in the isolated verification copy. Standard `npm run build` remains unchanged in the source repository.

## Verification

- Frontend production build passed; existing bundle-size warning remains.
- Backend production and test sources compiled using Java 21.
- Seven new SQL-backed JUnit tests passed with H2 for clinic isolation, revocation, role gates, task attribution, repeat check-in rejection, day/hospital filtering and finance queue restrictions.
- Two Node role-routing tests passed.
- Browser smoke checks passed for all nine hospital roles and the staff directory, using a temporary localhost harness with synthetic QA data. These are UI checks, not a live production integration test.
- Maven Surefire's standard test command could not resolve an uncached JUnit Platform 1.12.2 launcher offline. The seven new tests were executed directly with the installed JUnit Platform 6.0.3 launcher. The full existing suite and a real PostgreSQL/Flyway migration run were not completed.

## Remaining planned functionality

Dedicated inpatient admissions/wards and nursing observations, lab staff sample/result worklists, MedShop hospital/store session integration, dispensing, unified clinic invoices/payments/refunds, invitations and professional role assignment, shifts/attendance/leave, clinic profile editing, multi-organization hierarchy, financial reports, notification delivery and subscriptions remain roadmap work. Their workflows are not represented as successful local-only changes. Operational tasks do not implement these clinical or financial ledgers.

The new hospital workspace enforces assignment scope. Legacy passport sharing and other pre-existing APIs outside the touched controllers still require the broader authorization audit described in the original roadmap; this update is not a platform-wide security certification.

## Shared Android/iOS backend configuration follow-up

The existing backend is now configurable for web, Android and iOS token delivery. Exact CORS origins, configurable API URLs, native login handling, single-use code exchange, a shared API client and an Expo configuration starter were added. See [shared web/Android/iOS setup](SHARED_WEB_ANDROID_IOS_SETUP.md) for environment settings, provider setup and the explicit limits of the mobile starter. Hospital assignments and ownership permissions remain server-controlled and do not change with client type.
