# Product inventory and roadmap

The staging deployment uses commit `93c9911`. Use the staging site to review the current web release. Mobile status below refers to app source unless a build is explicitly mentioned. MedShop capabilities are classified by backend evidence. [Vetalya staging](https://vetalya-staging-z0u9.onrender.com/owner/login)

| Product area | Available | Partial or planned |
| --- | --- | --- |
| Accounts | Owner and vet web entry pages, password login, registration email OTP, owner web Google sign-in, Android Google integration in source, platform and clinic staff routing | Repeat full OAuth acceptance on target devices. iOS provider setup and signed release remain pending |
| Owner web | Dashboard, pet profiles, appointments, booking, care reminders, records, profile, Passport, sharing and emergency collar lookup | Nearby search, richer consultation discovery and booking-linked consent |
| Owner Android and iOS source | Expo screens for pets, visits, booking, records, profile, reminders, Passport and sharing. One shared backend and database with web | Latest source needs a new binary when requested. Older Android APKs represent earlier code. No current iOS store release was verified |
| Vet discovery | Verified-vet results, hospital data and PIN-based ordering | Existing PIN ordering compares numbers. Geographic radius, validated coordinates, city and locality resolution, and location-permission fallback need implementation |
| Booking | Slot availability, atomic slot booking, appointment views, owner cancellation and rescheduling APIs | Validate capacity and retries across booking paths. Configured fees, payment confirmation and scoped consent tied to appointment lifecycle need integration |
| Passport identity | Existing Passport numbers, pet identity records, summary, timeline, document metadata and public emergency collar QR lookup | Verified caregiver delegation, ownership transfer, duplicate review, device QR scanning and permanent secure issuance for all creation paths |
| Passport access | Owner history grouped by status and expiry. Approve and revoke actions. Server enforces matching requester, hospital, scope, active vet access and expiry for shared reads | Vet lookup by Passport ID, named recipient selection, partial approval, access audit and authenticated document delivery. Existing owner-initiated records identify the owner as requester and do not confer access on hospital members |
| Vet portal | Profile, hospital context, appointment queues, check-in, draft encounter, signing, prescriptions and diagnostic orders | Better draft recovery and full staging acceptance. Dedicated lab technician results workflow and professional verification need more work |
| Hospital operations | Hospital selection, scoped daily queue and queue search, operational tasks, manager directory and assignment controls | Inpatient admissions, nursing observations, medication administration, clinical handovers, dispensing and clinic finance |
| Platform administration | Separate admin portal and administrative provisioning routes | Verify operational approval policies and the production onboarding workflow |
| MedShop | Separate tenant-oriented backend for authentication, staff, catalog, stock, customers, invoices, GST and reports | Validate its deployment and availability independently. The clinic pharmacy and consultation billing flows require integration with suitable ledgers |
| Staff mobile | Owner Expo app is the shipped app codebase | Dedicated vet and hospital staff mobile experience |

## Delivery order

1. Verify the deployed Passport flow using separate staging owner and vet accounts. Cover request, approval, permitted reads, denials for other scopes and hospitals, and revocation.
2. Add named-vet Passport lookup and access requests with audited, rate-limited exact lookup. Decide how broad hospital-sharing records should be handled before changing their behavior.
3. Add geographic discovery alongside existing search. Confirm coordinate sources and postal data, then connect real availability filters.
4. Specify the booking consent policy before integrating it. Define explicit scope, the access window, and what cancellation, rescheduling and reassignment do to existing grants.
5. Design hospital clinical operations and pharmacy/finance integrations as separate workflows with explicit permissions and persistent records.
6. Validate mobile releases, migration upgrades and realistic load with staging fixtures before promoting a production release.

The roadmap follows the [Passport gap analysis](PASSPORT_FEATURE_GAP.md) and the [technical handbook](TECHNICAL_HANDBOOK.md). Scope choices in this roadmap are proposals.
