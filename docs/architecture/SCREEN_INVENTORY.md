# Vetalya Multi-Application Screen Inventory & UI Specifications

## 1. Application 1: Pet Parent Mobile App (iOS & Android — Expo React Native)

| Screen ID | Screen Title | Key Functional Modules & UX Elements |
| :--- | :--- | :--- |
| **MOB-PP-01** | Splash & Brand Launch | Vetalya official logo, animated brand tagline *"Every Pet's Health. Always."*, session token verification. |
| **MOB-PP-02** | Onboarding Carousel | High-quality animal photography slides (Preventative health, digital health passport, instant clinic booking). |
| **MOB-PP-03** | Auth: Mobile Number & DLT OTP | Indian phone format validation (`+91`), 6-digit OTP entry, resend cooldown timer, terms consent checkbox. |
| **MOB-PP-04** | Caregiver Profile Setup | Owner full name, residential address, emergency contact, city/pincode for local clinic discovery. |
| **MOB-PP-05** | Pet Parent Home Dashboard | Active pet switcher, next appointment card, vaccination due alerts, medication reminders, quick action pills. |
| **MOB-PP-06** | Pet Profile Detail | Species selector (Dog, Cat, Cattle, Horse, Bird), breed, age/DOB, microchip number, neutered status, avatar upload. |
| **MOB-PP-07** | Add/Edit Pet Workflow | Form wizard for adding multi-species pets with species-tailored inputs (e.g. cattle lactation/breed vs dog breed). |
| **MOB-PP-08** | Digital Pet Health Passport | Persistent `VET-ID` badge, blood group, allergies alert banner, verified vaccination history timeline, share button. |
| **MOB-PP-09** | Medical Records Timeline | Chronological feed of clinical encounters, signed SOAP summaries, diagnostic test reports, and prescriptions. |
| **MOB-PP-10** | Document Vault & Camera Capture | Document categorizer (Vaccination cert, Lab report, X-Ray, Insurance), camera upload, preview, presigned download. |
| **MOB-PP-11** | Passport Consent & Record Sharing | Active share grants list, QR code scanner/display, share expiry selector (24h, 7d, permanent), instant revoke button. |
| **MOB-PP-12** | Clinic Discovery & Geo Search | Interactive map & list view, distance filter, 24x7 emergency filter, species filter, clinic rating & doctor count. |
| **MOB-PP-13** | Clinic & Veterinarian Profile | Clinic photos, operating hours, vet credentials (VCI no.), services menu, pricing schedule, reviews. |
| **MOB-PP-14** | Appointment Slot Booking Wizard | Select pet -> select consultation type -> select date -> select doctor -> select available time slot -> enter symptoms. |
| **MOB-PP-15** | Booking Review & Payment | Fee breakdown, Razorpay/UPI checkout option, pay at clinic option, terms acceptance, booking confirmation. |
| **MOB-PP-16** | Active Appointment & Live Queue | Token number display, estimated waiting time, live queue position (e.g., "2 patients ahead of you"), check-in button. |
| **MOB-PP-17** | Services Marketplace Hub | Expandable service catalogue (Dog Walking, Grooming, Boarding, Training, Ambulance transport). |
| **MOB-PP-18** | Service Booking & Walker Tracking | Package selection (one-time, weekly, monthly), provider profile, walk completion report with GPS route map. |
| **MOB-PP-19** | Pet Insurance Discovery & Dossier | Filter plans by age/breed, premium calculators, policy document vault, 1-click insurance claims medical export. |
| **MOB-PP-20** | Notification Preferences & Settings | Toggle Push, SMS, and WhatsApp alerts for vaccines, appointments, and marketing; language selection (EN/HI). |

---

## 2. Application 2: Veterinary Staff Mobile App (iOS & Android — Expo React Native)

| Screen ID | Screen Title | Key Functional Modules & UX Elements |
| :--- | :--- | :--- |
| **MOB-VS-01** | Staff Login & Branch Switcher | Dual credential / mobile OTP login, multi-branch selector for visiting doctors. |
| **MOB-VS-02** | Doctor Daily Schedule & Queue | Today's appointments timeline, patient status (Waiting, In-Consultation, Completed), next patient call button. |
| **MOB-VS-03** | Rapid Patient Search & Scanner | Search by Pet Name, Passport ID (`VET-ID`), Microchip number, or scan pet parent's Health Passport QR code. |
| **MOB-VS-04** | Mobile Consultation Workspace | Rapid clinical note capture, speech-to-text dictation, vital signs entry, diagnosis selector, camera upload for lesions. |
| **MOB-VS-05** | Mobile Quick Prescription | Approved drug search, rapid dosage calculator based on patient weight (`mg/kg`), frequency, instructions, digital sign. |
| **MOB-VS-06** | Mobile Inpatient Rounds | Ward/cage inpatient list, record instant TPR vitals, mark administered IV fluids/medications, nurse voice notes. |
| **MOB-VS-07** | Diagnostic Result Quick Review | Push alert for new lab results, view abnormal lab values flagged in red, approve report to Pet Parent Passport. |

