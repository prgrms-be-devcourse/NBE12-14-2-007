import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { adminToken, loginAdmin } from "./helpers/admin";

const ok = (data: unknown) => ({
  success: true,
  code: "0000",
  message: "",
  data,
});

test("protected administrator pages require explicit login without requesting protected data", async ({
  page,
}) => {
  const protectedRequests: string[] = [];
  await page.route("**/api/v1/**", (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path.includes("/admin/")) protectedRequests.push(path);
    return route.fulfill({ json: ok([]) });
  });
  for (const path of [
    "/admin",
    "/admin/events",
    "/admin/reviews",
    "/admin/inquiries",
  ]) {
    await page.goto(path);
    await expect(
      page.getByRole("heading", { name: "관리자 로그인" }),
    ).toBeVisible();
    await expect(page).toHaveURL(/\/admin\/login\?next=/);
  }
  expect(protectedRequests).toEqual([]);
  await page.setViewportSize({ width: 390, height: 844 });
  expect(
    (await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa"]).analyze())
      .violations,
  ).toEqual([]);
});

test("administrator login rejects ordinary members and does not expose protected data", async ({
  page,
}) => {
  let protectedCalls = 0;
  await page.route("**/api/v1/**", (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path.includes("/admin/")) protectedCalls++;
    return route.fulfill({
      json: ok(
        path.endsWith("/auth/login")
          ? { accessToken: adminToken("ROLE_UNVERIFIED") }
          : null,
      ),
    });
  });
  await page.goto("/admin/reviews");
  await page.getByLabel("이메일").fill("member@example.com");
  await page.getByLabel("비밀번호").fill("test-password");
  await page.getByRole("button", { name: "로그인", exact: true }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  await expect(page).toHaveURL(/\/admin\/login/);
  expect(protectedCalls).toBe(0);
});

test("administrator login opens its original destination but reload requires login again", async ({
  page,
}) => {
  await page.route("**/api/v1/**", (route) => {
    const path = new URL(route.request().url()).pathname;
    return route.fulfill({
      json: ok(
        path.endsWith("/auth/login")
          ? { accessToken: adminToken() }
          : path.endsWith("/members/me")
            ? {
                id: "admin",
                role: "ROLE_ADMIN",
                nickname: "관리자",
                email: "admin@example.com",
              }
            : {
                content: [],
                totalElements: 0,
                totalPages: 0,
                number: 0,
                first: true,
                last: true,
              },
      ),
    });
  });
  await loginAdmin(page);
  await expect(
    page.getByRole("heading", { name: "후기 관리", exact: true }),
  ).toBeVisible();
  await page.reload();
  await expect(
    page.getByRole("heading", { name: "관리자 로그인" }),
  ).toBeVisible();
});
