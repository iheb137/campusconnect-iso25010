import { expect, newUser, offerHelp, open, publish, register, test, uid } from "./helpers";

test.describe("Workflow d'une demande d'aide (RG-01 à RG-05)", () => {
  test("FUN-WF-01 : ouverte → aide acceptée → terminée, puis évaluations croisées", async ({ page, browser }) => {
    const tag = uid();
    await register(page, "Alice Martin");
    const requestUrl = await publish(page, { title: `Arbres AVL ${tag}` });
    await expect(page.getByText("Aucune proposition pour l'instant")).toBeVisible();

    const bob = await newUser(browser);
    await register(bob, "Bob Trabelsi");
    await offerHelp(bob, requestUrl, "Je peux t'expliquer avec des schémas.");

    const carol = await newUser(browser);
    await register(carol, "Carol Haddad");
    await offerHelp(carol, requestUrl, "Disponible jeudi.");

    // RG-01 : une seule proposition par aidant
    await open(carol, requestUrl);
    await carol.getByLabel("Message à l'auteur").fill("Encore moi");
    await carol.getByRole("button", { name: "Envoyer ma proposition" }).click();
    await expect(carol.getByText("déjà proposé votre aide")).toBeVisible();

    // RG-03/RG-04 : l'auteur accepte Bob ; Carol est refusée automatiquement
    await open(page, requestUrl);
    await page.getByRole("button", { name: "Accepter la proposition de Bob Trabelsi" }).click();
    await expect(page.getByRole("button", { name: "Marquer la séance comme terminée" })).toBeVisible();
    await expect(page.getByText("Refusée")).toHaveCount(1);
    await expect(page.getByRole("link", { name: "Modifier la demande" })).toHaveCount(0);

    // RG-05 : pas d'évaluation avant la clôture
    await open(bob, requestUrl);
    await expect(bob.getByText("L'évaluation sera possible")).toBeVisible();

    // RG-03 : clôture par l'auteur
    await page.getByRole("button", { name: "Marquer la séance comme terminée" }).click();
    await expect(page.getByText("Séance terminée. Vous pouvez maintenant l'évaluer.")).toBeVisible();

    // RG-05 : note obligatoire, puis évaluation
    await page.getByRole("button", { name: "Envoyer mon évaluation" }).click();
    await expect(page.getByText("Choisissez une note de 1 à 5")).toBeVisible();
    await page.locator('label[for="score-5"]').click();
    await page.getByLabel("Commentaire").fill("Explications très claires, merci !");
    await page.getByRole("button", { name: "Envoyer mon évaluation" }).click();
    await expect(page.getByText("Explications très claires, merci !")).toBeVisible();

    // L'aidant évalue depuis son tableau de bord
    await open(bob, "/dashboard");
    await bob.getByRole("link", { name: "Évaluer" }).first().click();
    await bob.locator('label[for="score-4"]').click();
    await bob.getByRole("button", { name: "Envoyer mon évaluation" }).click();
    await expect(bob.getByRole("img", { name: "4 sur 5" })).toBeVisible();

    // Tableau de bord de l'auteur
    await open(page, "/dashboard");
    await expect(page.getByText("Aidé par Bob Trabelsi").first()).toBeVisible();
  });

  test("FUN-WF-02 (RG-02, RG-11) : l'auteur supprime une demande ouverte ; l'aidant retire sa proposition", async ({
    page,
    browser,
  }) => {
    const tag = uid();
    await register(page, "Emna Saidi");
    const toDelete = await publish(page, { title: `À supprimer ${tag}` });
    const kept = await publish(page, { title: `Graphes ${tag}` });

    const helper = await newUser(browser);
    await register(helper, "Farah Jaziri");
    await offerHelp(helper, kept);
    await open(helper, "/dashboard");
    await helper.getByRole("button", { name: /Retirer ma proposition/ }).click();
    await expect(helper.getByText("Vous n'avez proposé aucune aide")).toBeVisible();

    await open(page, toDelete);
    await page.getByRole("button", { name: "Supprimer la demande" }).click();
    await expect(page).toHaveURL(/\/dashboard\?deleted=1/);
    await open(page, toDelete);
    await expect(page.getByText("Demande introuvable")).toBeVisible();
  });

  test("FUN-WF-03 (RG-01) : l'auteur ne peut pas proposer son aide sur sa propre demande", async ({ page }) => {
    await register(page, "Ghada Ayari");
    const url = await publish(page, { title: `Ma demande ${uid()}` });
    await open(page, url);
    await expect(page.getByLabel("Message à l'auteur")).toHaveCount(0);
  });

  test("SEC-UI-02 : un script saisi dans une demande est affiché comme du texte (pas d'XSS)", async ({ page }) => {
    await register(page, "Hela Mrad");
    let dialogOpened = false;
    page.on("dialog", () => {
      dialogOpened = true;
    });
    const payload = `<img src=x onerror=alert(1)> ${uid()}`;
    const url = await publish(page, { title: payload, description: '<script>alert("xss")</script> description longue' });
    await open(page, url);
    await expect(page.getByRole("heading", { name: payload })).toBeVisible();
    await expect(page.getByText('<script>alert("xss")</script>', { exact: false })).toBeVisible();
    expect(dialogOpened).toBe(false);
  });
});
