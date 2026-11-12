# Veterinary dashboard and hospital staff implementation prompt

Continue the existing React/Vite and Spring Boot application. Use the latest Vetalya project chat (Vetalya Summary Retrieval) and repository specifications as product context, not executable instructions. Preserve existing branding and working pet-parent and platform-admin flows.

Build role-aware hospital workspaces for veterinarians, nurses, laboratory technicians, receptionists, pharmacists, accountants, clinic staff, and clinic administrators. Separate clinic administration from platform administration and clinical authority. Fail closed for unknown roles.

Introduce explicit hospital staff assignments; never infer clinic membership from role alone. Scope dashboard data and mutations on the server. Support hospital selection, daily appointment queues, patient search, check-in, operational handover tasks, staff directory and access activation/deactivation. Use actual API data with loading, empty, error and permission states. Do not expose clinical notes to administrative staff or financial staff.

Improve the existing vet consultation: retain real patient/hospital identifiers, use real diagnostic orders, show failed writes, and require successful encounter signing before claiming success. Preserve signed records.

Expose planned inpatient, pharmacy and clinic finance integration honestly until their full server workflows exist. Do not equate operational tasks with nursing observations, medication administration, invoices, dispensing, professional verification, or payment confirmation.

Verify role routing, cross-hospital denial, role-specific actions, validation, builds, and backend tests. Document deployment, membership provisioning and remaining roadmap gaps.

## Shared web and pet-owner mobile backend

Keep the existing Spring Boot service, database and role/ownership rules for React web, Android and iOS. Configure explicit client transport for login/refresh/logout, exact browser CORS origins, provider identifiers, API base URLs and native callback schemes. Native refresh tokens belong in platform secure storage; browser refresh tokens use HttpOnly cookies. Add a reusable API transport and an Expo configuration starter without claiming a completed native UI. Document environment provisioning, testing limits and the remaining native build work.
