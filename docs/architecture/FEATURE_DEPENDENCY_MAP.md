# Vetalya Feature Dependency Map & Module Graph

## 1. Module Hierarchy & Dependency Graph

```mermaid
flowchart TD
    %% Base Infrastructure
    subgraph CoreFoundation["Tier 0: Infrastructure & Security Foundation"]
        M1["M1: Identity & Authentication<br/>(JWT, OTP, Password, Token Rotation)"]
        M4["M4: Roles, Permissions & Scopes<br/>(RBAC, ABAC, Branch Context)"]
        M26["M26: Audit & Compliance Engine<br/>(Immutable Event Log, DPDP)"]
        M20["M20: Document Vault<br/>(Private S3, Presigned URLs, Anti-Malware)"]
        M22["M22: Notification Engine<br/>(Push, DLT SMS, WhatsApp Cloud API)"]
    end

    %% Organizations & Profiles
    subgraph EntityTier["Tier 1: Entities & Organizations"]
        M2["M2: User Profiles & Caregivers"]
        M3["M3: Organizations & Branches"]
        M5["M5: Pet Identity & Species Profiles<br/>(Dogs, Cats, Cattle, Horses, Birds)"]
        M6["M6: Pet Identifiers & Microchips"]
    end

    %% Passport Tier
    subgraph PassportTier["Tier 2: Universal Pet Health Passport"]
        M7["M7: Universal Pet Health Passport<br/>(Immutable VET-ID, Portable Timeline)"]
        M21["M21: Consent & Record Sharing<br/>(Time-limited, Scoped Consent, QR Access)"]
    end

    %% Clinical & Hospital Operations
    subgraph ClinicTier["Tier 3: Clinical & Hospital Operations"]
        M14["M14: Appointments & Token Queues<br/>(Pessimistic Slot Locking)"]
        M8["M8: Clinical Encounters & SOAP Notes"]
        M9["M9: Diagnoses & Vital Observations"]
        M10["M10: Vaccinations & Batch Tracking"]
        M11["M11: Prescriptions & Dosage Calculator"]
        M12["M12: Diagnostics & Lab Orders"]
        M13["M13: Hospitalization, Wards & Nursing Handover"]
    end

    %% Commerce & Pharmacy
    subgraph CommerceTier["Tier 4: Pharmacy, Finance & Marketplace"]
        M16["M16: MedShop Pharmacy & Inventory<br/>(Batch FEFO, Schedule H/H1, Movement Ledger)"]
        M15["M15: Billing, Payments & GST<br/>(Invoices, Receipts, Razorpay Webhooks)"]
        M17["M17: Marketplace Service Catalogue"]
        M18["M18: Service Providers & Walkers/Groomers"]
    end

    %% Advanced Ecosystem
    subgraph EcosystemTier["Tier 5: Platform & Ecosystem Integrations"]
        M19["M19: Pet Insurance Discovery & Dossier"]
        M23["M23: Reporting & Business Intelligence"]
        M24["M24: Clinic Subscriptions & SaaS Billing"]
        M25["M25: Vetalya Super Admin Governance"]
    end

    %% Dependencies
    M1 --> M2
    M1 --> M3
    M1 --> M4
    M4 --> M2
    M4 --> M3

    M2 --> M5
    M5 --> M6
    M5 --> M7
    M6 --> M7
    M21 --> M7

    M3 --> M14
    M5 --> M14
    M14 --> M8
    M8 --> M9
    M8 --> M10
    M8 --> M11
    M8 --> M12
    M8 --> M13

    M11 --> M16
    M12 --> M15
    M11 --> M15
    M8 --> M15

    M18 --> M17
    M5 --> M17
    M17 --> M15

    M7 --> M19
    M15 --> M19

    M3 --> M23
    M15 --> M23
    M16 --> M23

    M3 --> M24
    M24 --> M25
    M26 --> M25
```

---

## 2. Cross-Module Transactional Boundaries

| Trigger Event | Primary Module | Dependent Modules | Transactional Safety Mechanism |
| :--- | :--- | :--- | :--- |
| **Slot Booking** | `M14: Appointments` | `M15: Payments`, `M22: Notifications` | Database Pessimistic Lock on Slot ID + 10-minute hold Redis key to prevent double bookings. |
| **Encounter Finalization** | `M8: Encounters` | `M7: Passport`, `M10: Vaccinations`, `M11: Prescriptions` | Two-phase cryptographically signed snapshot; changes recorded as immutable revisions. |
| **Prescription Dispensation** | `M16: Pharmacy` | `M11: Prescriptions`, `M15: Billing` | DB Row-lock on `stock_batches`, updates batch quantity with FIFO/FEFO, inserts into `stock_movements`. |
| **Payment Confirmation** | `M15: Billing` | `M14: Appointments`, `M16: Pharmacy`, `M22: Notifications` | Webhook signature verification + idempotent key check; status transitioned inside single `@Transactional`. |
| **Record Share Approval** | `M21: Consent` | `M7: Passport`, `M26: Audit` | Grants time-limited permission token; every read event logged to `access_events` with timestamp and IP. |

---

## 3. Application-to-Module Mapping

| Application | Technology | Primary Consumed Modules |
| :--- | :--- | :--- |
| **1. Pet Parent Mobile App** | React Native (Expo, iOS & Android) | M1, M2, M5, M6, M7, M10, M14, M17, M19, M20, M21, M22 |
| **2. Vet Staff Mobile App** | React Native (Expo, iOS & Android) | M1, M3, M4, M5, M7, M8, M9, M10, M11, M12, M13, M14, M22 |
| **3. Veterinary Web Portal** | React / TypeScript | M1, M3, M4, M5, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M20, M21 |
| **4. Clinic Admin Portal** | React / TypeScript | M1, M3, M4, M14, M15, M16, M23, M24, M26 |
| **5. Vetalya Super Admin** | React / TypeScript | M1, M3, M4, M17, M18, M19, M23, M24, M25, M26 |

