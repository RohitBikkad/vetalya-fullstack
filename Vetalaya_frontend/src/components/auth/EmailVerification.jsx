import React, { useEffect, useRef, useState } from "react";
import { sendOtpApi, verifyOtpApi } from "../../services/api";
export default function EmailVerification({ email, onVerified }) {
  const currentEmail = useRef(email);
  const [code, setCode] = useState("");
  const [sent, setSent] = useState(false);
  const [verified, setVerified] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [cooldown, setCooldown] = useState(0);
  useEffect(() => { currentEmail.current = email; setCode(""); setSent(false); setVerified(false); setError(""); setCooldown(0); onVerified(""); }, [email, onVerified]);
  useEffect(() => { if (!cooldown) return; const timer = setTimeout(() => setCooldown(cooldown - 1), 1000); return () => clearTimeout(timer); }, [cooldown]);
  async function action(verify) {
    setBusy(true); setError("");
    try {
      const target = email.trim().toLowerCase();
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(target)) throw new Error("Enter a valid email address first.");
      if (verify) {
        const data = await verifyOtpApi(target, code, "REGISTRATION");
        if (!data.emailVerificationToken) throw new Error("Email verification failed. Request a new code.");
        if (currentEmail.current.trim().toLowerCase() !== target) return;
        onVerified(data.emailVerificationToken); setVerified(true);
      } else { await sendOtpApi(target, "REGISTRATION"); if (currentEmail.current.trim().toLowerCase() !== target) return; setSent(true); setCooldown(60); }
    } catch (e) { setError(e.message || "Unable to verify email."); }
    finally { setBusy(false); }
  }
  return <div className="email-verification" style={{ display: "grid", gap: 10, margin: "6px 0 16px", fontSize: 12 }}>
    {verified ? <p role="status" style={{ color: "#167355", margin: 0 }}>Email verified</p> : <>
      <button type="button" disabled={busy || cooldown > 0} onClick={() => action(false)} style={{ color: "#10324E", textAlign: "left", fontWeight: 700 }}>{cooldown ? `Resend in ${cooldown}s` : sent ? "Resend email code" : "Send email verification code"}</button>
      {sent && <><p role="status" style={{ margin: 0 }}>Check your inbox and spam folder. The code expires in 10 minutes.</p>
        <label style={{ display: "grid", gap: 6 }}>6-digit email code<input value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))} inputMode="numeric" autoComplete="one-time-code" maxLength={6} style={{ padding: 12, border: "1px solid #D8DFE5", borderRadius: 12 }} /></label>
        <button type="button" disabled={busy || code.length !== 6} onClick={() => action(true)} style={{ color: "#10324E", textAlign: "left", fontWeight: 700 }}>{busy ? "Please wait…" : "Verify email"}</button></>}
    </>}
    {error && <p role="alert" style={{ color: "#9f1239", margin: 0 }}>{error}</p>}
  </div>;
}
