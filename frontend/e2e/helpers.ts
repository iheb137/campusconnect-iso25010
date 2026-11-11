import { test as base, expect, type Browser, type Page } from "@playwright/test";

/**
 * Les actions destructives demandent une confirmation (window.confirm) : on l'accepte,
 * comme le ferait l'utilisateur. Playwright refuserait sinon la boîte de dialogue.
 */
export const test = base.extend({
  page: async ({ page }, use) => {
    page.on("dialog", (dialog) => dialog.accept().catch(() => undefined));
    await use(page);
  },
});

export { expect };

export const PASSWORD = "MotDePasse123";

export const ADMIN = {
  email: process.env.E2E_ADMIN_EMAIL ?? "admin@campusconnect.local",
  password: process.env.E2E_ADMIN_PASSWORD ?? "Admin12345",
};

export const MODERATOR = {
  email: process.env.E2E_MODERATOR_EMAIL ?? "moderateur@campusconnect.local",
  password: process.env.E2E_MODERATOR_PASSWORD ?? "Moderateur123",
};

/**
 * Navigue puis attend l'hydratation de React (marqueur posé par AuthProvider).
 * Sans cela, un navigateur lent peut saisir dans un formulaire que React réinitialise ensuite.
 * Une navigation interrompue (net::ERR_ABORTED / NS_BINDING_ABORTED) est retentée une fois.
 */
export async function open(page: Page, path: string): Promise<void> {
  // Scénarios à plusieurs utilisateurs : WebKit suspend le rendu des fenêtres en arrière-plan.
  await page.bringToFront();
  try {
    await page.goto(path);
  } catch (error) {
    if (!/ABORTED/i.test(String(error))) throw error;
    await page.goto(path);
  }
  await page.locator("html[data-hydrated='true']").waitFor({ state: "attached" });
}

/** Identifiant court et unique : les tests peuvent tourner plusieurs fois sur la même base. */
export function uid(): string {
  return `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`;
}

export function uniqueEmail(prefix: string): string {
  return `${prefix}.${uid()}@e2e.campusconnect.local`;
}

/** Inscription par l'interface ; l'utilisateur est connecté et arrive sur son profil. */
export async function register(page: Page, fullName: string, email = uniqueEmail("etudiant")): Promise<string> {
  await open(page, "/register");
  await page.getByLabel("Nom complet").fill(fullName);
  await page.getByLabel("Email").fill(email);
  await page.getByRole("textbox", { name: "Mot de passe", exact: true }).fill(PASSWORD);
  await page.getByLabel("Confirmer le mot de passe").fill(PASSWORD);
  await page.getByRole("button", { name: "Créer mon compte" }).click();
  await page.waitForURL("**/profile?welcome=1");
  return email;
}

export async function login(page: Page, email: string, password: string, landing: string): Promise<void> {
  await open(page, "/login");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Mot de passe").fill(password);
  await page.getByRole("button", { name: "Se connecter" }).click();
  await page.waitForURL(`**${landing}`);
}

/** Nouvelle session navigateur isolée (cookies séparés) pour un second utilisateur. */
export async function newUser(browser: Browser): Promise<Page> {
  const context = await browser.newContext({ locale: "fr-FR", reducedMotion: "reduce" });
  const page = await context.newPage();
  page.on("dialog", (dialog) => dialog.accept().catch(() => undefined));
  return page;
}

export interface NewRequest {
  title: string;
  subject?: string;
  level?: string;
  description?: string;
}

/** Publie une demande et renvoie son URL (sans paramètres). */
export async function publish(page: Page, request: NewRequest): Promise<string> {
  await open(page, "/requests/new");
  await page.getByLabel("Titre").fill(request.title);
  await page.getByLabel("Description").fill(request.description ?? "Description de test suffisamment longue pour être valide.");
  await page.getByLabel("Matière").selectOption({ label: request.subject ?? "Algorithmique" });
  await page.getByLabel("Niveau").selectOption(request.level ?? "L2");
  await page.getByLabel("Disponibilité").fill("Mardi soir");
  await page.getByRole("button", { name: "Publier" }).click();
  await page.waitForURL(/\/requests\/\d+\?created=1/);
  await expect(page.getByText("Votre demande est publiée.")).toBeVisible();
  return page.url().split("?")[0];
}

export async function offerHelp(page: Page, requestUrl: string, message = "Je peux vous aider avec des exemples."): Promise<void> {
  await open(page, requestUrl);
  await page.getByLabel("Message à l'auteur").fill(message);
  await page.getByRole("button", { name: "Envoyer ma proposition" }).click();
  await expect(page.getByText("Proposition envoyée")).toBeVisible();
}
