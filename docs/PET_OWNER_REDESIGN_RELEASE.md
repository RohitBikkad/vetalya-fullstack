# Pet-owner redesign release

The responsive React/Vite owner portal and Expo Android/iOS app share the existing Spring Boot API, accounts and database. The official branding is retained. Hospital and platform administration continue through the existing role router; only pet owners enter the new owner workspace.

## Implemented

- Owner sign-in/registration, dashboard, pet switching, pet create/edit, passport caregiver fields and profile updates.
- Real hospital/veterinarian discovery, future available slots, booking receipts with actual IDs, visit history and cancellation.
- Signed encounter and prescription viewing, medical timeline, document links and vaccination history/due dates.
- Scoped hospital sharing with explicit immediate grant wording, server-persisted history and revocation. Sharing history is restricted on the server to the pet's primary owner.
- Independent loading/error/empty states and visible write errors; responsive navigation, labelled forms and focus-managed web dialogs.
- Expo native navigation, in-memory access token / secure refresh token storage, shared API/domain packages and environment configuration.

The booking API returns scalar receipt fields rather than a lazy Hibernate entity and rejects slots whose start time has passed. Older stored species labels are normalized for editing.

## Development and release

Web: `cd Vetalaya_frontend && npm ci && npm run dev`. Configure the API proxy target or `VITE_API_BASE_URL` using the existing environment template. Local previews in this session use ports 3000/3002 and an isolated H2 backend on 8080.

Native: follow [mobile README](../apps/pet-owner-mobile/README.md). All clients point to the same backend `/api`; no separate mobile database is needed. Configure CORS for deployed web origins, HTTPS, callback schemes, registered native IDs, provider credentials and signing before distribution. Backend defaults must not be pointed at a real database during local demo seeding. Use an explicit local profile; the in-memory QA database is disposable and not production migration evidence.

Provision hospital staff through explicit active hospital assignments as documented in [staff release notes](VET_STAFF_RELEASE_NOTES.md). Pet-owner role alone grants no hospital membership. Signed clinical content remains server-controlled.

## Verification

Web production build; native Android and iOS bundle export; TypeScript and Expo lint; shared transport/owner-domain tests; focused backend role-isolation, client-auth, slot-booking and sharing-history tests. Live local API/browser checks covered owner login, pet and profile editing, passport, available-slot booking, cancellation and sharing/revocation. Native bundles are not APK/IPA files and have not been tested on a physical device. The full Maven test lifecycle remains constrained by unavailable offline Surefire dependencies; focused tests run with the cached JUnit launcher.

## Remaining roadmap

Private document byte storage/upload, Google/Apple native sign-in, account recovery, push delivery, atomic slot-aware rescheduling, inpatient, pharmacy/dispensing, invoices/payments, grooming, boarding, walking, training and insurance integrations remain incomplete and visibly labelled. No operational task is presented as a clinical observation, verified professional act, dispensing record or payment confirmation. Store signing/submission, accessibility/device regression and production deployment remain required release steps.
