# Vetalya OpenAPI 3.1 & REST API Specification (/api/v1)

## 1. Global API Standards & Conventions

### Base URI
`https://api.vetalya.com/api/v1` (Production)
`http://localhost:8080/api/v1` (Local Development)

### Standard Headers
* `Authorization`: `Bearer <jwt_access_token>`
* `Content-Type`: `application/json`
* `X-Correlation-ID`: Trace UUID injected on every request and response.
* `X-Tenant-ID`: Active organization / pharmacy tenant context for multi-tenant staff.
* `X-Branch-ID`: Specific clinic branch context.
* `Idempotency-Key`: Required UUID on all financial settlements, appointment bookings, and inventory transactions.

### Uniform Success Response
```json
{
  "success": true,
  "data": { ... },
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 142,
    "totalPages": 8
  }
}
```

### Uniform Error Response
```json
{
  "success": false,
  "error": {
    "code": "SLOT_ALREADY_RESERVED",
    "message": "The selected appointment slot has just been booked by another client.",
    "details": [
      {
        "field": "slotId",
        "issue": "Concurrent booking conflict"
      }
    ],
    "timestamp": "2026-11-12T08:15:00Z",
    "correlationId": "8f8a2c12-32b0-4fa2-8b43-912b7f80ce22"
  }
}
```

---

## 2. Comprehensive Endpoint Index

### 2.1 Identity, Authentication & Profile (`/auth`, `/users`)
* `POST /auth/register` — Register new user (Pet Parent, Vet, Staff).
* `POST /auth/send-otp` — Request DLT-approved SMS / WhatsApp OTP.
* `POST /auth/verify-otp` — Verify OTP and receive JWT access token + refresh token.
* `POST /auth/login` — Standard password / credential login.
* `POST /auth/refresh` — Rotate refresh token and issue new 15-minute access token.
* `POST /auth/logout` — Revoke token family and invalidate Redis session.
* `GET /users/me` — Retrieve active authenticated user profile, roles, and branch memberships.
* `PUT /users/me` — Update personal contact and address details.
* `GET /users/me/notifications/preferences` — Get push/SMS/WhatsApp alert preferences.
* `PUT /users/me/notifications/preferences` — Update notification channels.

### 2.2 Pet Registry & Profile (`/pets`)
* `GET /pets` — List pets owned or cared for by authenticated user.
* `POST /pets` — Register new pet (Dog, Cat, Bovine, Equine, Avian, etc.).
* `GET /pets/{petId}` — Retrieve detailed pet profile, microchip, and medical summary.
* `PATCH /pets/{petId}` — Update permitted non-clinical profile fields (avatar, color, emergency contact).
* `POST /pets/{petId}/caregivers` — Invite secondary caregiver (co-owner, trainer, family member).
* `DELETE /pets/{petId}/caregivers/{caregiverId}` — Revoke secondary caregiver access.
* `POST /pets/{petId}/identifiers` — Register microchip or RFID tag with vet verification.
* `POST /pets/{petId}/transfer-ownership` — Initiate verified ownership transfer.

### 2.3 Universal Pet Health Passport & Consent Sharing (`/pets/{petId}/passport`)
* `GET /pets/{petId}/passport` — Retrieve digital Pet Health Passport overview and verified badges.
* `GET /pets/{petId}/passport/timeline` — Paginated comprehensive medical timeline (encounters, vaccines, labs).
* `GET /pets/{petId}/passport/vaccinations` — List verified vaccinations and upcoming booster schedule.
* `POST /pets/{petId}/passport/share-requests` — Create time-limited access grant for a new clinic or vet.
* `GET /record-share-requests` — List inbound/outbound record sharing requests for caregiver or vet.
* `POST /record-share-requests/{id}/approve` — Approve access scope (Full, Vaccines only, Emergency summary).
* `POST /record-share-requests/{id}/reject` — Reject record access request.
* `POST /record-share-requests/{id}/revoke` — Instantly revoke previously active clinic access.

### 2.4 Document & Medical File Vault (`/documents`)
* `POST /documents/upload-intent` — Generate secure, presigned S3 upload URL with MIME & size restrictions.
* `POST /documents/confirm-upload` — Confirm document upload, trigger anti-malware scan, categorize document.
* `GET /documents/{documentId}` — Get document metadata and verification status.
* `GET /documents/{documentId}/download` — Generate time-limited (10-minute) presigned download URL.
* `DELETE /documents/{documentId}` — Delete owner-uploaded personal documents (clinical signed records cannot be deleted).

### 2.5 Clinics & Directory (`/clinics`)
* `GET /clinics` — Discover clinics by geo-proximity, services offered, and 24x7 emergency status.
* `GET /clinics/{clinicId}` — Detailed clinic/hospital overview, facilities, licenses, and reviews.
* `GET /clinics/{clinicId}/branches` — List active branches for multi-branch veterinary hospitals.
* `GET /clinics/{clinicId}/services` — Available clinical services and consultation fees.
* `GET /clinics/{clinicId}/veterinarians` — Registered veterinary surgeons and specialties.

