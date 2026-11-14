# Vetalya product and engineering documentation

Reviewed 14 November 2026 against application commit `94de3a0`. Render staging reported `93c9911` live at the time of review. Source availability, deployment, and acceptance-test status are tracked separately.

| Document | Contents |
| --- | --- |
| [Product inventory and roadmap](PRODUCT_STATUS.md) | Owner, vet, staff, platform admin and MedShop capabilities, known limits and remaining work |
| [Technical handbook](TECHNICAL_HANDBOOK.md) | Architecture, module boundaries, database, authorization, client contracts and observability |
| [Engineering and release runbook](ENGINEERING_RUNBOOK.md) | Local setup, validation, deployment, mobile releases and rollback |
| [API route catalog](api/ROUTE_CATALOG.md) | Controller mappings grouped by source component |
| [Passport feature gaps](PASSPORT_FEATURE_GAP.md) | Passport policy, current grant limits and next implementation steps |
| [Staging release history](../deploy/STAGING.md) | Historical rollout notes and smoke checks |
| [Hospital workspace release notes](VET_STAFF_RELEASE_NOTES.md) | Staff assignment, provisioning and operational workflow context |
| [Google and email verification notes](GOOGLE_AND_EMAIL_VERIFICATION.md) | Provider setup and previous validation |
| [Mobile app README](../apps/pet-owner-mobile/README.md) | Expo project setup and run commands |

The earlier [API design](api/OPENAPI_SPECIFICATION.md), [architecture](architecture/SYSTEM_ARCHITECTURE.md), [screen inventory](architecture/SCREEN_INVENTORY.md), [schema design](database/DATABASE_SCHEMA_AND_ERD.md), [roles design](permissions/ROLE_PERMISSION_MATRIX.md), [feature dependencies](architecture/FEATURE_DEPENDENCY_MAP.md), [clinical validation notes](architecture/LEGAL_CLINICAL_INTEGRATION_VALIDATION.md), [gap audit](AUDIT_AND_GAP_ANALYSIS.md), and [V1 plan](roadmap/V1_IMPLEMENTATION_PLAN.md) are historical design references. Use this hub, controller code, and Flyway migrations to establish current behavior.

Product changes to existing business workflows require the project owner's approval. Track the approved behavior and affected clients in the pull request.
