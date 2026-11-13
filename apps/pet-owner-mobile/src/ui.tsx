import React from "react";
import {
  ActivityIndicator,
  Image,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
  type TextInputProps,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import Feather from "@expo/vector-icons/Feather";
import { useOwner, type Data } from "./OwnerContext";
import { safeExternalUrl } from "../../../packages/owner-domain/src/model.js";
export const color = {
  navy: "#0B6B4F",
  teal: "#0B6B4F",
  orange: "#FF843F",
  ink: "#12221B",
  muted: "#64766D",
  line: "#E4ECE7",
  canvas: "#F4F7F5",
};
export const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: color.canvas },
  content: { padding: 22, paddingBottom: 35, gap: 18 },
  row: { flexDirection: "row", alignItems: "center", gap: 12 },
  between: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 12,
  },
  card: {
    backgroundColor: "white",
    padding: 20,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: color.line,
    gap: 14,
  },
  title: {
    fontFamily: "Manrope-Bold",
    fontSize: 28,
    fontWeight: "700",
    letterSpacing: -0.8,
    color: color.ink,
    lineHeight: 36,
  },
  heading: {
    fontFamily: "Manrope-Bold",
    fontSize: 17,
    fontWeight: "700",
    color: color.ink,
  },
  body: {
    fontFamily: "Manrope-Regular",
    fontSize: 13,
    color: color.muted,
    lineHeight: 21,
  },
  small: {
    fontFamily: "Manrope-Regular",
    fontSize: 11,
    color: color.muted,
    lineHeight: 18,
  },
  eyebrow: {
    fontFamily: "Manrope-SemiBold",
    fontSize: 9,
    fontWeight: "700",
    letterSpacing: 1.7,
    color: color.teal,
  },
  avatar: {
    width: 56,
    height: 56,
    borderRadius: 17,
    backgroundColor: "#E6F3EF",
    alignItems: "center",
    justifyContent: "center",
    overflow: "hidden",
  },
  badge: {
    alignSelf: "flex-start",
    backgroundColor: "#E9F6F1",
    paddingHorizontal: 9,
    paddingVertical: 5,
    borderRadius: 6,
  },
  badgeText: {
    fontFamily: "Manrope-SemiBold",
    fontSize: 10,
    fontWeight: "600",
    color: color.teal,
  },
  field: { gap: 8 },
  label: {
    fontFamily: "Manrope-Medium",
    fontSize: 12,
    fontWeight: "600",
    color: "#61788D",
  },
  input: {
    fontFamily: "Manrope-Regular",
    backgroundColor: "white",
    borderColor: color.line,
    borderWidth: 1,
    borderRadius: 11,
    padding: 14,
    fontSize: 14,
    color: color.ink,
    minHeight: 49,
  },
  button: {
    backgroundColor: color.navy,
    borderRadius: 11,
    padding: 15,
    alignItems: "center",
    justifyContent: "center",
    flexDirection: "row",
    gap: 9,
    minHeight: 48,
  },
  buttonText: {
    fontFamily: "Manrope-Bold",
    color: "white",
    fontSize: 13,
    fontWeight: "700",
  },
  secondary: {
    backgroundColor: "white",
    borderColor: color.line,
    borderWidth: 1,
  },
  pill: {
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: color.line,
    backgroundColor: "white",
  },
  pillActive: { borderColor: color.teal, backgroundColor: "#E9F6F3" },
  error: {
    backgroundColor: "#FFF1EC",
    padding: 14,
    borderRadius: 11,
    borderColor: "#F0D7CD",
    borderWidth: 1,
    gap: 7,
  },
  errorText: {
    fontFamily: "Manrope-Regular",
    color: "#A25742",
    fontSize: 12,
    lineHeight: 19,
  },
  divider: { height: 1, backgroundColor: color.line },
  hero: {
    backgroundColor: color.navy,
    borderRadius: 20,
    padding: 25,
    gap: 17,
    overflow: "hidden",
  },
  heroTitle: {
    fontFamily: "Manrope-Bold",
    fontSize: 34,
    fontWeight: "700",
    letterSpacing: -1,
    color: "white",
    lineHeight: 43,
  },
  stat: {
    flex: 1,
    backgroundColor: "white",
    borderRadius: 13,
    padding: 15,
    borderWidth: 1,
    borderColor: color.line,
    gap: 10,
  },
  statNumber: {
    fontFamily: "Manrope-Bold",
    fontSize: 27,
    fontWeight: "700",
    color: color.navy,
  },
  empty: { paddingVertical: 30, alignItems: "center", gap: 12 },
  logo: { width: 43, height: 43 },
  link: {
    fontFamily: "Manrope-SemiBold",
    fontSize: 12,
    color: color.teal,
    fontWeight: "600",
  },
});
export function Icon({
  name,
  size = 20,
  color: iconColor = color.teal,
}: {
  name: React.ComponentProps<typeof Feather>["name"];
  size?: number;
  color?: React.ComponentProps<typeof Feather>["color"];
}) {
  return <Feather name={name} size={size} color={iconColor} />;
}
export function Button({
  title,
  onPress,
  secondary = false,
  busy = false,
  disabled = false,
}: {
  title: string;
  onPress: () => void;
  secondary?: boolean;
  busy?: boolean;
  disabled?: boolean;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      disabled={busy || disabled}
      style={[
        styles.button,
        secondary && styles.secondary,
        (busy || disabled) && { opacity: 0.45 },
      ]}
    >
      {busy ? (
        <ActivityIndicator color={secondary ? color.teal : "white"} />
      ) : (
        <Text style={[styles.buttonText, secondary && { color: color.navy }]}>
          {title}
        </Text>
      )}
    </Pressable>
  );
}
export function Input({ label, ...props }: TextInputProps & { label: string }) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        accessibilityLabel={label}
        placeholderTextColor="#A8B4C0"
        style={[
          styles.input,
          props.multiline && { minHeight: 92, textAlignVertical: "top" },
        ]}
        {...props}
      />
    </View>
  );
}
export function Card({ children }: { children: React.ReactNode }) {
  return <View style={styles.card}>{children}</View>;
}
export function Empty({
  title,
  description,
}: {
  title: string;
  description: string;
}) {
  return (
    <View style={styles.empty}>
      <Icon name="heart" size={29} color="#9EBDB9" />
      <Text style={styles.heading}>{title}</Text>
      <Text style={[styles.body, { textAlign: "center" }]}>{description}</Text>
    </View>
  );
}
export function ErrorNotice({ message }: { message: string }) {
  return (
    <View accessibilityRole="alert" style={styles.error}>
      <Text style={styles.errorText}>{message}</Text>
    </View>
  );
}
export function PetAvatar({ pet }: { pet?: Data }) {
  const url = safeExternalUrl(pet?.photoUrl);
  return (
    <View style={styles.avatar}>
      {url ? (
        <Image
          source={{ uri: url }}
          style={{ width: "100%", height: "100%" }}
          accessibilityLabel={pet?.name}
        />
      ) : (
        <Icon name="heart" size={25} />
      )}
    </View>
  );
}
export function PetSwitcher() {
  const { pets, pet, selectPet } = useOwner();
  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={{ gap: 9 }}
    >
      {pets.map((p) => (
        <Pressable
          key={p.petId}
          accessibilityRole="button"
          accessibilityState={{ selected: pet?.petId === p.petId }}
          onPress={() => selectPet(p.petId)}
          style={[styles.pill, pet?.petId === p.petId && styles.pillActive]}
        >
          <Text
            style={{
              color: pet?.petId === p.petId ? color.teal : color.muted,
              fontWeight: "600",
            }}
          >
            {p.name}
          </Text>
        </Pressable>
      ))}
    </ScrollView>
  );
}
export function Screen({
  title,
  subtitle,
  children,
  details = false,
}: {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
  details?: boolean;
}) {
  const state = useOwner();
  return (
    <SafeAreaView style={styles.screen} edges={["top", "left", "right"]}>
      <ScrollView
        keyboardShouldPersistTaps="handled"
        contentContainerStyle={styles.content}
        refreshControl={
          <RefreshControl
            refreshing={state.loading}
            onRefresh={() => {
              state.reload();
              state.reloadDetails();
            }}
            tintColor={color.teal}
          />
        }
      >
        <View style={styles.between}>
          <View style={styles.row}>
            <Image
              source={require("../assets/vetalya-logo.png")}
              style={styles.logo}
            />
            <Image
              source={require("../assets/vetalya-wordmark.png")}
              style={{ width: 120, height: 48, resizeMode: "contain" }}
              accessibilityLabel="Vetalya · Every Pet. Health. Always."
            />
          </View>
          <Icon name="heart" size={21} />
        </View>
        <View style={{ gap: 8, marginTop: 9 }}>
          <Text style={styles.eyebrow}>YOUR COMPANION’S CARE SPACE</Text>
          <Text style={styles.title}>{title}</Text>
          {subtitle && <Text style={styles.body}>{subtitle}</Text>}
        </View>
        {state.errors.map((e) => (
          <ErrorNotice key={e} message={e} />
        ))}
        {details && (
          <>
            <PetSwitcher />
            {state.detailErrors.map((e) => (
              <ErrorNotice key={e} message={e} />
            ))}
            {state.detailLoading && <ActivityIndicator color={color.teal} />}
          </>
        )}
        {state.loading && <ActivityIndicator color={color.teal} />}
        <View style={{ gap: 18 }}>{children}</View>
        <Text style={[styles.small, { textAlign: "center", paddingTop: 15 }]}>
          Vetalya · Every Pet. Health. Always.
        </Text>
      </ScrollView>
    </SafeAreaView>
  );
}
