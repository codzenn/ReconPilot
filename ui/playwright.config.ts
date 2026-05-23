import { defineConfig, devices } from "@playwright/test";

const baseURL = process.env.E2E_BASE_URL || "http://localhost:8081";
const crossBrowser = process.env.E2E_CROSS_BROWSER === "1";

export default defineConfig({
  testDir: "./e2e",
  retries: 0,
  use: {
    baseURL,
    launchOptions: {
      args: ["--disable-gpu"]
    },
    trace: "retain-on-failure"
  },
  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },
    ...(crossBrowser ? [{ name: "firefox", use: { ...devices["Desktop Firefox"] } }] : []),
    ...(crossBrowser ? [{ name: "webkit", use: { ...devices["Desktop Safari"] } }] : [])
  ]
});
