// Configuration starter for a future Expo pet-owner UI, using the existing API.
module.exports = ({ config = {} } = {}) => {
  const variant = process.env.APP_VARIANT || "development";
  if (!["development", "preview", "production"].includes(variant))
    throw new Error("Unknown APP_VARIANT");
  const id = process.env.APP_IDENTIFIER || "com.vetalya.petowner";
  const suffix = variant === "production" ? "" : `.${variant}`;
  const scheme = process.env.APP_SCHEME || "myvetapp";
  if (!/^[a-z][a-z0-9+.-]*$/.test(scheme))
    throw new Error("Invalid APP_SCHEME");
  return {
    ...config,
    name: variant === "production" ? "Vetalya" : `Vetalya (${variant})`,
    slug: "vetalya-pet-owner",
    icon: "./assets/vetalya-logo.png",
    userInterfaceStyle: "light",
    plugins: [
      ...(config.plugins || []),
      "expo-router",
      "expo-secure-store",
      "expo-image",
      "expo-font",
    ],
    version: "0.1.0",
    scheme,
    platforms: ["android", "ios"],
    ios: {
      ...config.ios,
      bundleIdentifier: `${id}${suffix}`,
      supportsTablet: true,
      usesAppleSignIn: true,
    },
    android: { ...config.android, package: `${id}${suffix}` },
    extra: {
      ...config.extra,
      appVariant: variant,
      eas: { ...config.extra?.eas, projectId: "ecb6f326-b9a2-493d-bb68-474705608fc5" },
    },
  };
};
