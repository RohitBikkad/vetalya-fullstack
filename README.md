# Vetalya — Digital Animal Healthcare Platform

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19.0-blue.svg)](https://react.dev/)
[![Tailwind CSS](https://img.shields.io/badge/TailwindCSS-v3.4-38B2AC.svg)](https://tailwindcss.com/)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)]()

Vetalya is an enterprise-grade digital animal healthcare platform connecting pet parents, veterinary doctors, clinic networks, and regulatory authorities into a unified clinical and healthcare ecosystem.

---

## 📁 Repository Structure

```
vetalya-fullstack/
├── Vetalaya_frontend/           # React 19 + Tailwind CSS + Vite Frontend
│   ├── src/
│   │   ├── components/
│   │   │   ├── auth/          # Unified Login, Dual Sign-up, Google GIS
│   │   │   ├── dashboard/     # Pet Parent, Veterinarian & Admin Portals
│   │   │   └── layout/        # Brand Header, Session Timer, Telemetry
│   │   ├── context/           # AuthContext (JWT, Refresh, Expiration)
│   │   └── services/          # API Client & Interceptors
│   ├── package.json
│   └── vite.config.js
│
└── Vetalya_backend/             # Spring Boot 3.4 Backend
    └── vetalya-backend/
        ├── src/
        │   ├── main/java/com/pet/vetalya/
        │   │   ├── auth/      # JWT, OAuth2, Refresh Token, OTP Service
        │   │   ├── config/    # SecurityFilterChain, CORS, DataInitializer
        │   │   ├── entity/    # JPA Entities (user_auth, pet_owner, vet, etc.)
        │   │   └── repository/# Spring Data JPA Repositories
        │   └── main/resources/# application.yml, Flyway migrations, RSA keys
        └── pom.xml
```

---

## 🚀 Key Features

### 1. Multi-Role Dashboards & Strict RBAC
- **Pet Parent Portal (`ROLE_PET_OWNER`)**: Digital Pet Passports, Vaccination & Deworming tracker, Appointment Booking, and SOAP Prescriptions.
- **Veterinarian OPD Consultation Desk (`ROLE_VET`)**: Live Patient Queue, SOAP Case Recording, Digital Rx Generator, and DICOM Diagnostic Imaging Viewer.
- **Platform Admin Console (`ROLE_PLATFORM_ADMIN`)**: Executive KPIs, Veterinary Council License Approvals, RBAC Token Invalidation, and Supabase Telemetry.

### 2. Dual Role-Tailored Sign-Up & Single Unified Login
- Distinct onboarding for Pet Parents (companion quick-add) and Veterinarians (Veterinary Council Registration Number, State Council, degrees).
- Single unified login with credential-driven role routing (zero client role pickers).
- Mobile & Email OTP verification support.
- Pre-seeded Super Admin account (`admin@vetalya.com`).

### 3. Banking-Grade Authentication & Session Security
- Asymmetric RS256 JWT access tokens (15-minute TTL).
- Cryptographically secure refresh tokens with automatic token rotation and family-based reuse detection.
- Live session countdown timer with automatic client logout on token expiration.
- Multi-device concurrent session management.
- Complete 18-scenario automated security verification suite.

---

## 🛠️ Getting Started

### Prerequisites
- **Java**: OpenJDK 21 or later
- **Node.js**: v18 or later (v20+ recommended)
- **PostgreSQL**: Supabase or local instance

### Backend Setup
```bash
cd Vetalya_backend/vetalya-backend
./mvnw clean install
./mvnw spring-boot:run
```
The backend API starts on `http://localhost:8080`.

### Frontend Setup
```bash
cd Vetalaya_frontend
npm install
npm run dev
```
The frontend dev server starts on `http://localhost:3000`.

---

## 🧪 Security & Auth Test Suite
Execute the automated test suite:
```bash
python3 scratch/run_full_test_suite.py
```
Passes 100% across all 18 authentication and threat mitigation scenarios.
