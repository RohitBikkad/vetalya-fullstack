import React, { useState } from "react";
import { Alert, Pressable, Text, View } from "react-native";
import { router } from "expo-router";
import { useOwner, type Data } from "../../OwnerContext";
import {
  Screen,
  Card,
  Button,
  Empty,
  ErrorNotice,
  styles,
  color,
  Icon,
} from "../../ui";
import { ownerApi } from "../../api";
import {
  upcomingAppointments,
  dateLabel,
  timeLabel,
  appointmentLabel,
} from "../../../../../packages/owner-domain/src/model.js";
export default function Visits() {
  const { appointments, reload } = useOwner();
  const [filter, setFilter] = useState("upcoming"),
    [error, setError] = useState(""),
    [busy, setBusy] = useState("");
  const upcoming = upcomingAppointments(appointments);
  const visible =
    filter === "upcoming"
      ? upcoming
      : appointments.filter(
          (a) =>
            !upcoming.some((u: any) => u.appointmentId === a.appointmentId),
        );
  async function cancel(item: Data) {
    setError("");
    setBusy(item.appointmentId);
    try {
      await ownerApi.cancel(item.appointmentId, "Cancelled by pet parent");
      await reload();
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "Unable to cancel appointment.",
      );
    } finally {
      setBusy("");
    }
  }
  return (
    <Screen
      title="Make time for their care."
      subtitle="Your visits, from first checkup to follow-up."
    >
      <View style={styles.row}>
        {["upcoming", "history"].map((f) => (
          <Pressable
            key={f}
            style={[styles.pill, filter === f && styles.pillActive]}
            onPress={() => setFilter(f)}
          >
            <Text style={{ color: filter === f ? color.teal : color.muted }}>
              {f === "upcoming" ? "Upcoming" : "Past & cancelled"}
            </Text>
          </Pressable>
        ))}
      </View>
      {error && <ErrorNotice message={error} />}
      <Button
        title="+ Book an appointment"
        onPress={() => router.push("/book")}
      />
      {visible.map((a: Data) => (
        <Card key={a.appointmentId}>
          <View style={styles.between}>
            <Icon name="calendar" />
            <View style={styles.badge}>
              <Text style={styles.badgeText}>{appointmentLabel(a.status)}</Text>
            </View>
          </View>
          <Text style={styles.heading}>
            {a.hospital?.name || "Veterinary appointment"}
          </Text>
          <Text style={styles.body}>
            {a.pet?.name} · {dateLabel(a.appointmentAt)} ·{" "}
            {timeLabel(a.appointmentAt)}
          </Text>
          {a.vet && (
            <Text style={styles.small}>
              Dr. {a.vet.firstName} {a.vet.lastName}
            </Text>
          )}
          {["BOOKED", "CONFIRMED"].includes(a.status) && (
            <Button
              title="Cancel appointment"
              secondary
              busy={busy === a.appointmentId}
              onPress={() =>
                Alert.alert(
                  "Cancel this appointment?",
                  `${a.pet?.name} · ${dateLabel(a.appointmentAt)}. Clinic cancellation rules apply.`,
                  [
                    { text: "Keep visit", style: "cancel" },
                    {
                      text: "Cancel appointment",
                      style: "destructive",
                      onPress: () => cancel(a),
                    },
                  ],
                )
              }
            />
          )}
        </Card>
      ))}
      {!visible.length && (
        <Card>
          <Empty
            title="No visits to show"
            description="Booked appointments and their real status will appear here."
          />
        </Card>
      )}
    </Screen>
  );
}
