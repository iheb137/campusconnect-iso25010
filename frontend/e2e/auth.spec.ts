import { ADMIN, expect, login, MODERATOR, newUser, open, PASSWORD, register, test, uniqueEmail } from "./helpers";

test.describe("Authentification et rôles", () => {
  test("FUN-AUTH-01 : inscription puis connexion automatique @mobile", async ({ page }) => {
    await register(page, "Alice Martin");
    await expect(page.getByText("Bienvenue")).toBeVisible();
    await expect(page).toHaveURL(/\/profile\?welcome=1/);
  });

  test("FUN-AUTH-02 : validation du formulaire et identifiants incorrects", async ({ page }) => {
    await open(page, "/login");
    await page.getByRole("button", { name: "Se connecter" }).click();
    await expect(page.getByText("Saisissez un email valide")).toBeVisible();
    await page.getByLabel("Email").fill(uniqueEmail("inconnu"));
    await page.getByLabel("Mot de passe").fill("mauvais-mot-de-passe");
    await page.getByRole("button", { name: "Se connecter" }).click();
    await expect(page.getByText("Email ou mot de passe incorrect")).toBeVisible();
  });

  test("FUN-AUTH-03 : une page protégée renvoie vers la connexion, puis y revient", async ({ page }) => {
    const email = await register(page, "Bob Trabelsi");
    await page.context().clearCookies();
    await open(page, "/dashboard");
    await expect(page).toHaveURL(/\/login/);
    await page.getByLabel("Email").fill(email);
    await page.getByLabel("Mot de passe").fill(PASSWORD);
    await page.getByRole("button", { name: "Se connecter" }).click();
    await expect(page).toHaveURL(/\/dashboard/);
  });

  test("FUN-AUTH-04 : chaque rôle arrive sur son espace", async ({ browser }) => {
    const admin = await newUser(browser);
    await login(admin, ADMIN.email, ADMIN.password, "/admin/stats");
    await expect(admin.getByRole("link", { name: "Comptes" })).toBeVisible();

    const moderator = await newUser(browser);
    await login(moderator, MODERATOR.email, MODERATOR.password, "/admin");
    await expect(moderator.getByRole("heading", { name: "Publications signalées" })).toBeVisible();
    await expect(moderator.getByRole("link", { name: "Comptes" })).toHaveCount(0);
    await open(moderator, "/admin/users");
    await expect(moderator).toHaveURL(/\/admin$/);
  });

  test("SEC-UI-01 : le jeton est dans un cookie httpOnly, inaccessible au JavaScript", async ({ page, browserName }) => {
    await register(page, "Carla Ben Ali");
    const cookies = await page.context().cookies();
    const session = cookies.find((cookie) => cookie.name === "cc_token");
    expect(session, "cookie de session").toBeDefined();
    expect(session!.httpOnly).toBe(true);
    // Limite connue de Playwright : WebKit sous Windows ne restitue pas l'attribut SameSite (toujours « None »).
    if (!(browserName === "webkit" && process.platform === "win32")) {
      expect(session!.sameSite).toBe("Lax");
    }
    expect(await page.evaluate(() => document.cookie)).not.toContain("cc_token");
  });

  test("FUN-AUTH-05 : déconnexion", async ({ page }) => {
    await register(page, "Dora Kefi");
    await page.getByRole("button", { name: /Se déconnecter/ }).click();
    await expect(page).toHaveURL(/\/login/);
    await open(page, "/dashboard");
    await expect(page).toHaveURL(/\/login/);
  });
});
