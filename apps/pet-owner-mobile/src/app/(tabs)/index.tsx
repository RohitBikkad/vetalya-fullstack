import React, { useState } from "react";
import {
  ActivityIndicator,
  Alert,
  Image,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { router } from "expo-router";
import { useOwner } from "../../OwnerContext";
import { DesignIcon, type DesignIconName } from "../../DesignIcon";
import { color, ErrorNotice, Icon } from "../../ui";
import {
  appointmentLabel,
  safeExternalUrl,
  timeLabel,
  upcomingAppointments,
} from "../../../../../packages/owner-domain/src/model.js";

export default function Home() {
  const state = useOwner();
  const {
    pet,
    pets,
    profile,
    user,
    appointments,
    details,
    detailErrors,
    detailLoading,
    loading,
    errors,
    reload,
    reloadDetails,
    selectPet,
  } = state;
  const [failedPhoto, setFailedPhoto] = useState("");
  const next = upcomingAppointments(appointments).find(
    (a: any) => a.pet?.petId === pet?.petId,
  );
  const hour = Number(
    new Intl.DateTimeFormat("en-GB", {
      hour: "numeric",
      hourCycle: "h23",
      timeZone: "Asia/Kolkata",
    }).format(new Date()),
  );
  const greeting =
    hour < 12
      ? "Good morning!"
      : hour < 17
        ? "Good afternoon!"
        : "Good evening!";
  const name =
    [profile?.firstName, profile?.lastName].filter(Boolean).join(" ") ||
    user?.name ||
    "Pet parent";
  const initials = name
    .split(" ")
    .map((p: string) => p[0])
    .slice(0, 2)
    .join("");
  const datePart = (part: "day" | "month") =>
    next
      ? new Intl.DateTimeFormat("en-IN", {
          [part]: part === "day" ? "numeric" : "short",
          timeZone: "Asia/Kolkata",
        }).format(new Date(next.appointmentAt))
      : "";
  const tracking = (kind: string) =>
    Alert.alert(
      `${kind} tracking`,
      "This server workflow is not connected yet. No daily totals or health assessment are recorded here.",
    );
  const summaries = [
    [
      "♡",
      "Health",
      detailLoading ? "Loading…" : "View care records",
      "#DDF3E8",
      () => router.push("/(tabs)/care"),
    ],
    ["◷", "Meals", "Not tracked", "#FDEBDD", () => tracking("Meals")],
    ["↗", "Activity", "Not tracked", "#E6F5EE", () => tracking("Activity")],
    [
      "✓",
      "Weight",
      detailLoading
        ? "Loading…"
        : details.passport?.weightKg != null
          ? `${details.passport.weightKg} kg`
          : "Not recorded",
      "#E9EEEB",
      () => router.push("/passport"),
    ],
  ] as const;
  const actions = [
    ["book-vet", "Book vet", "#E6F5EE", "/book"],
    ["reminder", "Reminder", "#FDEBDD", "/(tabs)/care"],
    ["records", "Records", "#E9EEEB", "/(tabs)/records"],
    ["emergency", "Emergency", "#FBE5E2", "/emergency"],
  ] as const;
  return (
    <SafeAreaView style={s.screen} edges={["top", "left", "right"]}>
      <ScrollView
        contentContainerStyle={s.content}
        refreshControl={
          <RefreshControl
            refreshing={loading}
            onRefresh={() => {
              reload();
              reloadDetails();
            }}
            tintColor={color.teal}
          />
        }
      >
        <View style={s.greeting}>
          <View style={{ gap: 3, flex: 1 }}>
            <View style={s.brandRow}>
              <Image
                source={require("../../../assets/vetalya-logo.png")}
                style={s.emblem}
              />
              <Image
                source={require("../../../assets/vetalya-wordmark.png")}
                style={s.wordmark}
                accessibilityLabel="Vetalya · Every Pet. Health. Always."
              />
            </View>
            <Text style={s.title}>{greeting}</Text>
            <Text style={s.body}>
              {pet
                ? `How is ${pet.name} feeling today?`
                : "Let’s begin their health story."}
            </Text>
          </View>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Open my profile"
            style={s.parent}
            onPress={() => router.push("/(tabs)/profile")}
          >
            <Text style={s.captionGreen}>{initials}</Text>
          </Pressable>
        </View>
        {errors.map((e) => (
          <ErrorNotice key={e} message={e} />
        ))}
        {loading && !pets.length ? (
          <ActivityIndicator color={color.teal} />
        ) : pet ? (
          <View style={s.petCard}>
            <View style={s.petDetails}>
              <Text style={s.captionGreen}>MY PET</Text>
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Choose pet"
                onPress={() =>
                  Alert.alert("Choose your pet", undefined, [
                    ...pets.map((p) => ({
                      text: p.name,
                      onPress: () => selectPet(p.petId),
                    })),
                    { text: "Cancel", style: "cancel" },
                  ])
                }
              >
                <Text style={s.title}>
                  {pet.name}
                  {pets.length > 1 ? " ⌄" : ""}
                </Text>
              </Pressable>
              <Text style={s.caption}>
                {pet.breed || pet.species || "Breed not recorded"}
              </Text>
              <Text style={s.caption}>
                {pet.age || "Age not recorded"}
                {pet.gender && pet.gender !== "UNKNOWN"
                  ? ` · ${pet.gender.charAt(0)}${pet.gender.slice(1).toLowerCase()}`
                  : ""}
              </Text>
              <Pressable
                accessibilityRole="button"
                style={s.profileButton}
                onPress={() => router.push("/passport")}
              >
                <Text style={s.profileButtonText}>View profile</Text>
              </Pressable>
            </View>
            <View style={s.portrait}>
              {safeExternalUrl(pet.photoUrl) && failedPhoto !== pet.photoUrl ? (
                <Image
                  source={{ uri: pet.photoUrl }}
                  accessibilityLabel={pet.name}
                  style={s.photo}
                  onError={() => setFailedPhoto(pet.photoUrl)}
                />
              ) : (
                <Icon name="heart" size={64} />
              )}
            </View>
          </View>
        ) : (
          <View style={s.petCard}>
            <View style={{ gap: 10 }}>
              <Text style={s.heading}>Meet your companion</Text>
              <Text style={s.body}>
                Add your first pet to start their health story.
              </Text>
              <Pressable
                style={s.profileButton}
                onPress={() => router.push("/pet-form")}
              >
                <Text style={s.profileButtonText}>Add a pet</Text>
              </Pressable>
            </View>
          </View>
        )}
        <View style={s.section}>
          <Text style={s.heading}>Today’s overview</Text>
          <View style={s.summaryGrid}>
            {summaries.map(([symbol, label, value, bg, action]) => (
              <Pressable
                key={label}
                accessibilityRole="button"
                onPress={action}
                style={s.summary}
              >
                <View style={[s.symbol, { backgroundColor: bg }]}>
                  <Text style={s.symbolText}>{symbol}</Text>
                </View>
                <View style={{ flex: 1, gap: 4 }}>
                  <Text style={s.summaryLabel}>{label}</Text>
                  <Text style={s.caption}>{value}</Text>
                </View>
              </Pressable>
            ))}
          </View>
          {detailErrors.length > 0 && (
            <ErrorNotice message="Some health records couldn’t load. Open Passport or pull to refresh to retry." />
          )}
        </View>
        <View style={s.section}>
          <View style={s.between}>
            <Text style={s.heading}>Upcoming appointment</Text>
            <Pressable
              accessibilityRole="button"
              onPress={() => router.push("/(tabs)/visits")}
            >
              <Text style={s.captionGreen}>See all</Text>
            </Pressable>
          </View>
          {next ? (
            <Pressable
              accessibilityRole="button"
              style={s.appointment}
              onPress={() => router.push("/(tabs)/visits")}
            >
              <View style={s.date}>
                <Text style={s.dateNumber}>{datePart("day")}</Text>
                <Text style={s.captionGreen}>{datePart("month")}</Text>
              </View>
              <View style={{ flex: 1, gap: 2 }}>
                <Text style={s.body}>{timeLabel(next.appointmentAt)}</Text>
                <Text style={s.heading}>
                  {next.reason || "Veterinary consultation"}
                </Text>
                <Text style={s.caption}>
                  {next.hospital?.name || "Hospital not recorded"}
                </Text>
                <View style={[s.between, { flexWrap: "wrap" }]}>
                  <View style={s.petRow}>
                    <View style={s.miniPaw}>
                      <DesignIcon name="pet-paw" />
                    </View>
                    <Text style={s.summaryLabel}>
                      {next.pet?.name || pet?.name}
                    </Text>
                  </View>
                  <Text style={s.status}>{appointmentLabel(next.status)}</Text>
                </View>
              </View>
            </Pressable>
          ) : (
            <View style={s.appointment}>
              <View style={s.date}>
                <DesignIcon name="calendar" />
              </View>
              <View style={{ flex: 1, gap: 5 }}>
                <Text style={s.summaryLabel}>No upcoming appointments</Text>
                <Text style={s.caption}>
                  Plan the next visit when they need care.
                </Text>
                <Pressable
                  accessibilityRole="button"
                  onPress={() => router.push("/book")}
                >
                  <Text
                    style={[s.captionGreen, { fontFamily: "Manrope-SemiBold" }]}
                  >
                    Book a vet visit
                  </Text>
                </Pressable>
              </View>
            </View>
          )}
        </View>
        <View style={s.section}>
          <Text style={s.heading}>Quick actions</Text>
          <View style={s.between}>
            {actions.map(([icon, label, bg, path]) => (
              <Pressable
                accessibilityRole="button"
                key={label}
                onPress={() => router.push(path)}
                style={s.quickAction}
              >
                <View style={[s.actionIcon, { backgroundColor: bg }]}>
                  <DesignIcon name={icon as DesignIconName} />
                </View>
                <Text style={s.summaryLabel}>{label}</Text>
              </Pressable>
            ))}
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
const s = StyleSheet.create({
  screen: { flex: 1, backgroundColor: "#F4F7F5" },
  content: {
    paddingHorizontal: 24,
    paddingTop: 12,
    paddingBottom: 24,
    gap: 14,
    width: "100%",
    maxWidth: 620,
    alignSelf: "center",
  },
  greeting: {
    minHeight: 92,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 10,
  },
  brandRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    marginBottom: 4,
    height: 32,
  },
  emblem: { width: 32, height: 32, resizeMode: "contain" },
  wordmark: { width: 80, height: 32, resizeMode: "contain" },
  brand: {
    fontFamily: "Manrope-Medium",
    fontSize: 12,
    lineHeight: 18,
    color: "#0B6B4F",
  },
  title: {
    fontFamily: "Manrope-Bold",
    fontSize: 22,
    lineHeight: 28,
    color: "#12221B",
    letterSpacing: -0.5,
  },
  body: {
    fontFamily: "Manrope-Regular",
    fontSize: 14,
    lineHeight: 20,
    color: "#64766D",
  },
  caption: {
    fontFamily: "Manrope-Regular",
    fontSize: 12,
    lineHeight: 16,
    color: "#64766D",
  },
  captionGreen: {
    fontFamily: "Manrope-Medium",
    fontSize: 12,
    lineHeight: 16,
    color: "#0B6B4F",
  },
  parent: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: "#E6F5EE",
    justifyContent: "center",
    alignItems: "center",
  },
  petCard: {
    backgroundColor: "#E6F5EE",
    borderRadius: 12,
    padding: 16,
    height: 164,
    flexDirection: "row",
    alignItems: "center",
    overflow: "hidden",
  },
  petDetails: { flex: 1, gap: 6, alignItems: "flex-start" },
  portrait: {
    width: 146,
    height: 146,
    borderRadius: 18,
    overflow: "hidden",
    alignItems: "center",
    justifyContent: "center",
  },
  photo: { width: 146, height: 146, borderRadius: 18, resizeMode: "cover" },
  profileButton: {
    backgroundColor: "#0B6B4F",
    minHeight: 36,
    borderRadius: 18,
    paddingHorizontal: 14,
    paddingVertical: 8,
    boxShadow: "0px 3px 8px #0B6B4F1A",
    alignSelf: "flex-start",
  },
  profileButtonText: {
    fontFamily: "Manrope-Bold",
    fontSize: 14,
    lineHeight: 20,
    color: "white",
  },
  section: { gap: 10 },
  heading: {
    fontFamily: "Manrope-Bold",
    fontSize: 16,
    lineHeight: 20,
    color: "#12221B",
    letterSpacing: -0.3,
  },
  summaryGrid: { flexDirection: "row", flexWrap: "wrap", gap: 10 },
  summary: {
    width: "48%",
    flexGrow: 1,
    flexBasis: "45%",
    minHeight: 62,
    backgroundColor: "white",
    padding: 12,
    borderRadius: 12,
    flexDirection: "row",
    alignItems: "flex-start",
    gap: 10,
  },
  symbol: {
    width: 30,
    height: 30,
    borderRadius: 10,
    justifyContent: "center",
    alignItems: "center",
  },
  symbolText: {
    fontFamily: "Manrope-Bold",
    fontSize: 16,
    lineHeight: 22,
    color: "#0B6B4F",
  },
  summaryLabel: {
    fontFamily: "Manrope-Medium",
    fontSize: 12,
    lineHeight: 16,
    color: "#12221B",
  },
  between: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    gap: 8,
  },
  appointment: {
    backgroundColor: "white",
    padding: 14,
    borderRadius: 20,
    minHeight: 110,
    flexDirection: "row",
    alignItems: "flex-start",
    gap: 12,
    boxShadow: "0px 6px 20px #0D332617",
  },
  date: {
    minWidth: 44,
    minHeight: 58,
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderRadius: 16,
    backgroundColor: "#E6F5EE",
    alignItems: "center",
    justifyContent: "center",
  },
  dateNumber: {
    fontFamily: "Manrope-Bold",
    fontSize: 20,
    lineHeight: 25,
    color: "#0B6B4F",
  },
  petRow: { flexDirection: "row", gap: 6, alignItems: "center" },
  miniPaw: {
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: "#E6F5EE",
    alignItems: "center",
    justifyContent: "center",
  },
  status: {
    fontFamily: "Manrope-Medium",
    fontSize: 12,
    lineHeight: 16,
    color: "#12905F",
    backgroundColor: "#DDF3E8",
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  quickAction: { flex: 1, alignItems: "center", gap: 7 },
  actionIcon: {
    width: 36,
    height: 36,
    borderRadius: 12,
    justifyContent: "center",
    alignItems: "center",
  },
});
