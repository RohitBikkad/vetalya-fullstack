import React from "react";
import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { OwnerProvider } from "../OwnerContext";
export default function Layout() {
  return (
    <SafeAreaProvider>
      <OwnerProvider>
        <StatusBar style="dark" />
        <Stack screenOptions={{ headerShown: false }}>
          <Stack.Screen name="(tabs)" />
          <Stack.Screen name="sign-in" />
          <Stack.Screen name="index" />
          <Stack.Screen
            name="pet-form"
            options={{
              presentation: "modal",
              headerShown: true,
              title: "My companion",
              headerTintColor: "#123F70",
            }}
          />
          <Stack.Screen
            name="book"
            options={{
              presentation: "modal",
              headerShown: true,
              title: "Book a visit",
              headerTintColor: "#123F70",
            }}
          />
          <Stack.Screen
            name="passport"
            options={{
              headerShown: true,
              title: "Health passport",
              headerTintColor: "#123F70",
            }}
          />
          <Stack.Screen
            name="profile-form"
            options={{
              presentation: "modal",
              headerShown: true,
              title: "Caregiver profile",
              headerTintColor: "#123F70",
            }}
          />
          <Stack.Screen
            name="share"
            options={{
              presentation: "modal",
              headerShown: true,
              title: "Share records",
              headerTintColor: "#123F70",
            }}
          />
          <Stack.Screen
            name="record"
            options={{
              headerShown: true,
              title: "Clinical record",
              headerTintColor: "#123F70",
            }}
          />
          <Stack.Screen name="services" options={{headerShown:true,title:"Care services",headerTintColor:"#123F70"}}/>
        </Stack>
      </OwnerProvider>
    </SafeAreaProvider>
  );
}
