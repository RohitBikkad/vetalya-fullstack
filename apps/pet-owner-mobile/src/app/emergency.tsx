import React, { useEffect, useState } from "react";
import { Linking, Text, View } from "react-native";
import { Screen, Card, Button, ErrorNotice, Empty, styles } from "../ui";
import { ownerApi } from "../api";
import { type Data } from "../OwnerContext";
export default function Emergency() {
  const [hospitals, setHospitals] = useState<Data[]>([]),
    [error, setError] = useState(""),
    [loading, setLoading] = useState(true);
  useEffect(() => {
    let active = true;
    ownerApi
      .vets()
      .then((v: Data[]) => {
        if (active)
          setHospitals([
            ...new Map(
              v.flatMap((v) => v.hospitals || []).map((h) => [h.hospitalId, h]),
            ).values(),
          ] as Data[]);
      })
      .catch((e: Error) => {
        if (active) setError(e.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);
  return (
    <Screen
      title="Find veterinary care"
      subtitle="Contact a listed clinic directly. Opening hours and emergency availability must be confirmed with the clinic."
    >
      {error && <ErrorNotice message={error} />}{" "}
      {loading && <Text style={styles.body}>Loading clinics…</Text>}
      {!loading && !error && !hospitals.length && (
        <Empty
          title="No clinics returned"
          description="Your clinic directory is currently empty. Contact your usual veterinary team."
        />
      )}
      {hospitals.map((h) => (
        <Card key={h.hospitalId}>
          <Text style={styles.heading}>{h.name}</Text>
          <Text style={styles.body}>
            {[h.address, h.city, h.pincode].filter(Boolean).join(", ") ||
              "Address not recorded"}
          </Text>
          {h.phone && /^\+?[\d\s()-]{7,20}$/.test(h.phone) ? (
            <Button
              title={`Contact ${h.phone}`}
              onPress={() =>
                Linking.openURL(`tel:${h.phone.replace(/[^+\d]/g, "")}`).catch(
                  () => setError("This device cannot open the phone dialer."),
                )
              }
            />
          ) : (
            <View>
              <Text style={styles.body}>Clinic phone not recorded.</Text>
            </View>
          )}
        </Card>
      ))}
    </Screen>
  );
}
