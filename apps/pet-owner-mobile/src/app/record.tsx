import React from "react";
import { Linking, Text, View } from "react-native";
import { useLocalSearchParams, Redirect } from "expo-router";
import { useOwner } from "../OwnerContext";
import { Screen, Card, Button, Empty, styles } from "../ui";
import {
  dateLabel,
  safeExternalUrl,
} from "../../../../packages/owner-domain/src/model.js";
export default function Record() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { user, details } = useOwner();
  const item = (details.encounters || []).find(
    (r: any) => r.encounterId === id && r.isSigned,
  );
  if (!user) return <Redirect href="/sign-in" />;
  return (
    <Screen
      title="A moment in their care."
      subtitle="Signed veterinary records are read-only."
    >
      {!item ? (
        <Card>
          <Empty
            title="Record not available"
            description="Return to the selected pet’s records to load an authorized signed encounter."
          />
        </Card>
      ) : (
        <>
          <Card>
            <View style={styles.badge}>
              <Text style={styles.badgeText}>Signed veterinary record</Text>
            </View>
            <Text style={styles.heading}>{item.diagnosisPrimary}</Text>
            <Text style={styles.body}>
              {item.vetName} · {item.hospitalName}
            </Text>
            <Text style={styles.small}>Signed {dateLabel(item.signedAt)}</Text>
            {[
              ["Chief complaint", item.chiefComplaint],
              ["Assessment", item.assessmentNotes],
              ["Treatment plan", item.treatmentPlan],
              ["Diet instructions", item.dietInstructions],
              [
                "Follow-up",
                item.followUpDate ? dateLabel(item.followUpDate) : null,
              ],
            ].map(([k, v]) =>
              v ? (
                <View key={k} style={{ gap: 7 }}>
                  <Text style={styles.label}>{k}</Text>
                  <Text style={styles.body}>{v}</Text>
                </View>
              ) : null,
            )}
          </Card>
          <Card>
            <Text style={styles.heading}>Prescription</Text>
            {item.prescription ? (
              (item.prescription.items || []).map((m: any) => (
                <View key={m.itemId} style={{ gap: 6, paddingVertical: 10 }}>
                  <Text style={styles.heading}>{m.medicineName}</Text>
                  <Text style={styles.body}>
                    {m.dosage} · {m.frequency} · {m.duration}
                  </Text>
                  {m.instructions && (
                    <Text style={styles.small}>{m.instructions}</Text>
                  )}
                </View>
              ))
            ) : (
              <Text style={styles.body}>
                No prescription attached to this encounter.
              </Text>
            )}
            {safeExternalUrl(item.prescription?.pdfStorageUrl) && (
              <Button
                title="Open prescription PDF ↗"
                secondary
                onPress={() =>
                  Linking.openURL(
                    safeExternalUrl(item.prescription.pdfStorageUrl)!,
                  )
                }
              />
            )}
          </Card>
        </>
      )}
    </Screen>
  );
}
