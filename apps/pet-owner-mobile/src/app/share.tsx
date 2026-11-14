import React, { useEffect, useState } from "react";
import { Pressable, Text, View } from "react-native";
import { Redirect } from "expo-router";
import { useOwner, type Data } from "../OwnerContext";
import { Screen, Card, Button, ErrorNotice, styles } from "../ui";
import { ownerApi } from "../api";
import { dateLabel, groupPassportSharing, passportSharingSections } from "../../../../packages/owner-domain/src/model.js";
export default function Share() {
  const { user, pet, details, detailLoading, detailErrors, reloadDetails } = useOwner();
  const [hospitals, setHospitals] = useState<Data[]>([]),
    [target, setTarget] = useState(""),
    [scope, setScope] = useState("VACCINES_ONLY"),
    [hours, setHours] = useState(24),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  useEffect(() => {
    let active = true;
    ownerApi
      .vets()
      .then((v: Data[]) => {
        if (active)
          setHospitals([
            ...new Map(
              v
                .flatMap((v: Data) => v.hospitals || [])
                .map((h: Data) => [h.hospitalId, h]),
            ).values(),
          ] as Data[]);
      })
      .catch((e: Error) => {
        if (active) setError(e.message);
      });
    return () => {
      active = false;
    };
  }, []);
  if (!user) return <Redirect href="/sign-in" />;
  const sharingGroups = groupPassportSharing((details.shares || []) as (Data & { status: string })[]);
  async function action(type: string, request?: Data) {
    setBusy(true);
    setError("");
    try {
      if (type === "create") {
        if (!target || !pet)
          throw new Error("Choose a receiving hospital and a pet.");
        await ownerApi.share(pet.petId, {
          targetHospitalId: target,
          scope,
          expiryHours: hours,
        });
      } else if (request) {
        await (type === "approve"
          ? ownerApi.approve(request.requestId)
          : ownerApi.revoke(request.requestId));
      }
      await reloadDetails();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to update sharing.");
    } finally {
      setBusy(false);
    }
  }
  return (
    <Screen
      title="Share with care."
      subtitle={`Scoped hospital access for ${pet?.name || "your companion"}.`}
    >
      {error && <ErrorNotice message={error} />}
      <Card>
        <Text style={styles.body}>
          Grant the selected hospital access to the chosen records for a limited
          time. Submitting grants access immediately. You can revoke it below.
        </Text>
        <Text style={styles.label}>Receiving hospital</Text>
        {hospitals.map((h) => (
          <Pressable
            key={h.hospitalId}
            onPress={() => setTarget(h.hospitalId)}
            style={[styles.pill, target === h.hospitalId && styles.pillActive]}
          >
            <Text style={styles.body}>{h.name}</Text>
          </Pressable>
        ))}
        <Text style={styles.label}>Records to share</Text>
        {[
          ["VACCINES_ONLY", "Vaccinations only"],
          ["EMERGENCY_SUMMARY", "Emergency summary"],
          ["FULL_TIMELINE", "Full timeline"],
        ].map(([k, label]) => (
          <Pressable
            key={k}
            style={[styles.pill, scope === k && styles.pillActive]}
            onPress={() => setScope(k)}
          >
            <Text style={styles.body}>{label}</Text>
          </Pressable>
        ))}
        <View style={styles.row}>
          {[24, 72, 168].map((h) => (
            <Pressable
              key={h}
              style={[styles.pill, hours === h && styles.pillActive]}
              onPress={() => setHours(h)}
            >
              <Text style={styles.small}>
                {h === 24 ? "24 hours" : h === 72 ? "3 days" : "7 days"}
              </Text>
            </Pressable>
          ))}
        </View>
        <Button
          title="Grant hospital access"
          busy={busy}
          onPress={() => action("create")}
        />
      </Card>
      {detailErrors.length > 0 && (
        <ErrorNotice message={detailErrors.join(" · ")} />
      )}
      <Text style={styles.heading}>Hospital access history</Text>
      {detailLoading && <Text style={styles.body}>Loading access history…</Text>}
      {passportSharingSections.map(([key, label]) => sharingGroups[key].length > 0 && (
        <View key={key} style={{ gap: 12 }}>
          <Text style={styles.label}>{label}</Text>
          {sharingGroups[key].map((request) => (
            <Card key={request.requestId}>
              <Text style={styles.body}>
                {hospitals.find((h) => h.hospitalId === request.targetHospitalId)
                  ?.name ||
                  request.targetHospitalId ||
                  "Hospital not specified"}
              </Text>
              <View style={styles.badge}>
                <Text style={styles.badgeText}>{request.status}</Text>
              </View>
              <Text style={styles.body}>
                {request.scope.replaceAll("_", " ").toLowerCase()} · expires{" "}
                {dateLabel(request.expiresAt)}
              </Text>
              {request.targetHospitalId &&
                ["PENDING", "REQUESTED"].includes(request.status) && (
                  <Button
                    title="Approve hospital access"
                    busy={busy}
                    onPress={() => action("approve", request)}
                  />
                )}{" "}
              {request.status !== "REVOKED" && (
                <Button
                  title="Revoke request"
                  secondary
                  busy={busy}
                  onPress={() => action("revoke", request)}
                />
              )}
            </Card>
          ))}
        </View>
      ))}
      {!detailLoading && !detailErrors.length && !(details.shares || []).length && (
        <Text style={styles.body}>No hospital access grants recorded.</Text>
      )}
    </Screen>
  );
}
