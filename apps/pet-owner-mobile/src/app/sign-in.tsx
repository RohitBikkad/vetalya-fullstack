import React, { useEffect, useState } from "react";
import {
  Image,
  Pressable,
  ScrollView,
  Text,
  View,
  KeyboardAvoidingView,
  Platform,
  StyleSheet,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Redirect } from "expo-router";
import { useOwner } from "../OwnerContext";
import { Input, ErrorNotice, styles } from "../ui";
import { client, apiBaseUrl } from "../api";
import { googleIdToken, googleSignInAvailable } from "../google-sign-in";
export default function SignIn() {
  const { user, signIn, signInWithGoogle } = useOwner();
  const [register, setRegister] = useState(false),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const [form, setForm] = useState({
    fullName: "",
    email: "",
    phone: "",
    password: "",
    confirmation: "",
  });
  const [verificationEmail, setVerificationEmail] = useState("");
  const [emailCode, setEmailCode] = useState("");
  const [emailProof, setEmailProof] = useState("");
  const [resendIn, setResendIn] = useState(0);
  useEffect(() => { if (resendIn <= 0) return; const timer = setTimeout(() => setResendIn(resendIn - 1), 1000); return () => clearTimeout(timer); }, [resendIn]);
  async function sendCode(email: string) {
    await client().sendEmailOtp(email);
    setVerificationEmail(email);
    setEmailCode("");
    setEmailProof("");
    setResendIn(60);
  }
  async function resend() {
    setBusy(true);
    setError("");
    try {
      await sendCode(verificationEmail);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Couldn’t send the code.");
    } finally {
      setBusy(false);
    }
  }
  async function finishSignup() {
    if (busy || (!emailProof && emailCode.length !== 6)) return;
    setBusy(true);
    setError("");
    try {
      let proof = emailProof;
      if (!proof) {
        const result = await client().verifyEmailOtp(verificationEmail, emailCode);
        proof = result.emailVerificationToken;
        if (!proof) throw new Error("Couldn’t verify your email. Request a new code.");
        setEmailProof(proof);
      }
      await signIn({
        fullName: form.fullName.trim(),
        email: verificationEmail,
        phone: form.phone.trim(),
        password: form.password,
        emailVerificationToken: proof,
      }, true);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Couldn’t create your account.");
    } finally {
      setBusy(false);
    }
  }
  async function googleSignIn() {
    setBusy(true); setError("");
    try { const token = await googleIdToken(); if (token) await signInWithGoogle(token); }
    catch (e) { setError(e instanceof Error ? e.message : "Google sign-in failed. Please try again."); }
    finally { setBusy(false); }
  }
  if (user) return <Redirect href="/(tabs)" />;
  function field(key: keyof typeof form, value: string) {
    setForm((v) => ({ ...v, [key]: value }));
  }
  async function submit() {
    if (busy) return;
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
      register &&
      (!form.fullName.trim() ||
        !form.phone.trim() ||
        form.password !== form.confirmation)
    ) {
      setError(
        "Add your name and phone number, and make sure the passwords match.",
      );
      return;
    }
    setBusy(true);
    try {
      const email = form.email.trim().toLowerCase();
      if (register) await sendCode(email);
      else await signIn({ email, password: form.password });
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to sign in.");
    } finally {
      setBusy(false);
    }
  }
  return (
    <SafeAreaView style={[styles.screen, auth.screen]}>
      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === "ios" ? "padding" : undefined}
      >
        <ScrollView
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={[styles.content, auth.content]}
        >
          <View style={styles.row}>
            <Image
              source={require("../../assets/vetalya-logo.png")}
              style={{ width: 50, height: 50 }}
            />
            <Image
              source={require("../../assets/vetalya-wordmark.png")}
              style={{ width: 125, height: 50, resizeMode: "contain" }}
              accessibilityLabel="Vetalya · Every Pet. Health. Always."
            />
          </View>
          <View style={auth.card}>
          {verificationEmail ? <>
            <Text accessibilityRole="header" style={[styles.title, auth.title]}>Verify your email</Text>
            <Text style={styles.body}>Enter the 6-digit code sent to {verificationEmail}. The code expires in 10 minutes.</Text>
            {error && <ErrorNotice message={error} />}
            {!emailProof && <Input label="Email verification code" autoFocus editable={!busy}
              value={emailCode} onChangeText={(value) => setEmailCode(value.replace(/\D/g, ""))}
              keyboardType="number-pad" maxLength={6} autoComplete="one-time-code" />}
            <Pressable accessibilityRole="button" disabled={busy || (!emailProof && emailCode.length !== 6)}
              onPress={finishSignup} style={auth.submit}>
              <Text style={auth.submitText}>{busy ? "Please wait…" : emailProof ? "Retry account creation" : "Verify and create account"}</Text>
            </Pressable>
            <Pressable accessibilityRole="button" disabled={busy || resendIn > 0} onPress={resend}>
              <Text style={styles.link}>{resendIn ? `Resend in ${resendIn}s` : "Resend code"}</Text>
            </Pressable>
            <Pressable accessibilityRole="button" disabled={busy} onPress={() => {
              setVerificationEmail("");
              setEmailProof("");
              setEmailCode("");
              setError("");
            }}>
              <Text style={styles.link}>Back to account details</Text>
            </Pressable>
          </> : <>
          <Text style={auth.badge}>PET PARENT PORTAL</Text>
          <View style={{ gap: 8 }}>
            <Text style={[styles.title, auth.title]}>{register ? "Create your account" : "Pet Parent Login"}</Text>
            <Text style={styles.body}>{register ? "One account for your pets, appointments and health records." : "Welcome back. Your pet’s care, all in one place."}</Text>
          </View>
          <View style={auth.tabs}>
            {[false, true].map((value) => <Pressable key={String(value)} accessibilityRole="button" accessibilityState={{ selected: register === value }} disabled={busy} onPress={() => { setRegister(value); setError(""); }} style={[auth.tab, register === value && auth.activeTab]}><Text style={[auth.tabText, register === value && { color: "#10324E" }]}>{value ? "Sign up" : "Log in"}</Text></Pressable>)}
          </View>
          {error && <ErrorNotice message={error} />}
          <View style={{ gap: 17 }}>
            {register && (
              <Input
                label="Your full name"
                value={form.fullName}
                onChangeText={(v) => field("fullName", v)}
                editable={!busy}
                autoComplete="name"
              />
            )}
            <Input
              label="Email address"
              value={form.email}
              onChangeText={(v) => field("email", v)}
              editable={!busy}
              autoCapitalize="none"
              keyboardType="email-address"
              autoComplete="email"
            />
            {register && (
              <Input
                label="Mobile phone"
                value={form.phone}
                onChangeText={(v) => field("phone", v)}
                editable={!busy}
                keyboardType="phone-pad"
                autoComplete="tel"
              />
            )}
            <Input
              label="Password"
              value={form.password}
              onChangeText={(v) => field("password", v)}
              editable={!busy}
              secureTextEntry
              autoComplete={register ? "new-password" : "current-password"}
            />
            {register && (
              <Input
                label="Confirm password"
                value={form.confirmation}
                onChangeText={(v) => field("confirmation", v)}
                editable={!busy}
              secureTextEntry
              />
            )}
            <Pressable accessibilityRole="button" disabled={busy} onPress={submit} style={[auth.submit, busy && { opacity: 0.6 }]}><Text style={auth.submitText}>{busy ? "Please wait…" : register ? "Create account" : "Sign in to Vetalya"}</Text></Pressable>
            <Pressable
              disabled={busy}
              onPress={() => {
                setRegister(!register);
                setError("");
              }}
            >
              <Text style={[styles.link, { textAlign: "center", padding: 10 }]}>
                {register
                  ? "Already have an account? Sign in"
                  : "New to Vetalya? Create an account"}
              </Text>
            </Pressable>
          </View>
          {googleSignInAvailable && <Pressable accessibilityRole="button" disabled={busy} onPress={googleSignIn} style={[auth.tab, { borderWidth: 1, borderColor: "#D8DFE5", padding: 14 }]}><Text style={[auth.tabText, { color: "#10324E" }]}>Continue with Google</Text></Pressable>}
          <Text style={[styles.small, { textAlign: "center" }]}>Your pet’s records stay connected to your account.</Text>
          </>}
          </View>
          <Text style={[styles.small, { textAlign: "center" }]}>Every Pet. Health. Always.</Text>
          {__DEV__ && (
            <Text style={styles.small}>
              Development API: {apiBaseUrl || "Set EXPO_PUBLIC_API_BASE_URL"}
            </Text>
          )}
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const auth = StyleSheet.create({
  screen: { backgroundColor: "#F7F5EE" },
  content: { flexGrow: 1, paddingTop: 25, gap: 26, width: "100%", maxWidth: 540, alignSelf: "center" },
  card: { backgroundColor: "white", borderRadius: 28, borderWidth: 1, borderColor: "#10324E14", padding: 23, gap: 20 },
  badge: { color: "#10324E", backgroundColor: "#EFF6FF", borderRadius: 20, paddingVertical: 7, paddingHorizontal: 12, alignSelf: "flex-start", fontFamily: "Manrope-Bold", fontSize: 9, letterSpacing: 1 },
  title: { color: "#10324E", fontSize: 25, lineHeight: 33 },
  tabs: { flexDirection: "row", backgroundColor: "#F5F2EB", padding: 4, borderRadius: 15 },
  tab: { flex: 1, paddingVertical: 11, borderRadius: 11, alignItems: "center" },
  activeTab: { backgroundColor: "white" },
  tabText: { fontFamily: "Manrope-Bold", fontSize: 12, color: "#778089" },
  submit: { backgroundColor: "#10324E", padding: 15, borderRadius: 12, alignItems: "center" },
  submitText: { fontFamily: "Manrope-Bold", fontSize: 13, color: "white" },
});
