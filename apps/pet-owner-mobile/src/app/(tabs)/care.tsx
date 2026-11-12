import React from "react";
import { Text, View } from "react-native";
import { router } from "expo-router";
import { useOwner } from "../../OwnerContext";
import { Screen, Card, Button, Empty, styles, Icon } from "../../ui";
import { dateLabel } from "../../../../../packages/owner-domain/src/model.js";
export default function Care() {
  const { pet, due, details } = useOwner();
  const items = due.filter(
    (v) => v.petId === pet?.petId || v.petName === pet?.name,
  );
  return (
    <Screen
      title="Good care, right on time."
      subtitle="Preventive care from your veterinary records."
      details
    >
      <Card>
        <View style={styles.row}>
          <Icon name="activity" />
          <Text style={styles.heading}>Vaccinations due within 60 days</Text>
        </View>
        {items.map((v: any) => (
          <View key={v.vaccinationId} style={{ gap: 8, paddingVertical: 12 }}>
            <Text style={styles.heading}>{v.vaccineName}</Text>
            <Text style={styles.body}>{dateLabel(v.nextVaccinationDate)}</Text>
            <Button
              title="Book a visit"
              secondary
              onPress={() => router.push("/book")}
            />
          </View>
        ))}
        {!items.length && (
          <Empty
            title="No due vaccinations returned"
            description="Your clinician’s recorded due dates will appear here."
          />
        )}
      </Card>
      <Card>
        <Text style={styles.heading}>Vaccination history</Text>
        {(details.vaccines || []).map((v: any) => (
          <View key={v.vaccinationId} style={{ gap: 6, paddingVertical: 12 }}>
            <Text style={styles.heading}>{v.vaccineName}</Text>
            <Text style={styles.body}>
              Given {dateLabel(v.lastVaccinationDate)}
            </Text>
            <Text style={styles.small}>
              Next due {dateLabel(v.nextVaccinationDate)} · {v.vetName}
            </Text>
          </View>
        ))}
        {!(details.vaccines || []).length && (
          <Empty
            title="No records yet"
            description="Vaccinations recorded by your veterinary team will appear here."
          />
        )}
        <Text style={styles.small}>
          Medication reminders and push notifications await approved schedules
          and notification integration.
        </Text>
      </Card>
    </Screen>
  );
}
