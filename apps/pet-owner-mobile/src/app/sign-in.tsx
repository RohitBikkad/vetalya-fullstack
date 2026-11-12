import React, { useState } from "react";
import {
  Image,
  Pressable,
  ScrollView,
  Text,
  View,
  KeyboardAvoidingView,
  Platform,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Redirect } from "expo-router";
import { useOwner } from "../OwnerContext";
import { Button, Input, ErrorNotice, styles, color } from "../ui";
import { apiBaseUrl } from "../api";
export default function SignIn() {
  const { user, signIn } = useOwner();
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
  if (user) return <Redirect href="/(tabs)" />;
  function field(key: keyof typeof form, value: string) {
    setForm((v) => ({ ...v, [key]: value }));
  }
  async function submit() {
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
      await signIn(
        { ...form, email: form.email.trim().toLowerCase() },
        register,
      );
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to sign in.");
    } finally {
      setBusy(false);
    }
  }
  return (
    <SafeAreaView style={styles.screen}>
      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === "ios" ? "padding" : undefined}
      >
        <ScrollView
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={[styles.content, { paddingTop: 35, gap: 25 }]}
        >
          <View style={styles.row}>
            <Image
              source={require("../../assets/vetalya-logo.png")}
              style={{ width: 50, height: 50 }}
            />
            <View>
              <Text
                style={{
                  fontSize: 32,
                  color: color.navy,
                  fontWeight: "800",
                  letterSpacing: -1,
                }}
              >
                Vetalya
              </Text>
              <Text style={styles.eyebrow}>EVERY PET. HEALTH. ALWAYS.</Text>
            </View>
          </View>
          <View style={styles.hero}>
            <Text style={[styles.eyebrow, { color: "#A7D9D1" }]}>
              A HEALTHIER LIFE TOGETHER
            </Text>
            <Text style={styles.heroTitle}>
              A little care.{"\n"}A lifetime together.
            </Text>
            <Text style={[styles.body, { color: "#C2D5E3" }]}>
              Your companion’s health story, always close to you.
            </Text>
          </View>
          <View style={{ gap: 9 }}>
            <Text style={styles.title}>
              {register ? "Join your care space." : "Welcome back."}
            </Text>
            <Text style={styles.body}>
              {register
                ? "Create your pet-parent account."
                : "Sign in to your companion’s world."}
            </Text>
          </View>
          {error && <ErrorNotice message={error} />}
          <View style={{ gap: 17 }}>
            {register && (
              <Input
                label="Your full name"
                value={form.fullName}
                onChangeText={(v) => field("fullName", v)}
                autoComplete="name"
              />
            )}
            <Input
              label="Email address"
              value={form.email}
              onChangeText={(v) => field("email", v)}
              autoCapitalize="none"
              keyboardType="email-address"
              autoComplete="email"
            />
            {register && (
              <Input
                label="Mobile phone"
                value={form.phone}
                onChangeText={(v) => field("phone", v)}
                keyboardType="phone-pad"
                autoComplete="tel"
              />
            )}
            <Input
              label="Password"
              value={form.password}
              onChangeText={(v) => field("password", v)}
              secureTextEntry
              autoComplete={register ? "new-password" : "current-password"}
            />
            {register && (
              <Input
                label="Confirm password"
                value={form.confirmation}
                onChangeText={(v) => field("confirmation", v)}
                secureTextEntry
              />
            )}
            <Button
              title={register ? "Create my account" : "Sign in"}
              busy={busy}
              onPress={submit}
            />
            <Pressable
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
          <Text style={styles.small}>
            Google/Apple sign-in and account recovery need provider setup. This
            build uses your existing email/password account.
          </Text>
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
