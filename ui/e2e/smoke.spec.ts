import { expect, test } from "@playwright/test";

test("public pages render", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("link", { name: /reconpilot/i }).first()).toBeVisible();
  await expect(page.getByRole("link", { name: /create account/i }).first()).toBeVisible();

  await page.getByRole("link", { name: /sign in/i }).first().click();
  await expect(page.getByRole("heading", { name: /sign in/i })).toBeVisible();
  await expect(page.getByLabel(/email/i)).toBeVisible();
  await expect(page.getByLabel(/password/i)).toBeVisible();

  await page.goto("/signup");
  await expect(page.getByRole("heading", { name: /sign up/i })).toBeVisible();

  await page.goto("/verify");
  await expect(page.getByRole("heading", { name: /verify your email/i })).toBeVisible();

  await page.goto("/reset");
  await expect(page.getByRole("heading", { name: /request a reset link/i })).toBeVisible();
});
