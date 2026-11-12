import React from "react";
import { Redirect } from "expo-router";
import { ActivityIndicator, View } from "react-native";
import { useOwner } from "../OwnerContext";
export default function Index() {
  const { user, starting } = useOwner();
  if (starting)
    return (
      <View style={{ flex: 1, justifyContent: "center" }}>
        <ActivityIndicator color="#079D96" />
      </View>
    );
  return <Redirect href={user ? "/(tabs)" : "/sign-in"} />;
}
