# Vetalya Legal, Clinical & Regulatory Validation Blueprint (India Market)

## 1. Regulatory Context & Advisory Statement
Software code alone does not constitute legal or statutory compliance. The technical mechanisms designed in Vetalya provide the auditable controls, boundaries, and data architectures required to support compliance with Indian law, subject to mandatory ongoing review by qualified legal counsel, veterinary regulatory advisors, and chartered accountants.

---

## 2. Key Regulatory Frameworks & Platform Safeguards

### 2.1 Digital Personal Data Protection Act (DPDP Act 2023 & Rules)
* **Caregiver Consent:** Clear, affirmative consent captured before creating user profiles, storing caregiver phone numbers, or sharing pet medical records.
* **Purpose Limitation:** Caregiver data collected for veterinary healthcare cannot be automatically repurposed for third-party marketplace marketing without explicit separate opt-in consent.
* **Data Principal Rights:** Workflows and APIs provided for:
  - Right to Access summary of personal data.
  - Right to Correction of inaccurate caregiver profile data.
  - Right to Erasure (Caregiver personal identity can be anonymized upon request; however, clinical encounters and Schedule H registers must be retained under statutory medical/pharmaceutical record retention obligations).
* **Grievance Redressal Officer:** Designated contact point in terms of service and super admin portal.

### 2.2 Veterinary Council of India (VCI) & State Veterinary Councils
* **Veterinary Surgeon Verification:** Every prescribing veterinarian must submit their VCI or State Veterinary Council registration number, registered state, and year of qualification. Unverified profiles cannot sign prescriptions or finalize clinical encounters.
* **Veterinary Prescription Validity:** Prescriptions must bear the prescribing doctor’s full name, registration number, clinic address, patient details, species, weight, date, medicine name, strength, dosage instructions, and digital signature/hash.
* **Teleconsultation Constraints:** Under Indian veterinary guidelines, teleconsultation is strictly restricted to follow-up consultations or triage where a prior in-person veterinary-client-patient relationship (VCPR) has been established, or emergency first-aid triage advice. First-time prescription of critical drugs via teleconsultation is blocked by the system.

### 2.3 Drugs & Cosmetics Act, 1940 & Rules (Schedule H & H1 Registers)
* **Schedule H & H1 Drugs:** Antibiotics, sedatives, narcotics, and specialized veterinary pharmaceuticals require explicit prescription records and must be logged in an immutable register.
* **MedShop Register:**
  - Date of dispensing.
  - Name and address of the animal caregiver.
  - Name of the pet / species.
  - Name and registration number of the prescribing veterinary surgeon.
  - Drug name, batch number, manufacturer, and quantity dispensed.
* **No Autonomous Dispensing:** The software does not auto-dispense drugs without human pharmacist confirmation or licensed vet sign-off.

### 2.4 Goods and Services Tax (GST) Treatment
* **Veterinary Healthcare Exemption:** Under Notification No. 12/2017-Central Tax (Rate), healthcare services in relation to the treatment of animals are typically exempt from GST.
* **Commercial Services Taxable:** Pet grooming, dog walking, boarding, pet relocation, and retail pet accessories are taxable at applicable GST rates (18%).
* **Pharmaceuticals:** Medicines and surgical consumables carry specific HSN codes with GST rates (0%, 5%, 12%, or 18%).
* **Invoicing Separation:** The system automatically segregates exempt veterinary healthcare services from taxable commercial products and services on composite clinic invoices.

### 2.5 Insurance Regulatory and Development Authority of India (IRDAI)
* **Discovery & Dossier Only:** Vetalya acts as a technology platform providing plan information and generating structured claim documentation dossiers (medical timelines, bills, vaccine cards).
* **No Underwriting:** Vetalya does not act as an unlicensed insurance broker or underwrite policies. Policy issuance and claim approvals remain entirely with IRDAI-registered insurance companies.

### 2.6 Government Pet & Livestock Identity Integration (Pashu Aadhaar / INAPH / NDDB)
* **Current Status:** Government animal identity systems (e.g. 12-digit Pashu Aadhaar ear tags for livestock) are administered under Department of Animal Husbandry and Dairying (DAHD) and National Dairy Development Board (NDDB).
* **Architectural Boundary:** Vetalya provides an abstracted identifier model (`identifier_type = PASHU_AADHAAR`) with manual vet verification. Direct government API integration remains an architectural adapter hook that will only be activated when formal public API access, security clearances, and official data-sharing agreements are signed with relevant government authorities. Vetalya does not claim official government endorsement.
