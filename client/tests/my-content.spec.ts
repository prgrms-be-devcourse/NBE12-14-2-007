import { test, expect, type Page } from "@playwright/test";

const ok = (data: unknown) => ({
  success: true,
  code: "0000",
  message: "",
  data,
});
const row = (type: string, targetId: string, title: string, extra = {}) => ({
  type,
  targetId,
  title,
  createdAt: "2026-10-19T10:00:00",
  deletedAt: null,
  parentDeleted: false,
  originalPath: `/events/${targetId}`,
  deletion: null,
  ...extra,
});
const deleted = {
  deletedAt: "2026-10-19T11:00:00",
  originalPath: null,
  deletion: {
    action: "DELETE",
    actorType: "ADMIN",
    reasonCode: "SPAM",
    reasonLabel: "도배·스팸",
    reasonDetail: "관리자 삭제 안내",
    createdAt: "2026-10-19T11:00:00",
  },
};

async function setup(page: Page) {
  await page.addInitScript(() => sessionStorage.setItem("eventus.mode", "api"));
  await page.route("**/api/v1/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === "/api/v1/auth/refresh")
      return route.fulfill({ json: ok({ accessToken: "test-token" }) });
    if (path === "/api/v1/members/me")
      return route.fulfill({
        json: ok({
          id: "me",
          nickname: "테스트 회원",
          email: "me@example.com",
          role: "ROLE_UNVERIFIED",
          profileImg: null,
        }),
      });
    return route.fulfill({ json: ok([]) });
  });
}

test("my posts and comments show classifications, deletion reasons and only available original links", async ({
  page,
}) => {
  await setup(page);
  await page.route("**/api/v1/members/me/posts?*", (route) => {
    const index = Number(
      new URL(route.request().url()).searchParams.get("page"),
    );
    const content = index
      ? [row("POST", "next", "다음 페이지 후기")]
      : [
          row("FESTIVAL", "42", "내 행사 제보"),
          row("POST", "review", "삭제된 내 후기", deleted),
          row("COMMUNITY_POST", "community", "내 커뮤니티 글", {
            originalPath: "/community/community",
          }),
        ];
    return route.fulfill({
      json: ok({
        content,
        number: index,
        totalElements: 4,
        totalPages: 2,
        first: !index,
        last: !!index,
      }),
    });
  });
  await page.route("**/api/v1/members/me/comments?*", (route) =>
    route.fulfill({
      json: ok({
        content: [
          row("COMMENT", "17", "원문이 삭제된 댓글", {
            parentDeleted: true,
            originalPath: null,
          }),
          row("COMMUNITY_COMMENT", "18", "삭제된 커뮤니티 댓글", deleted),
        ],
        number: 0,
        totalElements: 2,
        totalPages: 1,
      }),
    }),
  );
  await page.goto("/mypage?tab=submissions");
  const posts = page.getByRole("region", {
    name: "내가 작성한 글",
    exact: true,
  });
  await expect(posts.getByRole("row")).toHaveCount(4);
  await expect(posts.getByRole("link", { name: "원문 보기" })).toHaveCount(2);
  const removed = posts.getByRole("row").filter({ hasText: "삭제된 내 후기" });
  await expect(removed).toContainText("삭제됨");
  await expect(removed.getByRole("link")).toHaveCount(0);
  await removed.getByRole("button", { name: "삭제 사유 보기" }).click();
  await expect(page.getByRole("dialog")).toContainText("관리자 삭제 안내");
  await page.getByRole("dialog").getByText("닫기", { exact: true }).click();
  await posts.getByRole("button", { name: "다음", exact: true }).click();
  await expect(posts).toContainText("다음 페이지 후기");
  await page
    .getByRole("button", { name: "내가 작성한 댓글", exact: true })
    .click();
  const comments = page.getByRole("region", {
    name: "내가 작성한 댓글",
    exact: true,
  });
  await expect(comments).toContainText("원문 삭제");
  await comments.getByRole("button", { name: "원문 삭제 안내" }).click();
  await expect(page.getByRole("dialog")).toContainText(
    "이 댓글 자체가 삭제된 것은 아닙니다.",
  );
});