### 2.6 Appointments & Queue Management (`/appointments`)
* `GET /appointments/availability` — Search available 15/30-minute booking slots by branch, doctor, and date.
* `POST /appointments` — Reserve slot, lock concurrency, and generate consultation booking.
* `GET /appointments` — List user's upcoming and historical appointments.
* `GET /appointments/{appointmentId}` — Retrieve appointment token, queue position, and instructions.
* `POST /appointments/{appointmentId}/check-in` — Patient arrives at clinic, enters active waiting queue.
* `POST /appointments/{appointmentId}/reschedule` — Reschedule booking prior to cut-off window.
* `POST /appointments/{appointmentId}/cancel` — Cancel appointment with configured clinic refund policy.

### 2.7 Clinical Consultation & SOAP Workspace (`/encounters`)
* `POST /encounters` — Start new consultation encounter for checked-in patient.
* `GET /encounters/{encounterId}` — Retrieve full clinical history, vitals, and draft notes.
* `PATCH /encounters/{encounterId}` — Update SOAP fields (Subjective, Objective, Assessment, Plan).
* `POST /encounters/{encounterId}/sign` — Cryptographically sign encounter (renders record immutable).
* `POST /encounters/{encounterId}/amend` — Create documented, auditable amendment to signed record.
* `POST /encounters/{encounterId}/prescriptions` — Generate digital prescription with dose instructions.
* `POST /encounters/{encounterId}/vaccinations` — Record administered vaccine batch and auto-compute next due date.
* `POST /encounters/{encounterId}/diagnostic-orders` — Order lab tests or imaging panels.

### 2.8 Hospitalization, Wards & Nursing (`/admissions`)
* `POST /admissions` — Admit patient to ward/cage (ICU, Isolation, General Ward).
* `GET /admissions` — Active inpatient census board with alert levels and assigned veterinary surgeons.
* `GET /admissions/{admissionId}` — Inpatient chart, nursing log, and medication administration records.
* `POST /admissions/{admissionId}/observations` — Record TPR vitals, CRT, fluid intake, and nursing notes.
* `POST /admissions/{admissionId}/treatments` — Mark scheduled IV medications or treatments administered.
* `POST /admissions/{admissionId}/discharge` — Finalize discharge summary, home care, and medications.

### 2.9 Billing, Invoices & GST (`/invoices`, `/payments`)
* `POST /invoices` — Create itemized billing statement (consultations, diagnostics, medicines).
* `GET /invoices/{invoiceId}` — Retrieve invoice breakdown with SAC/HSN codes and CGST/SGST/IGST.
* `GET /invoices/{invoiceId}/pdf` — Download tax-compliant GST tax invoice PDF.
* `POST /payments/create-order` — Create Razorpay/UPI payment order for invoice settlement.
* `POST /payments/webhook` — Secure webhook endpoint verifying gateway signatures and marking invoice paid.
* `POST /payments/{paymentId}/refund` — Issue auditable refund or credit note.

### 2.10 MedShop Pharmacy & Inventory (`/pharmacy`)
* `GET /pharmacy/items` — Search medicine catalogue, stock levels, and Schedule H/H1 status.
* `POST /pharmacy/purchase-orders` — Create supplier purchase order.
* `POST /pharmacy/goods-receipt` — Receive stock batches with expiry date and purchase price.
* `POST /pharmacy/dispense` — Dispense prescription with batch deduction and Schedule H register logging.
* `GET /pharmacy/stock-ledger` — Comprehensive movement audit log (purchases, sales, adjustments).
* `GET /pharmacy/alerts/expiring` — Inventory batches expiring within 30, 60, or 90 days.

### 2.11 Pet Care Services Marketplace (`/marketplace`)
* `GET /marketplace/services` — List service categories (Walking, Grooming, Boarding, Training).
* `GET /marketplace/providers` — Find verified service providers by pincode and rating.
* `POST /marketplace/bookings` — Book service package with schedule and location notes.
* `POST /marketplace/bookings/{id}/complete` — Provider marks service complete with photo proof.
* `POST /marketplace/bookings/{id}/reviews` — Pet parent submits verified review and star rating.

### 2.12 Pet Insurance Discovery (`/insurance`)
* `GET /insurance/plans` — Browse licensed animal insurance plans and coverage terms.
* `POST /insurance/quotes` — Generate indicative premium quote based on animal age and breed.
* `GET /insurance/dossier/{petId}` — Generate exportable medical record dossier for insurance claim filing.

### 2.13 Platform Super Administration (`/admin`)
* `GET /admin/dashboard` — Global platform metrics (active pets, clinics, revenue, bookings).
* `GET /admin/clinics/pending-verification` — Review veterinary clinic license applications.
* `POST /admin/clinics/{clinicId}/verify` — Approve or reject clinic registration with audit rationale.
* `GET /admin/audit-logs` — Tamper-evident platform security event log.
* `GET /admin/subscriptions` — SaaS billing and clinic subscription plan management.
