# Vetalya Role-Based & Attribute-Based Access Control (RBAC/ABAC)

## 1. Access Control Model & Principles
Vetalya enforces a multi-dimensional security model combining:
1. **System Roles (RBAC):** High-level privileges defining what user categories can execute.
2. **Tenant & Branch Scope (ABAC):** Clinic staff privileges are bounded to their assigned `organization_id` and `branch_id`.
3. **Pet Record Ownership & Caregiver Scope:** Pet parents can only access pets where an active caregiver link exists.
4. **Consent-Gated Clinical Access:** A veterinarian outside the pet's home clinic cannot view private historical encounters without an active `record_share_request` approved by the caregiver (except for emergency triage summary).
5. **Separation of Duties:** Accountants cannot view medical clinical notes; veterinarians cannot delete posted financial ledgers; platform support agents have zero default access to clinical records without time-limited, audited elevation.

---

## 2. Definitive Role Permission Matrix

| Module / Action | Pet Parent | Co-Caregiver | Veterinarian | Vet Nurse | Lab Tech | Receptionist | Pharmacist | Accountant | Clinic Admin | Service Provider | Vetalya Support | Super Admin |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Manage Pet Identity** | Full (Own) | Read (Own) | Verify/Tag | Read | No | Register Patient | No | No | Read (Branch) | No | Read (Audited) | Read/Audit |
| **View Health Passport** | Full (Own) | Full (Own) | Scope-Gated | Scope-Gated | Order-Only | No | Rx-Only | No | Branch-Only | No | Audited | Audited |
| **Upload Personal Docs** | Yes (Own) | Yes (Own) | No | No | No | No | No | No | No | No | No | No |
| **Grant/Revoke Consent** | Yes (Own) | No | No | No | No | No | No | No | No | No | No | No |
| **Create Encounters** | No | No | Yes (Branch) | No | No | No | No | No | No | No | No | No |
| **Edit Draft Encounters** | No | No | Author-Only | No | No | No | No | No | No | No | No | No |
| **Sign Encounters** | No | No | Author Vet | No | No | No | No | No | No | No | No | No |
| **Amend Signed Record** | No | No | Audited Vet | No | No | No | No | No | No | No | No | No |
| **Prescribe Medicines** | No | No | Yes (Branch) | No | No | No | No | No | No | No | No | No |
| **Administer Inpatient Meds** | No | No | Yes | Yes (Branch) | No | No | No | No | No | No | No | No |
| **Record Nursing Vitals** | No | No | Yes | Yes (Branch) | No | No | No | No | No | No | No | No |
| **Upload Lab Reports** | No | No | Yes | Yes | Yes (Branch) | No | No | No | No | No | No | No |
| **Manage Queue & Check-In** | No | No | View/Call | View/Call | View | Full (Branch) | No | No | Full (Branch) | No | No | No |
| **Book Appointments** | Yes | Yes | On-behalf | On-behalf | No | Yes (Branch) | No | No | Yes (Branch) | No | No | No |
| **Dispense Pharmacy Meds**| No | No | In-Clinic | In-Clinic | No | No | Full (Tenant)| No | Manage | No | No | No |
| **Create/Edit Invoices** | No | No | Add Items | Add Items | No | Full (Branch) | Dispense | Full (Tenant)| Full (Tenant)| No | No | No |
| **Receive Payments** | Pay Only | Pay Only | No | No | No | Yes (Branch) | Yes (Cashier)| Yes (Tenant)| Full (Tenant)| No | No | No |
| **Stock Inventory Movements**| No | No | No | No | No | No | Full (Tenant)| View | Full (Tenant)| No | No | No |
| **Manage Clinic Staff/Roles**| No | No | No | No | No | No | No | No | Full (Org) | No | No | Platform Admin|
| **Marketplace Service Fulfillment**| Book/Rate| Book/Rate | No | No | No | No | No | No | No | Fulfill (Own) | Dispute Help | Platform Admin|
| **Platform System Settings**| No | No | No | No | No | No | No | No | No | No | No | Full |
| **Audit Logs Access** | No | No | No | No | No | No | No | No | Branch Logs | No | Audited | Full Global |

---

## 3. Data Protection & Elevation Safeguards
* **Vetalya Support "Break-Glass" Elevation:** In production, platform support engineers do not have blanket `SELECT *` access on medical tables. When troubleshooting a ticket, a time-limited elevation token (max 60 minutes) must be explicitly requested and authorized, linking the technician's identity to a verified Zendesk/Jira ticket ID with full session keystroke audit logging.
* **Immutability of Signed Records:** Once a veterinarian signs a clinical encounter (`is_signed = TRUE`), the database rejects all `UPDATE` operations via trigger and Spring JPA pre-update hooks. Corrections require an entry in `encounter_amendment` retaining before-and-after snapshots and veterinary justification.
