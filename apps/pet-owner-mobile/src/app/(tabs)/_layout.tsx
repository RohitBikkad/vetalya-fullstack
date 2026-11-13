import React from "react";
import { Tabs, Redirect } from "expo-router";
import { ActivityIndicator, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useOwner } from "../../OwnerContext";
import { DesignIcon } from "../../DesignIcon";
import { color } from "../../ui";
export default function Layout() {
  const { user, starting } = useOwner();
  const insets = useSafeAreaInsets();
  if (starting)
    return (
      <View style={{ flex: 1, justifyContent: "center" }}>
        <ActivityIndicator color={color.teal} />
      </View>
    );
  if (!user) return <Redirect href="/sign-in" />;
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: "#0B6B4F",
        tabBarInactiveTintColor: "#64766D",
        tabBarStyle: {
          height: 60 + Math.max(24, insets.bottom),
          paddingTop: 10,
          paddingBottom: Math.max(24, insets.bottom),
          borderTopWidth: 0,
          borderTopLeftRadius: 28,
          borderTopRightRadius: 28,
          backgroundColor: "white",
          boxShadow: "0px -6px 24px #0D33261A",
        },
        tabBarLabelStyle: {
          fontFamily: "Manrope-SemiBold",
          fontSize: 11,
          marginTop: 4,
        },
        tabBarIconStyle: { height: 24, width: 24 },
      }}
    >
      {(
        [
          ["index", "Home", "home"],
          ["pets", "My Pets", "paw"],
          ["visits", "Appointments", "calendar"],
          ["passport", "Passport", "passport"],
          ["profile", "Profile", "profile"],
        ] as const
      ).map(([name, title, icon]) => (
        <Tabs.Screen
          key={name}
          name={name}
          options={{ title, tabBarIcon: () => <DesignIcon name={icon} /> }}
        />
      ))}
      <Tabs.Screen name="records" options={{ href: null }} />
      <Tabs.Screen name="care" options={{ href: null }} />
    </Tabs>
  );
}
