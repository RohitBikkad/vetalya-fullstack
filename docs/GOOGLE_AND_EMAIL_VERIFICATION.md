# Android and web authentication

Spring Boot remains the authentication server and uses the same Supabase staging database for web and Android. No client has database-owner credentials. iOS Google sign-in is deferred.

## Main branch configuration reviewed

Main uses Brevo HTTPS transactional email (`BREVO_API_KEY`, `SPRING_MAIL_FROM`) and optionally SMTP. Staging uses Brevo because SMTP ports are unsuitable for the Render free service. Real provider keys must be supplied in Render Environment, never recovered from Git history or committed. Staging no longer blanks the Brevo configuration. A provider failure returns a delivery error; no simulated email success is reported.

Google uses public web client ID `1030204623930-s3t2s597p2rvf5nal0i3552vh8t10h23.apps.googleusercontent.com`, matching main’s frontend. The web build receives `VITE_GOOGLE_CLIENT_ID` as a Docker ARG; the API receives `GOOGLE_CLIENT_ID`. Google ID token signature, issuer, expiry, audience, subject and verified email are checked server-side. Browser sessions use HttpOnly refresh cookies; Android uses SecureStore refresh tokens.

## Google Cloud configuration

- Authorized JavaScript origin: `https://vetalya-staging-z0u9.onrender.com`.
- Android OAuth client in the same Google project: package `com.vetalya.petowner.preview`.
- EAS staging certificate SHA-1: `25:CC:AD:A4:82:F0:06:1D:EF:3A:47:12:70:F5:E7:9D:C9:12:C4:3F`.
- The native SDK uses `EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID` as the server audience. No Google client secret is bundled in the app.
- If the OAuth consent screen remains in testing, add intended testers in Google Cloud. A different package/signing certificate requires a corresponding Android OAuth client.
- Native Google sign-in needs a rebuilt APK with Google Play Services; it does not work in Expo Go. No iOS signing or release is performed for this change.

## Email verification protocol

1. `POST /api/auth/otp/send` with email and purpose `REGISTRATION` sends a six-digit email code. Codes expire after 10 minutes; resends have a 60-second target cooldown.
2. `POST /api/auth/otp/verify` verifies the code and returns an opaque `emailVerificationToken`. Five incorrect attempts lock the code. Codes and receipts are not logged or returned by the send endpoint.
3. `POST /api/auth/register` includes that token. The token is bound to the normalized email, expires with the code, and is consumed once under a database row lock. Registration without it fails before account creation.

Flyway V15 widens hashed OTP storage, adds a hashed receipt field, indexes latest target/purpose lookup, and expires in-flight codes from the previous plaintext format. Existing accounts and signed clinical records are preserved. Verified email does not assert verified phone or professional credentials. Veterinarian registration remains pending professional verification.

User-reported secure environment and Google Cloud settings still require live delivery and device sign-in checks. Test results and release links are recorded in the staging release notes after deployment.
