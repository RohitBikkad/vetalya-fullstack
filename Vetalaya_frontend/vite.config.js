import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import { fileURLToPath } from "node:url";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const apiTarget = env.VITE_API_PROXY_TARGET || "http://127.0.0.1:8080";
  return {
    plugins: [react()],
    server: {
      fs: {
        allow: [
          fileURLToPath(new URL(".", import.meta.url)),
          fileURLToPath(new URL("../packages", import.meta.url)),
        ],
      },
      port: 3000,
      host: "127.0.0.1",
      proxy: {
        "/api": {
          target: apiTarget,
          changeOrigin: true,
          secure: false,
        },
        "/oauth2": {
          target: apiTarget,
          changeOrigin: true,
          secure: false,
        },
        "/login/oauth2": {
          target: apiTarget,
          changeOrigin: true,
          secure: false,
        },
      },
    },
  };
});
