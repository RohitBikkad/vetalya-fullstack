# 🐾 Vetalya Backend — Enterprise Veterinary & Animal Healthcare Platform

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/Database-Supabase%20PostgreSQL-3ECF8E.svg)](https://supabase.com)
[![Render](https://img.shields.io/badge/Deploy-Render-46E3B7.svg)](https://render.com)
[![UptimeRobot](https://img.shields.io/badge/Monitoring-UptimeRobot-3BD671.svg)](https://dashboard.uptimerobot.com/monitors)
[![Security](https://img.shields.io/badge/Security-RS256%20JWT%20%2B%20OAuth2-blue.svg)]()
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)]()

Vetalya is a mission-critical, enterprise-grade digital animal healthcare backend. It delivers unified clinical management, telemedicine, role-based access control (RBAC), electronic pet health passports, appointment scheduling, pharmacy cataloging, multi-channel OTP alerts, and real-time observability.

---

## 📑 Table of Contents
1. [Architecture & Infrastructure Overview](#-architecture--infrastructure-overview)
2. [Technology Stack](#-technology-stack)
3. [Cloud Database: Supabase PostgreSQL](#-cloud-database-supabase-postgresql)
4. [Cloud Hosting: Render Web Service](#-cloud-hosting-render-web-service)
5. [24/7 Availability: UptimeRobot Keepalive Monitoring](#-247-availability-uptimerobot-keepalive-monitoring)
6. [Security & Authentication Architecture](#-security--authentication-architecture)
7. [External Telephony & Messaging Integrations](#-external-telephony--messaging-integrations)
8. [API Endpoints Reference](#-api-endpoints-reference)
9. [Environment Variables & Configuration](#-environment-variables--configuration)
10. [Local Development & Docker Setup](#-local-development--docker-setup)
11. [Observability, Health Checks & Metrics](#-observability-health-checks--metrics)

---

## 🏗️ Architecture & Infrastructure Overview

```
                      +─────────────────────────────────────────────+
                      |         UptimeRobot Cloud Monitor           |
                      |    (https://dashboard.uptimerobot.com)      |
                      |   Sends HTTP Keepalive Ping Every 5 Mins    |
                      +──────────────────────┬──────────────────────+
                                             | GET /actuator/health
                                             v
+──────────────────────+       +──────────────────────────────────────+
|   Vetalya Frontend   | ----> |     Render Cloud Web Service         |
|  (React 19 + Vite)   | HTTPS |      (Spring Boot 3.4.1 / JRE 21)    |
| (Static Site/Render) |       |  https://vetalya-backend.onrender.com|
+──────────────────────+       +──────────────────┬───────────────────+
                                                  |
              +───────────────────────────────────┼───────────────────────────────────+
              |                                   |                                   |
              v                                   v                                   v
+───────────────────────────+       +───────────────────────────+       +───────────────────────────+
|     Supabase Database     |       |   Google / Apple OAuth    |       |  Notification Providers   |
|  PostgreSQL 15+ (Pooled)  |       |   JWKS Token Validation   |       |  - Meta WhatsApp API      |
|  Flyway Migrations (V1-V9)|       |   RS256 Asymmetric Keys   |       |  - Brevo Email API        |
|  HikariCP Connection Pool |       |   Refresh Token Family    |       |  - Fast2SMS DLT India     |
+───────────────────────────+       +───────────────────────────+       +───────────────────────────+
```

---

## 💻 Technology Stack

| Category | Technology / Library | Version / Details | Purpose |
|---|---|---|---|
| **Core Platform** | Java OpenJDK | 21 LTS | High-performance virtual threads and modern language constructs |
| **Framework** | Spring Boot | 3.4.1 | Production-ready microservice foundation |
| **Security** | Spring Security | 6.x | RBAC, CSRF, filter chain, CORS configuration |
| **JWT & Crypto** | Nimbus JOSE + JWT / BouncyCastle | Latest | Asymmetric RS256 token signing and public-key verification |
| **Database** | PostgreSQL on Supabase | 15+ | Relational data persistence with strict foreign keys and ACID compliance |
| **Connection Pool**| HikariCP | Built-in | Optimized connection pooling (`VetalyaHikariPool`) |
| **Migrations** | Flyway Community Edition | 10.x (V1 → V9) | Automated, reproducible, forward-only database migrations |
| **Cloud Hosting** | Render | Docker Linux Container | Automated CI/CD deployment with blueprint specification |
| **Uptime Monitor** | UptimeRobot | Webhook / HTTP(S) | 5-minute health probe to prevent cold starts and ensure 99.9% uptime |
| **Google Auth** | Google Identity Services (GIS) | OAuth2 ID Token JWKS | Cryptographic token verification without third-party redirects |
| **WhatsApp** | Meta WhatsApp Cloud Graph API | v21.0 | Free 1,000 monthly automated patient reminders & OTP messages |
| **SMS** | Fast2SMS Gateway | Quick SMS Route | High-deliverability Indian mobile SMS OTP delivery |
| **Email** | Brevo API & Spring Mail (JavaMail) | REST + SMTP (Port 587) | Transactional OTP and password reset emails |
| **Observability** | Spring Boot Actuator & Micrometer | Prometheus Format | Metrics, liveness/readiness probes, and system telemetry |
| **Identifier** | ULID Creator | 5.2.3 | Lexicographically sortable 128-bit entity IDs |
| **Container** | Docker Multi-Stage | Alpine Linux | Lean production runtime footprint (<250MB image) |

---

## 🗄️ Cloud Database: Supabase PostgreSQL

Vetalya utilizes **[Supabase](https://supabase.com)** as its primary production PostgreSQL database.

### 1. Connection Pooling (PgBouncer)
To manage high concurrency and avoid connection exhaust errors on cloud platforms:
- **Pooler Host**: `aws-0-ap-southeast-1.pooler.supabase.com`
- **Pooler Port**: `6543` (Transaction pooler) / `5432` (Session pooler)
- **SSL Requirement**: `sslmode=require&sslfactory=org.postgresql.ssl.NonValidatingFactory`
- **Prepare Threshold**: `prepareThreshold=0` (Ensures compatibility with transaction-level connection pooling)

### 2. Automated Flyway Migrations
Database schema versioning is managed automatically on application startup via Flyway:
- `V1__init_auth_schema.sql` — `user_auth`, `roles`, `permissions`, `refresh_tokens`
- `V2__pet_owner_and_pets.sql` — Pet profiles, microchip IDs, pet passports
- `V3__vet_profiles_and_licensing.sql` — Veterinary council numbers, verification status, clinics
- `V4__consultations_and_prescriptions.sql` — OPD queues, SOAP case notes, digital Rx
- `V5__pharmacy_catalog_and_inventory.sql` — MedShop catalog, stock levels, orders
- `V6__otp_and_notification_logs.sql` — Multi-channel OTP verification and delivery logs
- `V7__security_audit_events.sql` — Asymmetric token rotation and audit events
- `V8__fix_duplicate_indexes.sql` — Optimization of high-traffic lookup indices
- `V9__align_auth_constraints.sql` — Normalization of role constraints and credential validation

---

## 🚀 Cloud Hosting: Render Web Service

The backend is deployed as a managed Docker container on **[Render](https://render.com)**.

- **Live Production URL**: `https://vetalya-backend.onrender.com` (or unified via `https://vetalya-fullstack.onrender.com`)
- **Runtime**: Multi-stage Docker container (JDK 21 build stage $\rightarrow$ JRE 21 Alpine runtime)
- **Deployment Spec**: Defined in [`render.yaml`](file:///Users/ganesh/Rohit/vetakya-backend/render.yaml) for 1-click declarative infrastructure.
- **Health Check Path**: `/api/public/db-inspect` or `/actuator/health`
- **Port**: Bound to dynamic port assigned via `$PORT` (defaults to `8080`)

### Multi-Stage Dockerfile Highlights
```dockerfile
# Build Stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
```

---

## ⏱️ 24/7 Availability: UptimeRobot Keepalive Monitoring

On Render's free or starter tiers, services automatically spin down (sleep) after 15 minutes of inactivity. When a user subsequently visits the site, the platform suffers from a 30 to 50-second **cold start** delay while the container boots up.

To solve this, **[UptimeRobot](https://dashboard.uptimerobot.com/monitors)** is configured as an automated 24/7 keepalive monitor.

### 1. How It Works
1. UptimeRobot's distributed global probe network sends an **HTTP GET / HEAD** request to `https://vetalya-backend.onrender.com/actuator/health` every **5 minutes**.
2. The health check triggers the Spring Boot lightweight health probe (`LivenessState` / `ReadinessState`), returning `HTTP 200 {"status":"UP"}`.
3. This periodic incoming traffic completely prevents Render from putting the web service to sleep.
4. If the database or backend encounters an outage, UptimeRobot immediately dispatches real-time alerts via Email, Slack, or Webhook.

### 2. Configuration Settings on UptimeRobot Dashboard
When managing monitors on [dashboard.uptimerobot.com/monitors](https://dashboard.uptimerobot.com/monitors):
- **Monitor Type**: `HTTP(s)`
- **Friendly Name**: `Vetalya Production Backend Health`
- **URL (or IP)**: `https://vetalya-backend.onrender.com/actuator/health` (or your active Render URL)
- **Monitoring Interval**: `Every 5 minutes`
- **Monitor Timeout**: `30 seconds`
- **HTTP Method**: `GET`
- **Expected Status Code**: `200`
- **Alert Contacts**: Primary dev/admin team email

---

## 🔐 Security & Authentication Architecture

### 1. Dual Sign-Up, Single Unified Login
- **Pet Parent Registration** (`/api/auth/register`): Collects personal details, auto-assigns `ROLE_PET_OWNER`, and initializes digital pet passports.
- **Veterinary Doctor Registration** (`/api/auth/register/vet`): Mandates Veterinary Council Registration Number, State Council, degree, clinic details, and creates a pending verification record (`is_verified = false`).
- **Single Unified Login** (`/api/auth/login`): Users log in using their email/phone and password. The backend securely checks their credentials and returns their authorized role in the payload. The client does not dictate roles.

### 2. Banking-Grade Asymmetric Token Security
- **Access Tokens**: Short-lived (15 minutes), signed with an asymmetric **RS256** private key (`keys/private_key.pem`). Verified across services with the public key (`keys/public_key.pem`).
- **Refresh Tokens**: Stored as cryptographically secure random hashes in the database with automatic rotation and family tracking.
- **Secure Cookie**: Refresh tokens are returned in an `HttpOnly`, `SameSite=None`, `Secure` cookie to eliminate XSS token theft.

### 3. Google Identity Services (GIS) Verification
- Frontend uses Google GIS in popup mode (`ux_mode: 'popup'`).
- The backend independently downloads and caches Google's public JSON Web Key Sets (JWKS) from `https://www.googleapis.com/oauth2/v3/certs`.
- Verifies audience (`aud`), issuer (`iss`), expiration, and cryptographic signature on the received `id_token` without relying on insecure client data.

---

## 📲 External Telephony & Messaging Integrations

### 1. Meta WhatsApp Cloud API (Graph API v21.0)
- Delivers rich appointment confirmations, vaccination alerts, and OTP codes directly to customer WhatsApp numbers.
- Uses the Meta Business Cloud API with 1,000 free monthly conversations.
- Template integration configured in `WhatsAppNotificationService.java`.

### 2. Fast2SMS Indian SMS Gateway
- Dedicated integration for high-speed Indian mobile phone OTP authentication.
- Sends Quick SMS alerts directly to 10-digit Indian mobile numbers without third-party app dependencies.

### 3. Brevo Transactional Email REST API
- Cloud email delivery configured to bypass standard cloud hosting outbound SMTP port 587 firewalls.
- Delivers account activations, emergency access codes, and password resets within seconds.

---

## 📡 API Endpoints Reference

### Public Authentication & Onboarding
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Pet parent sign-up |
| `POST` | `/api/auth/register/vet` | Veterinary doctor sign-up with license |
| `POST` | `/api/auth/login` | Unified credential login |
| `POST` | `/api/auth/google` | Google GIS ID Token authentication |
| `POST` | `/api/auth/refresh` | Rotate access token via HTTP-only cookie |
| `POST` | `/api/auth/logout` | Revoke refresh token and clear cookie |
| `POST` | `/api/auth/otp/send` | Request OTP via SMS / WhatsApp / Email |
| `POST` | `/api/auth/otp/verify` | Verify OTP code |

### Clinical & Pet Operations (`ROLE_PET_OWNER`, `ROLE_VET`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/owner/pets` | List registered companion animals |
| `POST` | `/api/owner/pets` | Register a new pet passport |
| `GET` | `/api/owner/appointments` | View scheduled clinic consultations |
| `POST` | `/api/owner/appointments` | Book an appointment with a verified vet |
| `GET` | `/api/vet/queue` | Live OPD consultation queue |
| `POST` | `/api/vet/consultation` | Record clinical SOAP notes and digital Rx |

### Pharmacy & Health Monitoring
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/medicines` | Search medicine catalog |
| `GET` | `/actuator/health` | System liveness and readiness probe |
| `GET` | `/actuator/prometheus` | Prometheus metric scrape target |

---

## ⚙️ Environment Variables & Configuration

Configure these environment variables in your local `.env`, Docker, or Render dashboard:

```properties
# ── Application & Port ──
SPRING_PROFILES_ACTIVE=prod
PORT=8080

# ── Supabase PostgreSQL Database ──
DB_URL=jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require&sslfactory=org.postgresql.ssl.NonValidatingFactory&prepareThreshold=0
DB_USERNAME=postgres.your_supabase_project
DB_PASSWORD=your_secure_db_password
DB_POOL_MAX=20
DB_POOL_MIN_IDLE=5

# ── Google OAuth 2.0 ──
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-client-secret

# ── CORS & Client Redirect ──
CORS_ALLOWED_ORIGIN=*
WEB_REDIRECT_URL=https://vetalya-frontend.onrender.com

# ── WhatsApp Cloud API ──
WHATSAPP_ACCESS_TOKEN=your_meta_system_user_token
WHATSAPP_PHONE_NUMBER_ID=1500405019813304
WHATSAPP_ENABLED=true

# ── Fast2SMS India ──
FAST2SMS_API_KEY=your_fast2sms_api_key
FAST2SMS_ENABLED=true

# ── Brevo / SMTP Email ──
BREVO_API_KEY=your_brevo_api_key
SPRING_MAIL_HOST=smtp-relay.brevo.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your_brevo_login
SPRING_MAIL_PASSWORD=your_brevo_smtp_key
```

---

## 💻 Local Development & Docker Setup

### Prerequisites
- **JDK 21** or later (`java -version`)
- **Maven 3.9+** (or use included `./mvnw`)
- **Docker & Docker Compose** (optional)

### Running Locally
```bash
# 1. Clone the repository
git clone https://github.com/ganesh-rohith/vetalya-backend.git
cd vetalya-backend

# 2. Build and verify dependencies
./mvnw clean install -DskipTests

# 3. Launch Spring Boot
./mvnw spring-boot:run
```

The server will initialize Flyway migrations against your configured database and listen on `http://localhost:8080`.

### Running with Docker Compose
```bash
docker-compose up --build
```

---

## 📊 Observability, Health Checks & Metrics

Vetalya exposes built-in production telemetry:
- **Liveness Probe**: `GET /actuator/health/liveness` $\rightarrow$ Confirms container process is responsive.
- **Readiness Probe**: `GET /actuator/health/readiness` $\rightarrow$ Validates database connection pool and migration state.
- **Prometheus Metrics**: `GET /actuator/prometheus` $\rightarrow$ Scrapes JVM memory, garbage collection, and HTTP latency percentiles ($p50, p95, p99$).

---

## 🛡️ License & Copyright
Copyright © 2026 Vetalya Health Technologies. All rights reserved.  
Proprietary software for animal healthcare and veterinary clinical management.
