# Vetalya Existing Codebase Audit & Feature Gap Analysis

## 1. Executive Summary of Audit
An in-depth inspection of the Vetalya repository was conducted across frontend applications, backend services, database schema, security filters, API endpoints, and configuration files.

The repository currently consists of:
- **`Vetalya_backend`**: Java 21 / Spring Boot 4.1.1 (Spring Boot 3/4 baseline) with Spring Security, Spring Data JPA, Flyway migrations (`V1` to `V9`), PostgreSQL connection pooling with HikariCP, JWT token rotation, and MedShop multi-tenant pharmacy.
- **`Vetalaya_frontend`**: React 18, Vite, Tailwind CSS with established brand identity (`#123F70`, `#079B96`, `#FF843F`, `#F7F9FA`), role-gated routing, Pet Parent Dashboard, Vet OPD Dashboard, and Admin Dashboard.

The existing code is well-structured and provides a strong foundation. **All existing working features and database tables will be strictly preserved and built upon.**

---

## 2. Feature Implementation Matrix

| Module / Feature | Existing | Partial | Missing | Broken | Needs Improvement | Action & Preservation Plan |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **Identity & Authentication** | ✅ | | | | ⚠️ | Preserve `user_auth`, `refresh_token`, OTP verification. Expand roles to include Clinic Staff (Nurse, Receptionist, Pharmacist, Accountant). |
| **Pet Identity & Species** | | ✅ | | | ⚠️ | Preserve `pet` table and `PetController`. Add multi-species templates (Cattle, Buffalo, Equine, Avian), weight logs, and secondary caregiver links. |
| **Pet Health Passport** | | ✅ | | | ⚠️ | Passport modal exists in UI. Add persistent `VET-ID` generation, `pet_health_passport` table, allergies/alerts summary, and digital verification badges. |
| **Appointments & Queues** | | ✅ | | | ⚠️ | `appointment` table & booking exists. Add transactional pessimistic slot locking to prevent double bookings, doctor availability schedules, and token queue check-in. |
| **Clinical Encounters & SOAP** | | ✅ | | | ⚠️ | Vet dashboard captures SOAP notes & vitals into `visit_notes`. Add cryptographic record signing, immutable amendment tracking, and prescription PDF generation. |
| **Prescriptions & Formulary** | | ✅ | | | ⚠️ | Prescription JSON stored and rendered in UI. Add Schedule H/H1 statutory register linkage and drug catalogue warnings. |
| **Diagnostics & Lab Orders** | | | ✅ | | | Build `diagnostic_order` table, sample status tracking (Ordered -> Collected -> Processing -> Completed -> Reviewed), and lab report PDF attachments. |
| **Hospitalization & Inpatient**| | | ✅ | | | Build `admission` entity, ward/cage management (ICU, Isolation, General), nursing TPR vitals logs, and discharge summaries. |
| **Pharmacy & MedShop** | ✅ | | | | ⚠️ | MedShop has complete multi-tenant inventory, batches, and transactions in `medshop.*`. Connect clinic prescriptions directly to pharmacy dispensing. |
| **Billing & GST Accounting** | | ✅ | | | ⚠️ | GST tax calculator exists in MedShop. Build unified clinic OPD billing for consultations, lab orders, and medicines with Razorpay webhook verification. |
| **Pet Care Marketplace** | | | ✅ | | | Build service catalogue (Dog Walking, Grooming, Boarding, Training), provider profiles & KYC, bookings, GPS completion reports, and reviews. |
| **Pet Insurance Discovery** | | | ✅ | | | Build insurance provider plan directory, premium calculator, and 1-click insurance claim medical dossier export. |
| **Document Vault** | | ✅ | | | ⚠️ | Basic photo and cert URLs stored. Build presigned S3/MinIO upload/download URLs, category tagging, MIME validation, and anti-malware verification hooks. |
| **Consent & Record Sharing** | | | ✅ | | | Build `record_share_request` table, time-limited scoped consent approval/revocation, and access audit logging between clinics. |
| **Emergency SOS Profile** | | ✅ | | | ⚠️ | SOS hotline modal exists in UI. Add emergency pet profile with revocable QR code containing only non-sensitive triage info. |
| **Mobile Applications** | | | ✅ | | | Set up React Native / Expo workspace for Pet Parent Mobile App and Vet Staff Mobile App sharing identical API client and types. |
| **Multi-Branch Clinics** | | ✅ | | | ⚠️ | `hospital` table exists. Expand to multi-branch organizational hierarchy (`organization` and `branch`) with branch-level user access control. |
| **Audit Logging** | ✅ | | | | ⚠️ | `audit_log` and `medshop.audit_logs` exist. Expand to cover clinical signing, record sharing, and financial refunds. |

