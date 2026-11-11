import { expect, newUser, open, publish, register, test, uid } from "./helpers";

test.describe("Recherche et filtres des demandes", () => {
  test("FUN-SEARCH-UI-01 : recherche texte + filtres conservés dans l'URL @mobile", async ({ page, browser }) => {
    const tag = uid();
    const author = await newUser(browser);
    await register(author, "Ines Gharbi");
    await publish(author, { title: `Rotations AVL ${tag}`, subject: "Algorithmique", level: "L2" });
    await publish(author, { title: `Jointures SQL ${tag}`, subject: "Bases de données", level: "L1" });

    await register(page, "Jalel Chaouch");
    await open(page, "/requests");
    await page.getByLabel("Rechercher").fill(tag);
    await expect.poll(() => new URL(page.url()).searchParams.get("q")).toBe(tag);
    await expect(page.getByText("2 demandes")).toBeVisible();

    await page.getByLabel("Matière").selectOption({ label: "Algorithmique" });
    await expect(page.getByText(`Rotations AVL ${tag}`)).toBeVisible();
    await expect(page.getByText(`Jointures SQL ${tag}`)).toHaveCount(0);

    await page.reload();
    await expect(page.getByLabel("Rechercher")).toHaveValue(tag);
    await expect(page.getByText("1 demande")).toBeVisible();

    await page.getByLabel("Statut").selectOption("TERMINEE");
    await expect(page.getByText("Aucune demande ne correspond à ces filtres")).toBeVisible();

    await page.getByRole("button", { name: "Réinitialiser" }).click();
    await expect(page.getByLabel("Rechercher")).toHaveValue("");
  });
});
