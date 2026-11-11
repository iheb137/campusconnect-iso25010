import { ADMIN, expect, login, MODERATOR, newUser, open, publish, register, test, uid } from "./helpers";

test.describe("Modération, administration et audit", () => {
  test("FUN-MOD-01 (RG-06) : signalement → masquage par le modérateur → trace dans l'audit", async ({ page, browser }) => {
    const tag = uid();
    await register(page, "Karim Mansour");
    const url = await publish(page, { title: `Publication douteuse ${tag}` });

    const reporter = await newUser(browser);
    await register(reporter, "Lina Ferchichi");
    await open(reporter, url);
    await reporter.getByRole("button", { name: "Signaler cette publication" }).click();
    await reporter.getByLabel("Motif").fill(`Hors sujet ${tag}`);
    await reporter.getByRole("button", { name: "Envoyer le signalement" }).click();
    await expect(reporter.getByText("a été signalée")).toBeVisible();

    const moderator = await newUser(browser);
    await login(moderator, MODERATOR.email, MODERATOR.password, "/admin");
    const item = moderator.locator("li, article").filter({ hasText: `Hors sujet ${tag}` }).first();
    await expect(item).toBeVisible();
    await item.getByRole("button", { name: "Masquer la publication" }).click();
    await expect(moderator.getByText(`Hors sujet ${tag}`)).toHaveCount(0);

    await open(reporter, url);
    await expect(reporter.getByText("Demande introuvable")).toBeVisible();
    await open(page, "/dashboard");
    await expect(page.getByText("Masquée par la modération").first()).toBeVisible();

    const admin = await newUser(browser);
    await login(admin, ADMIN.email, ADMIN.password, "/admin/stats");
    await open(admin, "/admin/audit");
    await admin.getByLabel("Action").selectOption("REQUEST_HIDDEN");
    await expect(admin.locator("tr", { hasText: `Publication douteuse ${tag}` })).toBeVisible();
  });

  test("FUN-ADM-01 (RG-09) : gestion des matières", async ({ page }) => {
    const name = `Cyber ${uid()}`;
    await login(page, ADMIN.email, ADMIN.password, "/admin/stats");
    await open(page, "/admin/subjects");
    await page.getByLabel("Nouvelle matière").fill(name);
    await page.getByRole("button", { name: "Ajouter" }).click();
    await expect(page.getByText(`Matière « ${name} » ajoutée.`)).toBeVisible();
    await page.getByLabel("Nouvelle matière").fill(name.toUpperCase());
    await page.getByRole("button", { name: "Ajouter" }).click();
    await expect(page.getByText("Une matière porte déjà ce nom")).toBeVisible();
    await page.getByRole("button", { name: `Renommer ${name}` }).click();
    await page.getByLabel(`Nouveau nom de ${name}`).fill(`${name} bis`);
    await page.getByRole("button", { name: "Enregistrer" }).click();
    await expect(page.getByText("Matière renommée.")).toBeVisible();
    await page.getByRole("button", { name: `Supprimer ${name} bis` }).click();
    await expect(page.getByText(`Matière « ${name} bis » supprimée.`)).toBeVisible();
    await page.getByRole("button", { name: "Supprimer Algorithmique" }).click();
    await expect(page.getByText("utilisée par des demandes")).toBeVisible();
  });

  test("FUN-ADM-02 (RG-10) : désactivation d'un compte puis réactivation", async ({ page, browser }) => {
    const student = await newUser(browser);
    const email = await register(student, "Mehdi Bouaziz");

    await login(page, ADMIN.email, ADMIN.password, "/admin/stats");
    await open(page, "/admin/users");
    await page.getByLabel("Nom ou email").fill(email);
    await expect(page.getByText("1 compte(s)")).toBeVisible();
    await page.getByRole("switch", { name: "Désactiver le compte de Mehdi Bouaziz" }).click();
    await expect(page.getByText("Compte de Mehdi Bouaziz désactivé.")).toBeVisible();

    // Le jeton déjà émis cesse de fonctionner immédiatement
    await open(student, "/dashboard");
    await expect(student).toHaveURL(/\/login/);

    await page.getByRole("switch", { name: "Activer le compte de Mehdi Bouaziz" }).click();
    await expect(page.getByText("Compte de Mehdi Bouaziz activé.")).toBeVisible();
  });

  test("FUN-ADM-03 : tableau de bord statistique filtrable @mobile", async ({ page }) => {
    await login(page, ADMIN.email, ADMIN.password, "/admin/stats");
    await expect(page.getByText("Demandes publiées par jour")).toBeVisible();
    await page.getByLabel("Matière", { exact: true }).selectOption({ label: "Algorithmique" });
    await expect(page.getByText("Demandes publiées par jour")).toBeVisible();
    await expect(page.getByText("Période du")).toBeVisible();
  });
});
