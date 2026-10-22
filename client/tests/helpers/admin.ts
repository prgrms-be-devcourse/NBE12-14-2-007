import { expect, type Page } from "@playwright/test";

export function adminToken(role = "ROLE_ADMIN") {
  return `test.${Buffer.from(JSON.stringify({ role, exp: 4102444800 })).toString("base64url")}.signature`;
}

export async function loginAdmin(page: Page, path = "/admin/reviews") {
  await page.goto(path);
  await page.getByLabel("이메일").fill("admin@example.com");
  await page.getByLabel("비밀번호").fill("test-password");
  await page.getByRole("button", { name: "로그인", exact: true }).click();
  await expect(page).not.toHaveURL(/\/admin\/login/);
}
