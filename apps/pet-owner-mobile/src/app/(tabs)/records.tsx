import React from "react";
import { Linking, Pressable, Text, View } from "react-native";
import { router } from "expo-router";
import { useOwner } from "../../OwnerContext";
import { Screen, Card, Empty, styles, color, Icon } from "../../ui";
import {
  dateLabel,
  safeExternalUrl,
} from "../../../../../packages/owner-domain/src/model.js";
export default function Records() {
  const { pet, details } = useOwner();
  const timeline = details.timeline || [];
  const records = (details.encounters || []).filter((r: any) => r.isSigned);
  return (
    <Screen
      title="Every chapter of their health."
      subtitle="Recorded care, organized around your companion."
      details
    >
      {!pet ? (
        <Card>
          <Empty
            title="Add your companion first"
            description="Their health records will then appear here."
          />
        </Card>
      ) : (
        <>
          <Card>
            <Text style={styles.heading}>Signed clinical records</Text>
            {records.map((r: any) => (
              <Pressable
                key={r.encounterId}
                onPress={() =>
                  router.push({
                    pathname: "/record",
                    params: { id: r.encounterId },
                  })
                }
                style={{
                  gap: 7,
                  paddingVertical: 12,
                  borderBottomWidth: 1,
                  borderBottomColor: color.line,
                }}
              >
                <View style={styles.between}>
                  <Text style={styles.heading}>
                    {r.diagnosisPrimary || "Clinical consultation"}
                  </Text>
                  <Icon name="arrow-up-right" size={17} />
                </View>
                <Text style={styles.small}>
                  {dateLabel(r.signedAt)} ·{" "}
                  {r.vetName || "Authoring veterinarian"}
                </Text>
                {r.prescription && (
                  <View style={styles.badge}>
                    <Text style={styles.badgeText}>Prescription attached</Text>
                  </View>
                )}
              </Pressable>
            ))}
            {!records.length && (
              <Empty
                title="No signed records returned"
                description="Your veterinary team’s signed notes will appear here."
              />
            )}
          </Card>
          <Card>
            <Text style={styles.heading}>Health timeline</Text>
            {timeline.map((t: any) => (
              <View
                key={`${t.eventType}-${t.id}`}
                style={{
                  gap: 7,
                  paddingVertical: 14,
                  borderBottomColor: color.line,
                  borderBottomWidth: 1,
                }}
              >
                <Text style={styles.heading}>{t.title}</Text>
                <Text style={styles.body}>{t.subtitle || t.description}</Text>
                <Text style={styles.small}>
                  {dateLabel(t.eventTimestamp)} ·{" "}
                  {t.verificationStatus?.replaceAll("_", " ").toLowerCase() ||
                    "Status not supplied"}
                </Text>
              </View>
            ))}
            {!timeline.length && (
              <Empty
                title="Their story is just beginning"
                description="Consultations, vaccinations and documents will appear as they are recorded."
              />
            )}
          </Card>
          <Card>
            <Text style={styles.heading}>Document library</Text>
            {(details.documents || []).map((d: any) => (
              <View key={d.documentId} style={{ gap: 7, paddingVertical: 10 }}>
                <Text style={styles.heading}>{d.fileName}</Text>
                <Text style={styles.small}>
                  {d.verificationStatus?.replaceAll("_", " ").toLowerCase()}
                </Text>
                {safeExternalUrl(d.storagePath) && (
                  <Pressable
                    onPress={() =>
                      Linking.openURL(safeExternalUrl(d.storagePath)!)
                    }
                  >
                    <Text style={styles.link}>Open document ↗</Text>
                  </Pressable>
                )}
              </View>
            ))}
            <Text style={styles.body}>
              New file uploads await private document-storage integration.
              Existing document records remain available.
            </Text>
          </Card>
        </>
      )}
    </Screen>
  );
}
