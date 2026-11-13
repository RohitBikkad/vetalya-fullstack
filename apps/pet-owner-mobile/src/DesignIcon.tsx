import React from "react";
import { Image } from "expo-image";
const assets = {
  home: require("../assets/figma-owner/home.svg"),
  paw: require("../assets/figma-owner/paw.svg"),
  calendar: require("../assets/figma-owner/calendar.svg"),
  passport: require("../assets/figma-owner/passport.svg"),
  profile: require("../assets/figma-owner/profile.svg"),
  "pet-paw": require("../assets/figma-owner/pet-paw.svg"),
  "book-vet": require("../assets/figma-owner/book-vet.svg"),
  reminder: require("../assets/figma-owner/reminder.svg"),
  records: require("../assets/figma-owner/records.svg"),
  emergency: require("../assets/figma-owner/emergency.svg"),
};
export type DesignIconName = keyof typeof assets;
export function DesignIcon({ name }: { name: DesignIconName }) {
  const size =
    name === "pet-paw"
      ? 14
      : ["home", "paw", "calendar", "passport", "profile"].includes(name)
        ? 24
        : 20;
  return (
    <Image
      source={assets[name]}
      style={{ width: size, height: size }}
      contentFit="contain"
      accessibilityElementsHidden
      importantForAccessibility="no"
    />
  );
}
