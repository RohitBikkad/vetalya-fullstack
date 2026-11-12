import React, { useState } from "react";
import { Text, View } from "react-native";
import { router } from "expo-router";
import { useOwner } from "../../OwnerContext";
import { Screen, Card, Button, ErrorNotice, styles, Icon } from "../../ui";
export default function Profile() {
  const { profile, user, signOut } = useOwner();
  const [error, setError] = useState("");
  return (
    <Screen
      title="A little about you."
      subtitle="Keep your caregiver details close and up to date."
    >
      {error && <ErrorNotice message={error} />}
      <Card>
        <Icon name="user" size={29} />
        <Text style={styles.heading}>
          {profile?.firstName
            ? `${profile.firstName} ${profile.lastName}`
            : user?.fullName || user?.name}
        </Text>
        <Text style={styles.body}>{profile?.email || user?.email}</Text>
        <View style={styles.badge}>
          <Text style={styles.badgeText}>Pet parent</Text>
        </View>
        {[
          ["Phone", profile?.phone],
          ["Address", profile?.address1],
          ["City", profile?.city],
          ["Pincode", profile?.pincode],
        ].map(([k, v]) => (
          <View key={k} style={styles.between}>
            <Text style={styles.body}>{k}</Text>
            <Text style={styles.small}>{v || "Not provided"}</Text>
          </View>
        ))}
        <Button
          title="Edit my profile"
          disabled={!profile}
          onPress={() => router.push("/profile-form")}
        />
      </Card>
      <Card>
        <Text style={styles.heading}>Your care space is personal.</Text>
        <Text style={styles.body}>
          Owner-only records and sharing are controlled by the same backend as
          the web portal. Signed veterinary notes cannot be edited here.
        </Text>
        <Text style={styles.small}>
          Notification preferences and delivery are awaiting integration.
        </Text>
        <Button
          title="Sign out"
          secondary
          onPress={() => signOut().catch((e) => setError(e.message))}
        />
      </Card>
    </Screen>
  );
}