---

## 3. Application 3: Veterinary Web Portal (React / TypeScript — Desktop & Tablet)

| Screen ID | Screen Title | Key Functional Modules & UX Elements |
| :--- | :--- | :--- |
| **WEB-VP-01** | Clinical Operations Command Center | Real-time reception queue, waiting room dashboard, doctor occupancy, day appointment grid, emergency triage banner. |
| **WEB-VP-02** | Comprehensive Patient Registry | Advanced multi-species patient directory, duplicate detection alert, merge review request, historical visits. |
| **WEB-VP-03** | Complete SOAP Consultation Suite | Dual-pane interface: Left pane = pet medical history & passport; Right pane = active SOAP encounter editor. |
| **WEB-VP-04** | Digital Rx & Drug Interaction Suite | Formularies database, weight-based dosage auto-calculation, contraindication warnings, Schedule H prescription PDF. |
| **WEB-VP-05** | Diagnostic Laboratory Manager | Test ordering, external lab routing, barcode sample tagging, PDF report attachment, doctor review workflow. |
| **WEB-VP-06** | Inpatient Hospitalization & Wards | Interactive cage board (ICU, Canine Ward, Feline Ward, Isolation), fluid infusion rate sheets, shift handover notes. |
| **WEB-VP-07** | Reception & Appointment Scheduler | Multi-doctor day/week grid view, walk-in token generation, slot blocking, rescheduling modal, SMS confirmation trigger. |
| **WEB-VP-08** | Fast OPD Billing & Cashier Desk | Encounter-linked auto-billing, diagnostic + pharmacy line items, GST breakdown (CGST/SGST), UPI QR display on terminal. |

---

## 4. Application 4: Clinic Administration Portal (React / TypeScript)

| Screen ID | Screen Title | Key Functional Modules & UX Elements |
| :--- | :--- | :--- |
| **WEB-CA-01** | Clinic Executive Dashboard | Monthly revenue vs expenses, patient footfall metrics, consultation conversion, top prescribed medicines, stock valuation. |
| **WEB-CA-02** | Organization & Multi-Branch Setup | Branch address, geo-coordinates, operating hours, emergency facilities, facility licenses, VCI compliance records. |
| **WEB-CA-03** | Staff Directory & Role Assignments | Invite doctors, nurses, receptionists; assign branch permissions, verify VCI registration numbers, deactivate leavers. |
| **WEB-CA-04** | Service Catalogue & Pricing Master | Manage OPD consultation rates, procedure fees, surgery packages, emergency surcharges, custom GST tax rules. |
| **WEB-CA-05** | MedShop Pharmacy Management | Supplier directory, Purchase Orders (PO), Goods Received Notes (GRN), Batch expiry tracking, Schedule H audit register. |
| **WEB-CA-06** | Financial & Tax Reporting Center | Daily sales summaries, outstanding receivables, GST GSTR-1 & GSTR-3B export reports, doctor revenue sharing reports. |
| **WEB-CA-07** | Security Audit & Compliance Logs | Complete log of staff login sessions, patient record views, invoice cancellations, and clinical record amendments. |

---

## 5. Application 5: Vetalya Super Admin Portal (React / TypeScript)

| Screen ID | Screen Title | Key Functional Modules & UX Elements |
| :--- | :--- | :--- |
| **WEB-SA-01** | Platform Health & Global Analytics | Nationwide platform stats: active pets, registered clinics, total consultations, gross merchandise value (GMV). |
| **WEB-SA-02** | Clinic Onboarding & Verification | Review clinic trade licenses, veterinary degree certificates, state council verification, 1-click approve/reject/suspend. |
| **WEB-SA-03** | Marketplace Operations & KYC | Verify dog walkers, pet groomers, boarding facilities; inspect identity documents, set commission percentages. |
| **WEB-SA-04** | Insurance Partners & Plan Master | Manage active insurance providers, update policy eligibility rules, view anonymous claim volumes. |
| **WEB-SA-05** | SaaS Subscription & Billing Hub | Clinic SaaS tier management (Starter, Clinic Pro, Hospital Enterprise), billing cycles, Razorpay subscription sync. |
| **WEB-SA-06** | Support Escalation & Dispute Center | Clinic disputes, owner complaints, provider dispute resolution, audited temporary break-glass access authorization. |
| **WEB-SA-07** | Platform Security & Audit Hub | Global immutable audit trail, failed login brute-force alerts, suspicious export detection, DPDP compliance data requests. |
