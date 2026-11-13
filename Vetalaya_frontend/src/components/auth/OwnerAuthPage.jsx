import React, { useEffect, useRef, useState } from "react";
import {
  ArrowLeft,
  ArrowUpRight,
  Heart,
  ShieldCheck,
  Eye,
  EyeOff,
} from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import VetalyaBrandLogo from "./VetalyaBrandLogo";
import "./owner-auth.css";

export default function OwnerAuthPage({ initialMode = "login", onNavigate }) {
  const { login, signup, loginWithGoogle } = useAuth();
  const [mode, setMode] = useState(initialMode),
    [form, setForm] = useState({
      fullName: "",
      email: "",
      phone: "",
      password: "",
      confirmPassword: "",
    });
  const [visible, setVisible] = useState(false),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  const google = useRef();
  const registering = mode === "register";
  useEffect(() => {
    setMode(initialMode);
    setError("");
  }, [initialMode]);
  useEffect(() => {
    const id = import.meta.env.VITE_GOOGLE_CLIENT_ID;
    if (!id || id.includes("placeholder")) return;
    function initialize() {
      if (!window.google?.accounts?.id || !google.current) return;
      window.google.accounts.id.initialize({
        client_id: id,
        callback: async (response) => {
          if (response.credential) {
            try {
              await loginWithGoogle(response.credential, "PET_PARENT");
            } catch (e) {
              setError(e.message);
            }
          }
        },
      });
      window.google.accounts.id.renderButton(google.current, {
        type: "standard",
        theme: "outline",
        size: "large",
        text: "continue_with",
        shape: "rectangular",
        width: 300,
      });
    }
    initialize();
    const script = document.querySelector(
      'script[src="https://accounts.google.com/gsi/client"]',
    );
    script?.addEventListener("load", initialize);
    return () => script?.removeEventListener("load", initialize);
  }, [loginWithGoogle, mode]);
  const change = (key, value) => setForm((f) => ({ ...f, [key]: value }));
  async function submit(e) {
    e.preventDefault();
    setError("");
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      setError("Enter a valid email address.");
      return;
    }
    if (form.password.length < 8) {
      setError("Use a password of at least 8 characters.");
      return;
    }
    if (
      registering &&
      (!form.fullName.trim() ||
        !form.phone.trim() ||
        form.password !== form.confirmPassword)
    ) {
      setError(
        "Add your name and phone number, and make sure the passwords match.",
      );
      return;
    }
    setBusy(true);
    try {
      if (registering)
        await signup({
          ...form,
          fullName: form.fullName.trim(),
          email: form.email.trim().toLowerCase(),
          role: "ROLE_PET_OWNER",
        });
      else await login(form.email.trim().toLowerCase(), form.password);
    } catch (e) {
      setError(e.message || "We couldn’t sign you in. Please try again.");
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="owner-auth-page">
      <header className="owner-auth-nav">
        <a href="/" onClick={(e) => { e.preventDefault(); onNavigate("/"); }} aria-label="Vetalya home"><VetalyaBrandLogo /></a>
        <button className="owner-auth-home" onClick={() => onNavigate("/")}><ArrowLeft size={14} /> Home</button>
      </header>
      <main className="owner-auth-main">
        <section className="owner-auth-card" aria-labelledby="owner-auth-title">
          <div className="owner-auth-badge"><Heart size={14} /> Pet parent portal</div>
          <h1 id="owner-auth-title">{registering ? "Create your account" : "Pet Parent Login"}</h1>
          <p className="owner-auth-intro">{registering ? "One account for your pets, appointments and health records." : "Welcome back. Your pet’s care, all in one place."}</p>
          <div className="owner-auth-tabs" aria-label="Account access">
            <button type="button" aria-pressed={!registering} className={!registering ? "active" : ""} onClick={() => { setMode("login"); setError(""); }}>Log in</button>
            <button type="button" aria-pressed={registering} className={registering ? "active" : ""} onClick={() => { setMode("register"); setError(""); }}>Sign up</button>
          </div>
          {error && <div role="alert" className="owner-auth-error">{error}</div>}
          <form onSubmit={submit}>
            <div className="owner-form-grid">
              {registering && (
                <label className="wide">
                  <span>Your full name</span>
                  <input
                    required
                    autoComplete="name"
                    maxLength={200}
                    placeholder="First and last name"
                    value={form.fullName}
                    onChange={(e) => change("fullName", e.target.value)}
                  />
                </label>
              )}
              <label className="wide">
                <span>Email address</span>
                <input
                  required
                  type="email"
                  autoComplete="email"
                  placeholder="you@example.com"
                  value={form.email}
                  onChange={(e) => change("email", e.target.value)}
                />
              </label>
              {registering && (
                <label className="wide">
                  <span>Mobile phone</span>
                  <input
                    required
                    type="tel"
                    autoComplete="tel"
                    maxLength={20}
                    placeholder="Your contact number"
                    value={form.phone}
                    onChange={(e) => change("phone", e.target.value)}
                  />
                </label>
              )}
              <label className="wide">
                <span>Password</span>
                <div className="owner-password">
                  <input
                    required
                    type={visible ? "text" : "password"}
                    minLength={8}
                    autoComplete={
                      registering ? "new-password" : "current-password"
                    }
                    placeholder={
                      registering ? "At least 8 characters" : "Your password"
                    }
                    value={form.password}
                    onChange={(e) => change("password", e.target.value)}
                  />
                  <button
                    type="button"
                    aria-label={visible ? "Hide password" : "Show password"}
                    onClick={() => setVisible(!visible)}
                  >
                    {visible ? <EyeOff size={17} /> : <Eye size={17} />}
                  </button>
                </div>
              </label>
              {registering && (
                <label className="wide">
                  <span>Confirm password</span>
                  <input
                    required
                    minLength={8}
                    type={visible ? "text" : "password"}
                    autoComplete="new-password"
                    placeholder="Enter your password again"
                    value={form.confirmPassword}
                    onChange={(e) => change("confirmPassword", e.target.value)}
                  />
                </label>
              )}
              <button
                className="owner-button wide"
                disabled={busy}
                style={{ width: "100%", marginTop: 4 }}
              >
                {busy
                  ? "Please wait…"
                  : registering
                    ? "Create account"
                    : "Sign in to Vetalya"}
                <ArrowUpRight size={16} />
              </button>
            </div>
          </form>
          <div ref={google} className="owner-google-button" />

          <p className="owner-auth-security"><ShieldCheck size={15} /> Your pet’s records stay connected to your account.</p>
        </section>
        <p className="owner-auth-staff">Veterinarian or hospital staff? <button onClick={() => onNavigate("/vet/login")}>Visit the staff portal <ArrowUpRight size={13} /></button></p>
      </main>
      <footer className="owner-auth-footer">© {new Date().getFullYear()} Vetalya · Every Pet. Health. Always.</footer>
    </div>
  );
}
