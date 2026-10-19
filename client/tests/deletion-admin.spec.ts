import { test, expect, type Page } from "@playwright/test";

const ok = (data: unknown) => ({
  success: true,
  code: "0000",
  message: "",
  data,
});
const pageOf = (content: unknown[]) => ({
  content,
  totalElements: content.length,
  totalPages: 1,
  number: 0,
  size: 20,
  first: true,
  last: true,
});

async function setup(
  page: Page,
  type: "ADMIN" | "AUTHOR" | "UNKNOWN" | null,
  source = "MEMBER",
) {
  let deleted = type !== null;
  let deletionType = type;
  let reason: unknown;
  let restores = 0;
  const token = `test.${Buffer.from(JSON.stringify({ role: "ROLE_ADMIN", exp: 4102444800 })).toString("base64url")}.signature`;
  const item = () => ({
    festivalId: 42,
    providerType: source,
    title: "삭제 정책 테스트 행사",
    category: "축제",
    instNm: "운영팀",
    imgUrl: null,
    beginDe: "2026-10-20T00:00:00",
    endDe: "2026-10-21T23:59:59",
    region: "SEOUL",
    status: "OPEN",
    deletedAt: deleted ? "2026-10-19T10:00:00" : null,
    deletion: deleted
      ? {
          deletedAt: "2026-10-19T10:00:00",
          type: deletionType,
          reason: "기존 삭제 사유",
        }
      : null,
  });
  await page.addInitScript(() => sessionStorage.setItem("eventus.mode", "api"));
  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace("/api/v1", "");
    let data: unknown = pageOf([]);
    if (path === "/auth/login" || path === "/auth/refresh")
      data = { accessToken: token };
    else if (path === "/members/me")
      data = {
        id: "admin",
        nickname: "관리자",
        role: "ROLE_ADMIN",
        email: "admin@example.com",
        createdAt: "2026-10-01",
        updatedAt: "2026-10-01",
        profileImg: null,
        phone: null,
      };
    else if (
      path === "/admin/festivals/42" &&
      route.request().method() === "DELETE"
    ) {
      reason = route.request().postDataJSON();
      deleted = true;
      deletionType = "ADMIN";
      data = null;
    } else if (path === "/admin/festivals/42/restore") {
      restores++;
      deleted = false;
      data = null;
    } else if (path === "/admin/festivals")
      data = pageOf(
        !deleted || url.searchParams.get("includeDeleted") === "true"
          ? [item()]
          : [],
      );
    else if (path === "/festivals/42") {
      if (deleted) {
        await route.fulfill({
          status: 404,
          json: { success: false, message: "삭제된 행사", data: null },
        });
        return;
      }
      data = {
        ...item(),
        festivalContent: "행사 설명",
        regionDetail: "서울 행사장",
        referenceUrl: "https://example.com",
        accurateCount: 0,
        inaccurateCount: 0,
        likeCount: 0,
      };
    }
    await route.fulfill({ json: ok(data) });
  });
  await page.goto("/admin/events");
  await page.getByLabel("이메일").fill("admin@example.com");
  await page.getByLabel("비밀번호").fill("test-password");
  await page.getByRole("button", { name: "로그인", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "행사 관리", exact: true }),
  ).toBeVisible();
  return { reason: () => reason, restores: () => restores };
}

test("admin deletion requires reason and sends it to server", async ({
  page,
}) => {
  const state = await setup(page, null);
  await page
    .getByRole("button", { name: "삭제 정책 테스트 행사 검토" })
    .click();
  const dialog = page.getByRole("dialog");
  await dialog.getByRole("button", { name: "삭제", exact: true }).click();
  await expect(
    dialog.getByRole("button", { name: "삭제합니다" }),
  ).toBeDisabled();
  await dialog.getByLabel("삭제 사유").fill("  행사 취소 확인  ");
  await dialog.getByRole("button", { name: "삭제합니다" }).click();
  await expect(dialog).toHaveCount(0);
  expect(state.reason()).toEqual({ reason: "행사 취소 확인" });
});

for (const type of ["ADMIN", "AUTHOR", "UNKNOWN"] as const) {
  test(`restore is based on deletion actor ${type}, not event source`, async ({
    page,
  }) => {
    const state = await setup(
      page,
      type,
      type === "ADMIN" ? "MEMBER" : "PUBLIC",
    );
    await page.getByLabel("삭제된 행사 포함").check();
    await page
      .getByRole("button", { name: "삭제 정책 테스트 행사 검토" })
      .click();
    const dialog = page.getByRole("dialog");
    await expect(dialog).toContainText("기존 삭제 사유");
    const restore = dialog.getByRole("button", { name: "복구", exact: true });
    if (type === "ADMIN") {
      await expect(restore).toBeEnabled();
      await restore.click();
      await expect(dialog).toHaveCount(0);
      expect(state.restores()).toBe(1);
    } else {
      await expect(restore).toBeDisabled();
      expect(state.restores()).toBe(0);
    }
  });
}
