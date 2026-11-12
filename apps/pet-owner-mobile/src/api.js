import { Platform } from "react-native";
import * as SecureStore from "expo-secure-store";
import { createApiClient } from "../../../packages/api-client/src/client.js";
import { createOwnerRepository } from "../../../packages/owner-domain/src/api.js";
let accessToken;
const tokenStore = {
  getAccessToken: async () => accessToken,
  getRefreshToken: () =>
    Platform.OS === "web"
      ? Promise.resolve(undefined)
      : SecureStore.getItemAsync("vetalya.refreshToken"),
  async saveTokens(tokens) {
    if (Platform.OS !== "web" && tokens.refreshToken)
      await SecureStore.setItemAsync(
        "vetalya.refreshToken",
        tokens.refreshToken,
      );
    accessToken = tokens.accessToken;
  },
  async clear() {
    accessToken = undefined;
    if (Platform.OS !== "web")
      await SecureStore.deleteItemAsync("vetalya.refreshToken");
  },
};
export const apiBaseUrl =
  process.env.EXPO_PUBLIC_API_BASE_URL ||
  (__DEV__
    ? Platform.OS === "android"
      ? "http://10.0.2.2:8080/api"
      : "http://localhost:8080/api"
    : "");
let instance;
export function client() {
  if (!instance)
    instance = createApiClient({
      baseUrl: apiBaseUrl,
      platform: Platform.OS,
      production: !__DEV__,
      tokenStore,
    });
  return instance;
}
export const ownerApi = createOwnerRepository((path, options) =>
  client().request(path, options),
);
