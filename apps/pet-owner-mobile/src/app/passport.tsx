import React, { useState } from "react";
import { Text, View } from "react-native";
import { router, Redirect } from "expo-router";
import { useOwner, type Data } from "../OwnerContext";
import {
  Screen,
  Card,
  Input,
  Button,
  Empty,
  ErrorNotice,
  PetAvatar,
  styles,
  color,
} from "../ui";
import { ownerApi } from "../api";
import {
  dateLabel,
  SPECIES,
} from "../../../../packages/owner-domain/src/model.js";
export default function Passport() {
  const { user, pet, details, reloadDetails } = useOwner();
  const value = details.passport;
  const [editing, setEditing] = useState(false),
    [form, setForm] = useState<Data>({}),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  if (!user) return <Redirect href="/sign-in" />;
  async function save() {
    setBusy(true);
    setError("");
    try {
      const weight =
        form.weightKg === "" || form.weightKg == null
          ? null
          : Number(form.weightKg);
      if (weight !== null && (!Number.isFinite(weight) || weight <= 0))
        throw new Error("Weight must be a positive number.");
      await ownerApi.savePassport(
        pet!.petId,
        Object.fromEntries(
          [
            "microchipNumber",
            "emergencyContactPhone",
            "allergiesSummary",
            "chronicConditionsSummary",
            "dietaryNotes",
          ]
            .map((k) => [k, form[k] || null])
            .concat([["weightKg", weight]]),
        ),
      );
      await reloadDetails();
      setEditing(false);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to save passport.");
    } finally {
      setBusy(false);
    }
  }
  return (
    <Screen
      title={`${pet?.name || "Your companion"}’s passport.`}
      subtitle="Important details that travel with your companion."
      details
    >
      {error && <ErrorNotice message={error} />}{" "}
      {!pet ? (
        <Empty
          title="Add your pet first"
          description="Their passport will then be available here."
        />
      ) : (
        <>
          <Card>
            <View style={styles.between}>
              <View style={{ gap: 9 }}>
                <Text style={styles.eyebrow}>DIGITAL HEALTH PASSPORT</Text>
                <Text style={styles.title}>{pet.name}</Text>
                <Text style={styles.body}>
                  {SPECIES[pet.species]} · {pet.breed || "Breed not recorded"}
                </Text>
              </View>
              <PetAvatar pet={pet} />
            </View>
            <Text
              style={{ fontSize: 11, color: color.teal, fontWeight: "600" }}
            >
              {value?.passportNumber || "Passport number not available"}
            </Text>
            <View style={styles.divider} />
            {[
              ["Birthday", dateLabel(pet.dob)],
              [
                "Weight",
                value?.weightKg ? `${value.weightKg} kg` : "Not recorded",
              ],
              ["Microchip", value?.microchipNumber || "Not recorded"],
              [
                "Emergency contact",
                value?.emergencyContactPhone || "Not recorded",
              ],
            ].map(([k, v]) => (
              <View style={styles.between} key={k}>
                <Text style={styles.body}>{k}</Text>
                <Text style={styles.small}>{v}</Text>
              </View>
            ))}
            <Button
              title="Edit caregiver details"
              secondary
              disabled={!value}
              onPress={() => {
                setForm({ ...value });
                setEditing(!editing);
              }}
            />
            <Button
              title="Manage record sharing →"
              onPress={() => router.push("/share")}
            />
          </Card>
          {editing ? (
            <Card>
              {[
                ["weightKg", "Weight (kg)"],
                ["microchipNumber", "Microchip number"],
                ["emergencyContactPhone", "Emergency contact"],
                ["allergiesSummary", "Known allergies (caregiver notes)"],
                [
                  "chronicConditionsSummary",
                  "Ongoing conditions (caregiver notes)",
                ],
                ["dietaryNotes", "Dietary notes"],
              ].map(([k, label]) => (
                <Input
                  key={k}
                  label={label}
                  value={String(form[k] ?? "")}
                  maxLength={
                    k === "emergencyContactPhone"
                      ? 20
                      : k === "microchipNumber"
                        ? 50
                        : 1000
                  }
                  keyboardType={k === "weightKg" ? "decimal-pad" : "default"}
                  onChangeText={(v) => setForm((f) => ({ ...f, [k]: v }))}
                />
              ))}
              <Button
                title="Save caregiver details"
                busy={busy}
                onPress={save}
              />
            </Card>
          ) : (
            <Card>
              <Text style={styles.heading}>Important health details</Text>
              {[
                ["Allergies", value?.allergiesSummary],
                ["Ongoing conditions", value?.chronicConditionsSummary],
                ["Dietary notes", value?.dietaryNotes],
              ].map(([k, v]) => (
                <View key={k} style={{ gap: 6 }}>
                  <Text style={styles.label}>{k}</Text>
                  <Text style={styles.body}>{v || "Not recorded"}</Text>
                </View>
              ))}
              <Text style={styles.small}>
                These caregiver details are distinct from signed veterinary
                records.
              </Text>
            </Card>
          )}
        </>
      )}
    </Screen>
  );
}
