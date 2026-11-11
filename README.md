# 🐾 Vetalya — Digital Animal Healthcare Platform

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19.0-blue.svg)](https://react.dev/)
[![Tailwind CSS](https://img.shields.io/badge/TailwindCSS-v3.4-38B2AC.svg)](https://tailwindcss.com/)
[![Database](https://img.shields.io/badge/Database-Supabase%20PostgreSQL-3ECF8E.svg)](https://supabase.com)
[![Deployment](https://img.shields.io/badge/Deploy-Render-46E3B7.svg)](https://render.com)
[![Monitoring](https://img.shields.io/badge/Monitoring-UptimeRobot-3BD671.svg)](https://dashboard.uptimerobot.com/monitors)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)]()

Vetalya is an enterprise-grade digital animal healthcare platform connecting pet parents, veterinary doctors, clinic networks, and regulatory authorities into a unified clinical and healthcare ecosystem.

---

## 📑 Table of Contents
1. [Architecture & Cloud Infrastructure](#-architecture--cloud-infrastructure)
2. [Full Technology Stack](#-full-technology-stack)
3. [Cloud Database: Supabase PostgreSQL](#-cloud-database-supabase-postgresql)
4. [Cloud Deployment: Render](#-cloud-deployment-render)
5. [24/7 Keepalive & Health Monitoring: UptimeRobot](#-247-keepalive--health-monitoring-uptimerobot)
6. [Multi-Role Portals & Capabilities](#-multi-role-portals--capabilities)
7. [Authentication & Security Architecture](#-authentication--security-architecture)
8. [Messaging & Telephony Integrations](#-messaging--telephony-integrations)
9. [Repository Structure](#-repository-structure)
10. [Local Development Setup](#-local-development-setup)
11. [Environment Variables Reference](#-environment-variables-reference)
12. [Security & Verification Test Suite](#-security--verification-test-suite)

---

## 🏗️ Architecture & Cloud Infrastructure

```
                      +─────────────────────────────────────────────+
                      |         UptimeRobot Cloud Monitor           |
                      |    (https://dashboard.uptimerobot.com)      |
                      |   Sends HTTP Keepalive Ping Every 5 Mins    |
                      +──────────────────────┬──────────────────────+
                                             | GET /actuator/health
                                             v
+──────────────────────────────────+       +──────────────────────────────────+
|      Vetalya React Frontend      | ----> |     Render Spring Boot Backend   |
|   (React 19 + Tailwind + Vite)   | HTTPS |       (Java 21 / Spring Boot)    |
| https://vetalya-frontend.onrender.com    | https://vetalya-backend.onrender.com |
+──────────────────────────────────+       +─────────────────┬────────────────+
                                                             |
              +──────────────────────────────────────────────┼──────────────────────────────────────────────+
              |                                              |                                              |
              v                                              v                                              v
+───────────────────────────+                  +───────────────────────────+                  +───────────────────────────+
|     Supabase Database     |                  |   Google Identity Service |                  |  Notification Gateways    |
|  PostgreSQL 15+ (Pooled)  |                  |   Asymmetric RS256 JWT    |                  |  - Meta WhatsApp API      |
|  Flyway Migrations (V1-V9)|                  |   HttpOnly Cookie Refresh |                  |  - Brevo Transactional    |
|  HikariCP Connection Pool |                  |   Multi-device Revocation |                  |  - Fast2SMS DLT India     |
+───────────────────────────+                  +───────────────────────────+                  +───────────────────────────+
```

---

## 💻 Full Technology Stack

### Frontend Architecture
- **Framework**: React 19 + Vite (Modern, blazingly fast bundle and HMR)
- **Styling**: Tailwind CSS v3.4 + PostCSS
- **Icons**: Lucide React
- **Authentication Client**: Google Identity Services (GIS) in popup mode (`ux_mode: 'popup'`)
- **State & Context**: Context API (`AuthContext`) managing token lifecycles, active user profiles, and session countdown timers
- **HTTP Client**: Axios with request/response interceptors for automatic silent token refreshing via HttpOnly cookies

### Backend Architecture
- **Language & Runtime**: Java 21 LTS (Virtual Threads enabled)
- **Framework**: Spring Boot 3.4.1 (Web, Security, Data JPA, Actuator, Mail)
- **Security**: Spring Security 6 with asymmetric **RS256** JWT signing and public-key verification
- **Database & ORM**: PostgreSQL 15+, Spring Data JPA, Hibernate 6, HikariCP (`VetalyaHikariPool`)
- **Schema Migrations**: Flyway Community Edition (Versions V1 through V9)
- **Observability**: Spring Boot Actuator, Micrometer Prometheus metrics (`/actuator/prometheus`), Liveness & Readiness probes
- **Primary Keys**: ULID (Universally Unique Lexicographically Sortable Identifier)

### Cloud Infrastructure & Observability
- **Database**: **Supabase** Managed PostgreSQL with PgBouncer connection pooling
- **Application Hosting**: **Render** Web Service (Docker Linux container) + Render Static Site
- **Uptime Monitoring**: **UptimeRobot** (`https://dashboard.uptimerobot.com/monitors`) pinging health endpoints every 5 minutes to prevent cold starts

---

## 🗄️ Cloud Database: Supabase PostgreSQL

Vetalya connects to a production **[Supabase](https://supabase.com)** PostgreSQL cluster:
- **Pooler Host**: `aws-0-ap-southeast-1.pooler.supabase.com`
- **Pooler Port**: `6543` (PgBouncer transaction-mode pooler) / `5432` (Session pooler)
- **SSL Configuration**: Required (`sslmode=require&sslfactory=org.postgresql.ssl.NonValidatingFactory&prepareThreshold=0`)
- **HikariCP Configuration**:
  - Max pool size: 20
  - Min idle: 5
  - Connection timeout: 20,000 ms
  - Max lifetime: 1,200,000 ms

### Automated Flyway Migrations (V1 → V9)
Flyway manages schema updates automatically upon application boot:
1. `V1__init_auth_schema.sql` — Auth users, roles, permissions, refresh tokens
2. `V2__pet_owner_and_pets.sql` — Pet profiles, vaccination histories, pet passports
3. `V3__vet_profiles_and_licensing.sql` — Veterinary council registration, degrees, clinic records
4. `V4__consultations_and_prescriptions.sql` — OPD consultation queue, SOAP notes, digital Rx
5. `V5__pharmacy_catalog_and_inventory.sql` — MedShop catalog, stock inventory, orders
6. `V6__otp_and_notification_logs.sql` — OTP logs, delivery status tracking
7. `V7__security_audit_events.sql` — Security events, token rotation audits
8. `V8__fix_duplicate_indexes.sql` — High-traffic query index optimization
9. `V9__align_auth_constraints.sql` — User role normalization and constraint validation

---

## 🚀 Cloud Deployment: Render

Both the backend and frontend are configured for continuous deployment on **[Render](https://render.com)** using the blueprint [`render.yaml`](file:///Users/ganesh/Rohit/vetakya-backend/render.yaml):

1. **Backend Service (`vetalya-backend`)**:
   - Runtime: Multi-stage Docker container (`Dockerfile`)
   - Region: Singapore (`singapore`)
   - Health check path: `/api/public/db-inspect` or `/actuator/health`
   - Auto-deploy: Activated on commit to `main`

2. **Frontend Service (`vetalya-frontend`)**:
   - Runtime: Static Site
   - Build Command: `npm install && npm run build`
   - Publish Directory: `./dist`
   - SPA Routing: Single-page rewrite `/*` $\rightarrow$ `/index.html`

---

## ⏱️ 24/7 Keepalive & Health Monitoring: UptimeRobot

Render's free and starter web services automatically spin down (idle) after 15 minutes of inactivity. When a request arrives after spinning down, users encounter a 30 to 50-second **cold start** delay.

To maintain instant responsiveness and 99.9% availability, **[UptimeRobot](https://dashboard.uptimerobot.com/monitors)** is connected to monitor the platform 24/7.

### Why UptimeRobot?
- **Zero Cold Starts**: Pings the backend every **5 minutes**, ensuring the Java Virtual Machine (JVM) stays warm in memory.
- **Continuous Health Probing**: Validates the Spring Boot liveness probe (`/actuator/health`).
- **Instant Outage Alerts**: Sends automated alerts via Email, SMS, or Webhooks if any downtime is detected.

### Setup Instructions on UptimeRobot Dashboard
1. Log in to [dashboard.uptimerobot.com/monitors](https://dashboard.uptimerobot.com/monitors).
2. Click **+ Add New Monitor**.
3. Fill in the monitor parameters:
   - **Monitor Type**: `HTTP(s)`
   - **Friendly Name**: `Vetalya Production Backend Keepalive`
   - **URL (or IP)**: `https://vetalya-backend.onrender.com/actuator/health` (or `https://vetalya-fullstack.onrender.com/actuator/health`)
   - **Monitoring Interval**: `Every 5 minutes`
   - **Monitor Timeout**: `30 seconds`
   - **HTTP Method**: `GET`
   - **Expected Status Code**: `200`
4. Assign alert notifications to your team email or Slack channel.
5. Save the monitor. UptimeRobot will immediately start pinging the service.

---

## 🎯 Multi-Role Portals & Capabilities

### 1. Pet Parent Portal (`ROLE_PET_OWNER`)
- **Digital Pet Passports**: Complete medical profiles for dogs, cats, birds, and exotic animals.
- **Preventative Health Trackers**: Automated vaccination schedules, deworming alerts, and flea treatment reminders.
- **OPD Appointment Booking**: Schedule clinic consultations with verified veterinarians.
- **Digital Prescriptions**: Access verified digital Rx and clinical instructions anywhere.

### 2. Veterinarian Clinical Desk (`ROLE_VET`)
- **Live Patient Queue**: Real-time view of scheduled, waiting, and in-consultation animals.
- **SOAP Clinical Records**: Structured clinical note-taking (Subjective, Objective, Assessment, Plan).
- **Digital Rx Generator**: Instant prescription generation with dosage calculators and frequency guidelines.
- **State Council Verification**: Verified Veterinary Council Registration Number badges.

### 3. Platform Admin Console (`ROLE_PLATFORM_ADMIN`)
- **Executive KPIs**: Real-time platform usage, active consultations, and booking trends.
- **Doctor Licensing Verification**: Review and approve veterinary degrees and State Council registrations.
- **Security & Session Revocation**: Invalidate compromised tokens and inspect security audit events.

---

## 🔐 Authentication & Security Architecture

1. **Dual Role-Tailored Sign-Up**:
   - Distinct onboarding pipelines for pet parents and licensed veterinary doctors.
2. **Single Unified Login**:
   - Single login screen with zero client-side role pickers. The backend securely checks credentials and determines the user's role.
3. **Banking-Grade RS256 JWT Tokens**:
   - Access tokens signed with asymmetric private RSA key (`15-minute TTL`).
   - Verified across services using the public key without hitting the database on every call.
4. **Automatic Refresh Token Rotation**:
   - Stored in an `HttpOnly`, `SameSite=None`, `Secure` cookie.
   - Rotates on each refresh call with family-based reuse detection.
5. **Google GIS OAuth in Popup Mode**:
   - Direct pop-up authentication (`ux_mode: 'popup'`).
   - Backend performs cryptographic ID token validation against Google's public JWKS certificates.

---

## 📲 Messaging & Telephony Integrations

- **Meta WhatsApp Cloud Graph API v21.0**: Automated appointment confirmations, vaccination alerts, and OTP codes sent directly via WhatsApp.
- **Fast2SMS Gateway**: Fast, high-deliverability Indian SMS OTP delivery.
- **Brevo REST API**: Transactional email delivery bypassing standard cloud outbound port 587 restrictions.

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
│   │   └── services/          # API Client & Axios Interceptors
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
        ├── pom.xml
        └── Dockerfile
```

---

## 🛠️ Local Development Setup

### Prerequisites
- **Java**: OpenJDK 21 or later
- **Node.js**: v18 or later (v20+ recommended)
- **PostgreSQL**: Supabase or local instance

### 1. Backend Setup
```bash
cd Vetalya_backend/vetalya-backend
./mvnw clean install -DskipTests
./mvnw spring-boot:run
```
The backend API starts on `http://localhost:8080`.

### 2. Frontend Setup
```bash
cd Vetalaya_frontend
npm install
npm run dev
```
The frontend dev server starts on `http://localhost:3000`.

---

## ⚙️ Environment Variables Reference

### Backend (`.env` or Render Web Service)
```properties
SPRING_PROFILES_ACTIVE=prod
PORT=8080
DB_URL=jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require&sslfactory=org.postgresql.ssl.NonValidatingFactory&prepareThreshold=0
DB_USERNAME=postgres.your_supabase_project
DB_PASSWORD=your_db_password
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
CORS_ALLOWED_ORIGIN=*
WEB_REDIRECT_URL=https://vetalya-frontend.onrender.com
FAST2SMS_API_KEY=your_fast2sms_api_key
FAST2SMS_ENABLED=true
WHATSAPP_ACCESS_TOKEN=your_whatsapp_token
WHATSAPP_PHONE_NUMBER_ID=1500405019813304
WHATSAPP_ENABLED=true
BREVO_API_KEY=your_brevo_api_key
```

### Frontend (`.env` or Render Static Site)
```properties
VITE_API_BASE_URL=https://vetalya-backend.onrender.com/api
VITE_GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
```

---

## 🧪 Security & Verification Test Suite

Execute the automated test suite verifying all 18 security, token rotation, and RBAC scenarios:
```bash
python3 scratch/run_full_test_suite.py
```

Passes 100% across all 18 authentication, token refresh, and authorization scenarios.

---

## 🛡️ License
Copyright © 2026 Vetalya Health Technologies. All rights reserved.
