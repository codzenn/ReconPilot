import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";

export default defineConfig(() => {
  const apiTarget = process.env.VITE_API_PROXY_TARGET || "http://localhost:8080";
  return {
    plugins: [react()],
    base: "/",
    build: {
      outDir: path.resolve(__dirname, "../src/main/resources/static"),
      emptyOutDir: true,
      sourcemap: true
    },
    server: {
      port: 5173,
      proxy: {
        "/api": apiTarget
      }
    },
    test: {
      environment: "jsdom",
      setupFiles: "./src/test/setup.ts",
      include: ["src/**/*.test.{ts,tsx}"],
      exclude: ["node_modules/**", "e2e/**"]
    }
  };
});
