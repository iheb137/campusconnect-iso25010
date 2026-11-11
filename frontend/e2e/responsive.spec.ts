import { ADMIN, expect, login, open, register, test } from "./helpers";

test.describe("Compatibilité — affichage mobile", () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test("COMP-RESP-01 : aucune page ne déborde horizontalement à 390 px @mobile", async ({ page, browser }) => {
    await register(page, "Rania Sassi");
    const admin = await (await browser.newContext({ viewport: { width: 390, height: 844 } })).newPage();
    await login(admin, ADMIN.email, ADMIN.password, "/admin/stats");
    const checks: Array<[typeof page, string]> = [
      [page, "/requests"],
      [page, "/requests/new"],
      [page, "/dashboard"],
      [page, "/profile"],
      [admin, "/admin/stats"],
      [admin, "/admin/users"],
      [admin, "/admin/audit"],
    ];
    for (const [p, path] of checks) {
      await open(p, path);
      await p.waitForLoadState("networkidle");
      const overflow = await p.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
      expect(overflow, `débordement sur ${path}`).toBeLessThanOrEqual(0);
    }
  });
});
