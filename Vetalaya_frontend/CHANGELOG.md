# Frontend Changelog

All notable changes to the **Vetalya** Frontend application will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [0.2.0] - 2026-11-11

### Added & Changed
- **Google OAuth 2.0 Identity Services**:
  - Configured verified Google Client ID (`1030204623930-s3t2s597p2rvf5nal0i3552vh8t10h23.apps.googleusercontent.com`).
  - Added native Google Sign-In button container in `AuthCard.jsx`.
  - Added `googleAuthApi` in `src/services/api.js` calling `POST /api/auth/google`.
- **API URL Alignment**:
  - Updated production API base URL in `.env.production` and `src/services/api.js` to `https://vetalya-fullstack.onrender.com/api`.
  - Configured environment variables in `.env`, `.env.production`, and `.env.example`.

---

## [0.1.0] - 2026-11-10

### Added
- **Multi-Channel OTP Verification**:
  - Integrated email OTP verification with Brevo backend.
  - Added simulated OTP display fallback for rapid development testing.
  - Added support for mobile registration with auto-detection.

---

## [0.0.1] - 2026-10-30

### Initial Release
- React 19 + Vite with Tailwind CSS.
- Role-based views: Pet Owner, Veterinarian, Admin, Clinic Staff.
- Modular navigation, responsive mobile drawers, and authentication modals.
