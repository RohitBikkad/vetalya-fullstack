# Changelog

All notable changes to the **Vetalya** platform (Backend & Frontend) will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Planned
- WhatsApp template messages for appointment reminders & vaccination alerts
- Payment gateway webhook integration (Razorpay / Stripe)
- Automated CI/CD deployment verification test suites

---

## [0.2.0] - 2026-11-11

### Performance Optimizations
- **Eliminated Per-Request Audit Write Overhead**: Prevented Spring Security from dispatching duplicate `LOGIN_SUCCESS` audit database writes on every normal stateless Bearer token request (`GET /pets`, `GET /profile`, `GET /vaccinations/due`, etc.). Stateless API calls now update in-memory active user metrics instantly with zero database overhead.
- **Resolved Database Connection Pool Thrashing**: Fixed Hikari pool exhaustion (`VetalyaHikariPool`) caused by failing audit log transactions on dashboard loads, dropping API latency from ~4.5s down to standard sub-second response times.

### Database & Migrations
- **Flyway V9 Migration (`V9__widen_audit_log_columns.sql`)**:
  - Widened `audit_log.role` to `VARCHAR(100)` to prevent PostgreSQL `value too long for character varying(20)` errors when multi-role strings are processed.
  - Widened `audit_log.client_type` to `VARCHAR(50)`.
  - Widened `audit_log.provider` to `VARCHAR(50)`.
  - Removed restrictive check constraint `audit_log_provider_check` to seamlessly support modern authentication providers (`GOOGLE`, `APPLE`, `PHONE`, `JWT`, `OTP`).
- **Defensive Truncation in AuditService**: Added automatic defensive truncation for all string columns in `AuditService.java` prior to SQL execution to guarantee resilience against DB exceptions.

### Authentication & Integrations
- **Google OAuth 2.0 Integration**:
  - Configured verified Google Client ID (`1030204623930-s3t2s597p2rvf5nal0i3552vh8t10h23.apps.googleusercontent.com`) and Client Secret (`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`).
  - Updated frontend Google Identity Services button in `AuthCard.jsx`.
  - Updated backend JWT cryptographic verification in `NativeTokenVerificationService` and `application.yml`.
  - Synced client configuration across `.env`, `.env.production`, and `.env.example`.

### Frontend & Routing
- **Render Production URL Alignment**:
  - Updated `VITE_API_BASE_URL` in `.env.production` and `src/services/api.js` from deprecated `vetalaya-v2-1.onrender.com` to the active production backend service `https://vetalya-fullstack.onrender.com/api`.

---

## [0.1.0] - 2026-11-10

### Added
- **Meta WhatsApp Cloud API Integration**:
  - Implemented `WhatsAppService.java` to dispatch mobile OTPs via WhatsApp Graph API `v21.0`.
  - Created `WhatsAppWebhookController.java` to handle Meta's handshake verification (`GET`) and event delivery receipts (`POST`).
  - Added support for both `/api/public/whatsapp/webhook` and `/api/auth/whatsapp/webhook` with optional trailing slash support (`/webhook/`).
  - Configured `WHATSAPP_VERIFY_TOKEN` (`vetalya_whatsapp_secret`) and mapped environment variables in `render.yaml`.
- **Brevo REST API Transactional Email**:
  - Implemented `EmailService.java` with Brevo v3 HTTP API over port 443 (`POST https://api.brevo.com/v3/smtp/email`) to bypass Render's outbound SMTP port 587 block.
  - Added fallback to standard Spring JavaMailSender for local development.
- **Fast2SMS SMS Dispatch**:
  - Created `Fast2SmsService.java` for SMS OTP routing using DLT and OTP routes.
- **Unified OTP Verification**:
  - Enhanced `OtpService.java` to orchestrate multi-channel OTP delivery across Email (Brevo), WhatsApp Cloud API, and SMS (Fast2SMS).

### Security & Infrastructure
- **SecurityFilterChain Alignment**:
  - Updated `ApiSecurityConfig.java` to explicitly permit all WhatsApp webhook endpoints and authentication endpoints without requiring Bearer tokens.
  - Fixed Spring Security ordering conflicts between MedShop store filter chain (`@Order(1)`) and main API chain (`@Order(2)`).
- **Supabase Database Connection Resiliency**:
  - Switched database connectivity to Supabase Transaction Pooler on port 6543 with `prepareThreshold=0` and NonValidatingFactory SSL.
  - Added `ApplicationEnvironmentPreparedEvent` self-healing database listener in `VetalyaApplication.java` to detect IP addresses and auto-correct obsolete database credentials.
  - Added Flyway migration `V8__alter_state_code_to_varchar.sql` to align entity definitions with database schema.

---

## [0.0.1] - 2026-10-30

### Initial Fullstack Release
- Spring Boot 3.4+ / Java 21 modular monolith architecture with domain-driven packages (`vet`, `owner`, `medshop`, `audit`, `auth`).
- React 19 + Vite frontend with Tailwind CSS and role-based portals (Pet Owner, Veterinarian, Admin, Clinic).
- RS256 asymmetric JWT authentication with RSA keypair generation.
- Supabase PostgreSQL integration with Flyway database migrations (`V1` through `V7`).
- Render Docker container deployment configuration with multi-stage build.