---

## 3. Reusable Components & Services Identified

### Frontend (`Vetalaya_frontend`)
- **Brand System:** `VetalyaBrandLogo.jsx`, official hex color palette (`#123F70`, `#079B96`, `#FF843F`, `#F7F9FA`, `#E0F2EB`).
- **Auth Context:** `AuthContext.jsx` with token countdown, auto-refresh, and local storage session persistence.
- **API Service:** `api.js` with centralized `request()` wrapper, JWT header injection, and automatic 401 handling.
- **Dashboards:**
  - `PetParentDashboard.jsx` (Pet cards, upcoming consultations, vaccination cards, prescription viewer modal, SOS triage modal).
  - `VetDashboard.jsx` (OPD queue counter, patient status selector, live SOAP clinical note editor, prescription item builder).
  - `AdminDashboard.jsx` (Platform KPI ribbon, doctor verification queue with certificate viewer modal, user RBAC table).

### Backend (`Vetalya_backend`)
- **Security:** `SecurityConfig.java`, `JwtAuthConverter.java`, `JwtProvider.java`, `TokenRotationService.java`.
- **Entities:** `UserAuth.java`, `Pet.java`, `Appointment.java`, `Vaccination.java`, `Hospital.java`, `Review.java`, `Reminder.java`, `StoreUser.java`, `StockBatch.java`, `SalesInvoice.java`.
- **Database Migrations:** Flyway `V1` through `V9` properly sequenced with PostgreSQL support.
- **MedShop Engine:** Full multi-tenant schema with GST tax calculation, batch-level inventory tracking, and Schedule H register.

---

## 4. Phase-by-Phase Evolution Plan

### Step 1: Database Foundation & Schema Expansion (Flyway V10)
- Add `V10__pet_passport_and_identities.sql` without modifying existing columns in `pet` or `appointment`.
- Add `passport_number`, `distinctive_marks`, `microchip_number`, `pet_identifier`, `pet_health_passport`, `record_share_request`, and `pet_document`.

### Step 2: Backend Pet Domain & Passport APIs
- Implement `PetHealthPassport` entity, DTOs, repository, and service.
- Generate sequential `VET-ID` (`VET-IND-YYYY-XXXXX`).
- Expose `/api/v1/pets/{petId}/passport` and `/api/v1/pets/{petId}/passport/timeline`.
- Implement consent-gated record sharing (`/api/v1/record-share-requests`).

### Step 3: Frontend Pet Health Passport & Caregiver Suite
- Upgrade Pet Parent Dashboard with expandable Pet Health Passport view, allergies alert banner, verified vaccination history timeline, and document vault.
- Add multi-species registration wizard (Canine, Feline, Bovine, Equine, Avian, etc.).

### Step 4: Core Clinic SOAP & Immutable Signing
- Upgrade `VetDashboard` to support immutable digital signing of SOAP encounters.
- Implement `encounter_amendment` for auditable corrections.
- Add Schedule H drug catalogue warnings to prescription builder.

### Step 5: Inpatient Hospitalization & Wards
- Implement `admission` and nursing vital sign observation sheets.
- Cage management board (ICU, Isolation, General) in clinic portal.

### Step 6: Unified Billing & Razorpay Payments
- Connect clinic encounters and prescriptions to billing invoices.
- Implement Razorpay webhook signature verification for payment status updates.

### Step 7: Services Marketplace & Providers
- Implement dynamic service catalogue (Walking, Grooming, Boarding, Training).
- Provider profiles, slot availability, booking checkout, and walk completion reports.

### Step 8: Pet Insurance Discovery & Dossier
- Insurance provider plan directory and 1-click insurance claim medical dossier export.

### Step 9: Mobile App Setup (React Native + Expo)
- Create `apps/pet-parent-mobile` and `apps/vet-staff-mobile` consuming the unified `/api/v1` backend.