test("failed own-content query shows an error and permits retry", async ({
  page,
}) => {
  await setup(page);
  let fail = true;
  await page.route("**/api/v1/members/me/posts?*", (route) =>
    fail
      ? route.fulfill({
          status: 500,
          json: { success: false, message: "조회 실패", data: null },
        })
      : route.fulfill({
          json: ok({ content: [], number: 0, totalElements: 0, totalPages: 0 }),
        }),
  );
  await page.goto("/mypage?tab=submissions");
  await expect(page.getByText("조회 실패", { exact: true })).toBeVisible();
  fail = false;
  await page.getByRole("button", { name: "다시 시도" }).click();
  await expect(
    page.getByRole("heading", { name: "작성한 내용이 없습니다" }),
  ).toBeVisible();
});

async function eventPage(page: Page, role = "ROLE_UNVERIFIED", author = "me") {
  await setup(page);
  await page.route("**/api/v1/members/me", (route) =>
    route.fulfill({
      json: ok({
        id: "me",
        nickname: "테스트 회원",
        email: "me@example.com",
        role,
        profileImg: null,
      }),
    }),
  );
  await page.route("**/api/v1/festivals/42", (route) =>
    route.fulfill({
      json: ok({
        festivalId: 42,
        providerType: "MEMBER",
        title: "내 행사 제보",
        category: "ETC",
        region: "SEOUL",
        regionDetail: "",
        beginDe: "2026-10-19",
        endDe: "2026-10-20",
        status: "OPEN",
        accurateCount: 0,
        inaccurateCount: 0,
        likeCount: 0,
        member: {
          id: author,
          nickname: "제보자",
          role: "ROLE_UNVERIFIED",
          profileImg: null,
        },
      }),
    }),
  );
  await page.route("**/api/v1/members/me/submissions", (route) =>
    route.fulfill({
      json: ok([{ festivalId: 42, festivalSubmissionId: "own-submission" }]),
    }),
  );
}

test("author can delete a submission from its public original reached through my page", async ({
  page,
}) => {
  await eventPage(page);
  await page.route("**/api/v1/members/me/posts?*", (route) =>
    route.fulfill({
      json: ok({
        content: [row("FESTIVAL", "42", "내 행사 제보")],
        number: 0,
        totalElements: 1,
        totalPages: 1,
      }),
    }),
  );
  await page.route("**/api/v1/members/me/submissions/own-submission", (route) =>
    route.fulfill({ json: ok(null) }),
  );
  await page.goto("/mypage?tab=submissions");
  await page.getByRole("link", { name: "원문 보기" }).click();
  await expect(page).toHaveURL(/\/events\/42$/);
  await page.getByRole("button", { name: "내 제보 삭제", exact: true }).click();
  const request = page.waitForRequest(
    (r) =>
      r.method() === "DELETE" &&
      r.url().endsWith("/members/me/submissions/own-submission"),
  );
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "제보 삭제", exact: true })
    .click();
  await request;
  await expect(page).toHaveURL(/\/mypage\?tab=submissions$/);
});

test("author can open submission editing from the public event", async ({
  page,
}) => {
  await eventPage(page);
  await page.goto("/events/42");
  await page.getByRole("button", { name: "제보 수정", exact: true }).click();
  await expect(page).toHaveURL(/\/submissions\/own-submission\/edit$/);
});

test("another member has no submission deletion control", async ({ page }) => {
  await eventPage(page, "ROLE_UNVERIFIED", "another-member");
  await page.goto("/events/42");
  await expect(
    page.getByRole("heading", { name: "내 행사 제보", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: /내 제보 삭제|행사 삭제/ }),
  ).toHaveCount(0);
});

test("admin deletes another member's event through the common reason-based API", async ({
  page,
}) => {
  await eventPage(page, "ROLE_ADMIN", "another-member");
  await page.route("**/api/v1/admin/deletions", (route) =>
    route.fulfill({ json: ok(null) }),
  );
  await page.goto("/events/42");
  await page.getByRole("button", { name: "행사 삭제", exact: true }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("삭제 사유 분류").selectOption("OTHER");
  await expect(
    dialog.getByRole("button", { name: "삭제", exact: true }),
  ).toBeDisabled();
  await dialog.getByLabel("상세 사유").fill("운영 정책 위반 확인");
  const request = page.waitForRequest(
    (r) => r.method() === "POST" && r.url().endsWith("/admin/deletions"),
  );
  await dialog.getByRole("button", { name: "삭제", exact: true }).click();
  expect((await request).postDataJSON()).toEqual({
    targetType: "FESTIVAL",
    targetId: "42",
    reasonCode: "OTHER",
    reasonDetail: "운영 정책 위반 확인",
  });
  await expect(page).toHaveURL(/\/explore$/);
});
