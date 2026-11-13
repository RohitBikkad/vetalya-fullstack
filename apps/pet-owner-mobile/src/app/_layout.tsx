import React from "react";
import { ActivityIndicator, View } from "react-native";
import { useFonts } from "expo-font";
import { Manrope_400Regular } from "@expo-google-fonts/manrope/400Regular";
import { Manrope_500Medium } from "@expo-google-fonts/manrope/500Medium";
import { Manrope_600SemiBold } from "@expo-google-fonts/manrope/600SemiBold";
import { Manrope_700Bold } from "@expo-google-fonts/manrope/700Bold";
import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { OwnerProvider } from "../OwnerContext";
export default function Layout() {
  const [loaded, error] = useFonts({
    "Manrope-Regular": Manrope_400Regular,
    "Manrope-Medium": Manrope_500Medium,
    "Manrope-SemiBold": Manrope_600SemiBold,
    "Manrope-Bold": Manrope_700Bold,
  });
  if (!loaded && !error)
    return (
      <View
        style={{
          flex: 1,
          justifyContent: "center",
          backgroundColor: "#F4F7F5",
        }}
      >
        <ActivityIndicator color="#0B6B4F" />
      </View>
    );
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
          <Stack.Screen
            name="services"
            options={{
              headerShown: true,
              title: "Care services",
              headerTintColor: "#123F70",
            }}
          />
        </Stack>
      </OwnerProvider>
    </SafeAreaProvider>
  );
}
