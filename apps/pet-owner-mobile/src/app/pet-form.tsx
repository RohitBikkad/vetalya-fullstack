import React, { useState } from "react";
import { Text, View, Pressable } from "react-native";
import { router, useLocalSearchParams, Redirect } from "expo-router";
import { useOwner, type Data } from "../OwnerContext";
import { Screen, Card, Input, Button, ErrorNotice, styles, color } from "../ui";
import { ownerApi } from "../api";
import {
  SPECIES,
  petPayload,
} from "../../../../packages/owner-domain/src/model.js";
export default function PetForm() {
  const { id } = useLocalSearchParams<{ id?: string }>();
  const { user, pets, reload } = useOwner();
  const existing = pets.find((p) => p.petId === id);
  const [form, setForm] = useState<Data>(
      existing
        ? { ...existing }
        : { name: "", species: "DOG", gender: "UNKNOWN", breed: "", dob: "" },
    ),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  if (!user) return <Redirect href="/sign-in" />;
  const field = (k: string, v: string) => setForm((f) => ({ ...f, [k]: v }));
  async function save() {
    setBusy(true);
    setError("");
    try {
      await ownerApi.savePet(petPayload(form), id);
      await reload();
      router.back();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to save pet.");
    } finally {
      setBusy(false);
    }
  }
  return (
    <Screen
      title={id ? "A familiar little face." : "Meet your new companion."}
      subtitle="The details that make them, them."
    >
      {error && <ErrorNotice message={error} />}
      <Card>
        <Input
          label="Pet’s name"
          value={form.name}
          maxLength={100}
          onChangeText={(v) => field("name", v)}
        />
        <Text style={styles.label}>Species</Text>
        <View style={[styles.row, { flexWrap: "wrap" }]}>
          {Object.entries(SPECIES).map(([k, v]) => (
            <Pressable
              key={k}
              style={[styles.pill, form.species === k && styles.pillActive]}
              onPress={() => field("species", k)}
            >
              <Text
                style={{ color: form.species === k ? color.teal : color.muted }}
              >
                {v}
              </Text>
            </Pressable>
          ))}
        </View>
        <Text style={styles.label}>Sex</Text>
        <View style={styles.row}>
          {[
            ["UNKNOWN", "Not recorded"],
            ["MALE", "Male"],
            ["FEMALE", "Female"],
          ].map(([k, v]) => (
            <Pressable
              key={k}
              style={[styles.pill, form.gender === k && styles.pillActive]}
              onPress={() => field("gender", k)}
            >
              <Text style={styles.small}>{v}</Text>
            </Pressable>
          ))}
        </View>
        <Input
          label="Breed (optional)"
          value={form.breed || ""}
          maxLength={100}
          onChangeText={(v) => field("breed", v)}
        />
        <Input
          label="Date of birth (YYYY-MM-DD, optional)"
          value={form.dob || ""}
          placeholder="2023-04-15"
          onChangeText={(v) => field("dob", v)}
          maxLength={10}
        />
        <Button
          title={id ? "Save pet profile" : "Add my pet"}
          busy={busy}
          onPress={save}
        />
      </Card>
    </Screen>
  );
}
