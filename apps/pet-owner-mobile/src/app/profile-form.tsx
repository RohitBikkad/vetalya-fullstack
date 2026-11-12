import React, { useState } from "react";
import { router, Redirect } from "expo-router";
import { useOwner, type Data } from "../OwnerContext";
import { Screen, Card, Input, Button, ErrorNotice } from "../ui";
import { ownerApi } from "../api";
import { profilePayload } from "../../../../packages/owner-domain/src/model.js";
export default function ProfileForm() {
  const { user, profile, reload } = useOwner();
  const [form, setForm] = useState<Data>({ ...profile }),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  if (!user) return <Redirect href="/sign-in" />;
  async function save() {
    setBusy(true);
    setError("");
    try {
      await ownerApi.saveProfile(profilePayload(form));
      await reload();
      router.back();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to update profile.");
    } finally {
      setBusy(false);
    }
  }
  return (
    <Screen
      title="Your caregiver profile."
      subtitle="Details that help your care team reach you."
    >
      {error && <ErrorNotice message={error} />}
      <Card>
        {[
          ["firstName", "First name"],
          ["lastName", "Last name"],
          ["address1", "Address line 1"],
          ["address2", "Address line 2"],
          ["city", "City"],
          ["state", "State"],
          ["pincode", "Pincode"],
          ["country", "Country"],
        ].map(([k, v]) => (
          <Input
            key={k}
            label={v}
            value={form[k] || ""}
            maxLength={k === "pincode" ? 6 : 100}
            onChangeText={(value) => setForm((f) => ({ ...f, [k]: value }))}
          />
        ))}
        <Button title="Save my profile" busy={busy} onPress={save} />
      </Card>
    </Screen>
  );
}
