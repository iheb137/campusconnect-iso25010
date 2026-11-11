import AxeBuilder from "@axe-core/playwright";
import type { Page } from "@playwright/test";

import { ADMIN, expect, login, open, publish, register, test, uid } from "./helpers";

/**
 * Utilisabilité / accessibilité : WCAG 2.1 niveaux A et AA vérifiés par axe-core.
 * Le détail de chaque analyse est joint au rapport HTML de Playwright.
 */
async function expectAccessible(page: Page, name: string) {
  // Attendre la fin des animations et transitions : un texte en cours de fondu fausse la mesure de contraste.
  await page.evaluate(() => Promise.all(document.getAnimations().map((animation) => animation.finished.catch(() => undefined))));
  const results = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
    .analyze();
  await test.info().attach(`axe-${name}.json`, {
    body: JSON.stringify(results.violations, null, 2),
    contentType: "application/json",
  });
  const summary = results.violations.map((violation) => `${violation.id} (${violation.impact}) × ${violation.nodes.length}`);
  expect(summary, `violations WCAG sur ${name}`).toEqual([]);
}

test.describe("Accessibilité WCAG 2.1 AA", () => {
  test("USA-A11Y-01 : pages publiques", async ({ page }) => {
    for (const path of ["/login", "/register"]) {
      await open(page, path);
      await expect(page.getByRole("main")).toBeVisible();
      await expectAccessible(page, path);
    }
  });

  test("USA-A11Y-02 : pages étudiant (clair et sombre)", async ({ page }) => {
    await register(page, "Nadia Hamdi");
    const url = await publish(page, { title: `Accessibilité ${uid()}` });
    for (const scheme of ["light", "dark"] as const) {
      await page.emulateMedia({ colorScheme: scheme });
      for (const path of ["/requests", "/requests/new", url, "/dashboard", "/profile"]) {
        await open(page, path);
        await page.waitForLoadState("networkidle");
        await expectAccessible(page, `${scheme} ${path}`);
      }
    }
  });

  test("USA-A11Y-03 : pages d'administration", async ({ page }) => {
    await login(page, ADMIN.email, ADMIN.password, "/admin/stats");
    for (const path of ["/admin/stats", "/admin", "/admin/users", "/admin/subjects", "/admin/audit"]) {
      await open(page, path);
      await page.waitForLoadState("networkidle");
      await expectAccessible(page, path);
    }
  });

  test("USA-KBD-01 : connexion entièrement au clavier", async ({ page, browserName }) => {
    test.skip(browserName === "webkit", "WebKit ne déplace pas le focus sur les liens avec Tab par défaut (réglage macOS)");
    const email = await register(page, "Omar Klai");
    await page.context().clearCookies();
    await open(page, "/login");
    await page.getByLabel("Email").focus();
    await page.keyboard.type(email);
    await page.keyboard.press("Tab");
    await page.keyboard.type("MotDePasse123");
    await page.keyboard.press("Enter");
    await expect(page).toHaveURL(/\/dashboard/);
  });
});
