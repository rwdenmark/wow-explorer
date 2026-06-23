import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  // Relative asset URLs so the built SPA works at the site root (Render) and under
  // a subpath (e.g. /wow behind the Tailscale Funnel path mapping) from one build.
  base: "./",
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": "http://localhost:8080"
    }
  }
});
