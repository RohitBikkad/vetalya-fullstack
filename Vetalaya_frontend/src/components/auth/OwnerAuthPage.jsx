import React, { useEffect, useRef, useState } from "react";
import {
  ArrowLeft,
  ArrowUpRight,
  Heart,
  ShieldCheck,
  Eye,
  EyeOff,
  PawPrint,
} from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import "../dashboard/petparent/owner.css";

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
    <div className="owner-app owner-auth">
      <section className="owner-auth-story">
        <a
          className="owner-brand"
          href="/"
          onClick={(e) => {
            e.preventDefault();
            onNavigate("/");
          }}
        >
          <img className="brand-emblem" src="/vetalya-logo.png" alt="" />
          <img
            className="brand-wordmark"
            src="/vetalya-wordmark.png"
            alt="Vetalya · Every Pet. Health. Always."
          />
        </a>
        <div className="owner-auth-story-content">
          <span className="owner-hero-kicker">
            <span />A HEALTHIER, HAPPIER LIFE TOGETHER
          </span>
          <h1>
            A little care.
            <br />A lifetime
            <br />
            <em>together.</em>
          </h1>
          <p>
            Their first checkup. Their next adventure.
            <br />
            Their whole health story, close to your heart.
          </p>
          <div className="owner-auth-benefits">
            <div>
              <ShieldCheck size={19} />
              <span>One companion. One health passport.</span>
            </div>
            <div>
              <Heart size={19} />
              <span>Thoughtful care, every step of the way.</span>
            </div>
          </div>
        </div>
        <div className="owner-auth-art" aria-hidden="true">
          <Heart size={95} />
          <PawPrint size={55} />
        </div>
        <footer>Made for the ones who make your life whole.</footer>
      </section>
      <section className="owner-auth-form-panel">
        <header>
          <button className="owner-text-button" onClick={() => onNavigate("/")}>
            <ArrowLeft size={15} />
            Back to Vetalya
          </button>
          <span>For pet parents</span>
        </header>
        <div className="owner-auth-form">
          <p className="owner-eyebrow">YOUR COMPANION’S CARE SPACE</p>
          <h2>{registering ? "A beautiful beginning." : "Welcome back."}</h2>
          <p>
            {registering
              ? "Create your account. Their health story starts here."
              : "Your companion’s world is waiting for you."}
          </p>
          <div className="owner-auth-switch">
            <button
              className={!registering ? "active" : ""}
              onClick={() => {
                setMode("login");
                setError("");
              }}
            >
              Sign in
            </button>
            <button
              className={registering ? "active" : ""}
              onClick={() => {
                setMode("register");
                setError("");
              }}
            >
              Create an account
            </button>
          </div>
          {error && (
            <div role="alert" className="owner-alert">
              {error}
            </div>
          )}
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
                    ? "Create my care space"
                    : "Sign in to Vetalya"}
                <ArrowUpRight size={16} />
              </button>
            </div>
          </form>
          <div ref={google} className="owner-google-button" />
          {!registering && (
            <p className="owner-auth-help">
              Account recovery is not connected in this build. Use your existing
              account credentials.
            </p>
          )}
          <div className="owner-auth-trust">
            <ShieldCheck size={16} />
            <span>Your records stay connected to your account.</span>
          </div>
        </div>
        <footer>
          © {new Date().getFullYear()} Vetalya · Every Pet. Health. Always.
          <button onClick={() => onNavigate("/vet/login")}>
            Staff portal <ArrowUpRight size={12} />
          </button>
        </footer>
      </section>
    </div>
  );
}
