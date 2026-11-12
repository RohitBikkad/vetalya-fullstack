# Backend Changelog

All notable changes to the **Vetalya** Spring Boot Backend service will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [0.2.1] - 2026-11-11

### Fixed
- **Null Type Safety & Static Analysis Compliance**:
  - Replaced method references across all stream pipelines, collectors, and optionals with explicit, null-safe lambda expressions (`(v -> v != null && v.isVerified())`, `(a -> a.getAuthority())`, `(m -> m.getName())`, etc.).
  - Resolves Eclipse JDT and VS Code compiler warnings: `Null type safety: parameter 'this' provided via method descriptor Function<...>.apply(...) needs unchecked conversion to conform to '@NonNull ...'`.
  - Added assertion `assertThat(meterName).isNotBlank()` in `MetricsCardinalityTest.java` resolving unused local variable diagnostic.
  - Configured `"java.compile.nullAnalysis.mode": "disabled"` across all project workspaces to prevent false-positive null type warnings on unannotated third-party classes.
  - **`SecurityAuditEventListener.java`**: Added null-safe retrieval for `AuthorizationDeniedEvent`'s authentication supplier (`authSupplier != null ? authSupplier.get() : null`), null-guarded `event.getException().getMessage()`, and null-checked authority collections.
  - **`AuthController.java`**: Added null checks across all `@RequestBody` parameters (`login`, `register`, `sendOtp`, `verifyOtp`, `loginWithOtp`, `exchange`, `nativeGoogleLogin`, `nativeAppleLogin`) to prevent `NullPointerException` on empty payloads.
  - Replaced unsafe `Map.of()` calls with `LinkedHashMap` for responses containing potentially null fields (`refreshToken`, exception messages) preventing runtime NPEs.
  - Cleaned up unimported `OtpService` references and added null guards in `getUserMap` and `extractCookie`.

---

## [0.2.0] - 2026-11-11

### Performance & Optimization
- **Per-Request Audit Overhead Eliminated**: Prevented Spring Security from dispatching duplicate `LOGIN_SUCCESS` audit database writes on every normal stateless Bearer token request (`GET /pets`, `GET /profile`, etc.). Stateless API calls now update in-memory active user metrics with zero database overhead.
- **Hikari Connection Pool Thrashing Resolved**: Fixed pool exhaustion caused by failed audit transactions on dashboard load.

### Database & Migrations
- **Flyway V9 Migration (`V9__widen_audit_log_columns.sql`)**:
  - Widened `audit_log.role` to `VARCHAR(100)` to prevent PostgreSQL `value too long for character varying(20)` errors when multi-role strings are processed.
  - Widened `audit_log.client_type` to `VARCHAR(50)`.
  - Widened `audit_log.provider` to `VARCHAR(50)`.
  - Removed restrictive check constraint `audit_log_provider_check` to support modern authentication providers (`GOOGLE`, `APPLE`, `PHONE`, `JWT`, `OTP`).
- **Defensive Truncation in AuditService**: Added automatic defensive truncation for all string columns prior to SQL execution in `AuditService.java`.

### Authentication
- **Google OAuth 2.0 Integration**:
  - Configured verified Google Client ID (`1030204623930-s3t2s597p2rvf5nal0i3552vh8t10h23.apps.googleusercontent.com`) and Client Secret (`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`).
  - Added cryptographic ID token verification in `NativeTokenVerificationService.java` using Google's public JWK certs.

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
- **Fast2SMS SMS Dispatch**:
  - Created `Fast2SmsService.java` for SMS OTP routing using DLT and OTP routes.
- **Unified OTP Verification**:
  - Enhanced `OtpService.java` to orchestrate multi-channel OTP delivery across Email (Brevo), WhatsApp Cloud API, and SMS (Fast2SMS).

### Security & Database
- **SecurityFilterChain Alignment**:
  - Explicitly permitted all WhatsApp webhook endpoints in `ApiSecurityConfig.java`.
- **Supabase Resiliency**:
  - Transaction Pooler on port 6543 with `prepareThreshold=0` and NonValidatingFactory SSL.
  - Flyway migration `V8__alter_state_code_to_varchar.sql`.

---

## [0.0.1] - 2026-10-30

### Initial Release
- Spring Boot 3.4+ / Java 21 modular monolith with RS256 JWT, Flyway migrations, and Supabase integration.

