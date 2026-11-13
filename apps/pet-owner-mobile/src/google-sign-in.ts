import { Platform } from "react-native";
import { GoogleSignin, isSuccessResponse } from "@react-native-google-signin/google-signin";
const webClientId = process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID;
export const googleSignInAvailable = Platform.OS === "android" && Boolean(webClientId);
export async function googleIdToken(): Promise<string | null> {
  if (!googleSignInAvailable) throw new Error("Google sign-in is not configured for this build.");
  GoogleSignin.configure({ webClientId, offlineAccess: false });
  await GoogleSignin.hasPlayServices({ showPlayServicesUpdateDialog: true });
  const response = await GoogleSignin.signIn();
  if (!isSuccessResponse(response)) return null;
  if (!response.data.idToken) throw new Error("Google did not return a sign-in token. Please try again.");
  return response.data.idToken;
}
