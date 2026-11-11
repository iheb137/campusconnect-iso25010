import { defineConfig, devices } from "@playwright/test";

/**
 * Tests de bout en bout (ISO 25010 : adéquation fonctionnelle, compatibilité, utilisabilité).
 * L'application doit tourner avant le lancement (start.bat ou docker compose).
 *   E2E_BASE_URL  adresse du frontend (défaut : installation locale QA, port 3010)
 * Rapports : playwright-report/index.html (HTML) et test-results/junit.xml (JUnit).
 */
const baseURL = process.env.E2E_BASE_URL ?? "http://localhost:3010";

export default defineConfig({
  testDir: "./e2e",
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  // Un seul worker : WebKit sous Windows se fige quand plusieurs fenêtres tournent en parallèle.
  workers: 1,
  // Une nouvelle tentative : un test qui ne passe qu'au 2e essai est signalé « flaky » dans le rapport.
  retries: 1,
  reporter: [
    ["list"],
    ["html", { outputFolder: "playwright-report", open: "never" }],
    ["junit", { outputFile: "test-results/junit.xml" }],
    ["json", { outputFile: "test-results/results.json" }],
  ],
  use: {
    baseURL,
    locale: "fr-FR",
    // Animations réduites : les contrôles de contraste ne doivent pas tomber pendant un fondu d'apparition.
    contextOptions: { reducedMotion: "reduce" },
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },
    { name: "firefox", use: { ...devices["Desktop Firefox"] } },
    // WebKit (moteur de Safari) est nettement plus lent sous Windows : délai par test allongé.
    { name: "webkit", use: { ...devices["Desktop Safari"] }, timeout: 180_000 },
    // Compatibilité mobile : seulement les scénarios marqués @mobile
    { name: "mobile-chrome", use: { ...devices["Pixel 7"] }, grep: /@mobile/ },
  ],
});
