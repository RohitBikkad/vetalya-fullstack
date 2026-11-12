import React from "react";
import { Tabs, Redirect } from "expo-router";
import { ActivityIndicator, View } from "react-native";
import { useOwner } from "../../OwnerContext";
import { Icon, color } from "../../ui";

export default function Layout() {
  const { user, starting } = useOwner();
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
        tabBarActiveTintColor: color.teal,
        tabBarInactiveTintColor: "#9AABB9",
        tabBarStyle: { borderTopColor: color.line, paddingTop: 9 },
        tabBarLabelStyle: { fontSize: 9, fontWeight: "600" },
      }}
    >
      {(
        [
          ["index", "Home", "home"],
          ["pets", "Pets", "heart"],
          ["visits", "Visits", "calendar"],
          ["records", "Records", "file-text"],
          ["care", "Care", "activity"],
          ["profile", "Profile", "user"],
        ] as const
      ).map(([name, title, icon]) => (
        <Tabs.Screen
          key={name}
          name={name}
          options={{
            title,
            tabBarIcon: ({ color: tint }) => (
              <Icon name={icon} color={typeof tint === "string" ? tint : color.teal} size={20} />
            ),
          }}
        />
      ))}
    </Tabs>
  );
}
