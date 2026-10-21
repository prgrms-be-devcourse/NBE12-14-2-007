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
    createdAt: "2026-10-19T10:00:00",
    endDe: "2026-10-21T23:59:59",
    region: "SEOUL",
    status: "OPEN",
    deletedAt: deleted ? "2026-10-19T10:00:00" : null,
    deletion:
      deleted && deletionType !== "UNKNOWN"
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
      path === "/admin/deletions" &&
      route.request().method() === "POST"
    ) {
      reason = route.request().postDataJSON();
      deleted = true;
      deletionType = "ADMIN";
      data = null;
    } else if (
      path === "/admin/deletions/restore" &&
      route.request().method() === "POST"
    ) {
      expect(route.request().postDataJSON()).toEqual({
        targetType: "FESTIVAL",
        targetId: "42",
      });
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
  if (type === null) {
    await expect(
      page.getByRole("columnheader", { name: "등록일", exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("row").filter({ hasText: "삭제 정책 테스트 행사" }),
    ).toContainText("2026.10.19");
  }
  return { reason: () => reason, restores: () => restores };
}

test("admin deletion requires reason and sends it to server", async ({
  page,
}) => {
  const state = await setup(page, null);
  await page
    .getByRole("button", { name: "삭제 정책 테스트 행사 관리하기" })
    .click();
  const dialog = page.getByRole("dialog");
  await dialog.getByRole("button", { name: "삭제", exact: true }).click();
  await dialog.getByLabel("삭제 사유 분류").selectOption("OTHER");
  await expect(
    dialog.getByRole("button", { name: "삭제합니다" }),
  ).toBeDisabled();
  await dialog.getByLabel("상세 사유").fill("  행사 취소 확인  ");
  await dialog.getByRole("button", { name: "삭제합니다" }).click();
  await expect(dialog).toHaveCount(0);
  expect(state.reason()).toEqual({
    targetType: "FESTIVAL",
    targetId: "42",
    reasonCode: "OTHER",
    reasonDetail: "행사 취소 확인",
  });
});

test("failed deletion keeps the dialog open for correction", async ({
  page,
}) => {
  await setup(page, null);
  await page.route("**/api/v1/admin/deletions", async (route) => {
    await route.fulfill({
      status: 409,
      json: {
        success: false,
        code: "CONFLICT",
        message: "이미 처리된 대상입니다.",
        data: null,
      },
    });
  });
  await page
    .getByRole("button", { name: "삭제 정책 테스트 행사 관리하기" })
    .click();
  const dialog = page.getByRole("dialog");
  await dialog.getByRole("button", { name: "삭제", exact: true }).click();
  await dialog.getByLabel("삭제 사유 분류").selectOption("SPAM");
  const response = page.waitForResponse(
    (res) => res.url().endsWith("/admin/deletions") && res.status() === 409,
  );
  await dialog.getByRole("button", { name: "삭제합니다" }).click();
  await response;
  await expect(dialog).toBeVisible();
  await expect(
    dialog.getByRole("button", { name: "삭제합니다" }),
  ).toBeEnabled();
});

test("classified deletion does not require optional detail", async ({
  page,
}) => {
  const state = await setup(page, null);
  await page
    .getByRole("button", { name: "삭제 정책 테스트 행사 관리하기" })
    .click();
  const dialog = page.getByRole("dialog");
  await dialog.getByRole("button", { name: "삭제", exact: true }).click();
  await dialog.getByLabel("삭제 사유 분류").selectOption("SPAM");
  await dialog.getByRole("button", { name: "삭제합니다" }).click();
  await expect(dialog).toHaveCount(0);
  expect(state.reason()).toEqual({
    targetType: "FESTIVAL",
    targetId: "42",
    reasonCode: "SPAM",
  });
});

test("review and reply deletion preserve threads and use common deletion APIs", async ({
  page,
}) => {
  await setup(page, null);
  const post = {
    id: "review-42",
    title: "관리 대상 후기",
    date: "2026-10-19T10:00:00",
    member: { id: "writer", nickname: "작성자", profileImg: null },
    content: "후기 본문",
    deletedAt: null,
  };
  let deletedComment = false;
  const requests: unknown[] = [];
  const restorations: unknown[] = [];
  await page.route("**/api/v1/admin/posts?*", (route) =>
    route.fulfill({ json: ok(pageOf([post])) }),
  );
  await page.route("**/api/v1/posts/review-42", (route) =>
    route.fulfill({ json: ok(post) }),
  );
  await page.route("**/api/v1/admin/posts/review-42/comments?*", (route) =>
    route.fulfill({
      json: ok(
        pageOf([
          {
            id: 5,
            parentId: null,
            member: post.member,
            content: "보존할 부모 댓글",
            date: post.date,
            deletedAt: null,
            parentDeleted: false,
            deletion: null,
            replies: [
              {
                id: 17,
                parentId: 5,
                replies: [],
                member: post.member,
                content: "삭제할 댓글",
                date: post.date,
                deletedAt: deletedComment ? post.date : null,
                parentDeleted: false,
                deletion: deletedComment
                  ? {
                      action: "DELETE",
                      actorType: "ADMIN",
                      reasonCode: "SPAM",
                      reasonLabel: "도배·스팸",
                      reasonDetail: null,
                      createdAt: post.date,
                    }
                  : null,
              },
            ],
          },
        ]),
      ),
    }),
  );
  await page.route("**/api/v1/admin/deletions", (route) => {
    requests.push(route.request().postDataJSON());
    deletedComment = true;
    return route.fulfill({ json: ok(null) });
  });
  await page.route("**/api/v1/admin/deletions/restore", (route) => {
    restorations.push(route.request().postDataJSON());
    deletedComment = false;
    return route.fulfill({ json: ok(null) });
  });
  await page.getByRole("link", { name: "후기 관리", exact: true }).click();
  await page.getByRole("button", { name: "관리 대상 후기 검토" }).click();
  const dialog = page.getByRole("dialog");
  const comment = dialog
    .getByRole("listitem")
    .filter({ hasText: "삭제할 댓글" });
  await comment.getByRole("button", { name: "댓글 삭제" }).click();
  await comment.getByLabel("삭제 사유 분류").selectOption("SPAM");
  await comment.getByRole("button", { name: "삭제", exact: true }).click();
  await expect(comment).toContainText("삭제됨");
  await expect(comment).toHaveClass(/is-reply/);
  await expect(
    dialog.getByText("보존할 부모 댓글", { exact: true }),
  ).toBeVisible();
  await expect(comment.getByText("삭제할 댓글", { exact: true })).toHaveCSS(
    "text-decoration-line",
    "line-through",
  );
  await expect(comment).toContainText("도배·스팸");
  await expect(
    comment.getByRole("button", { name: "댓글 복구" }),
  ).toBeEnabled();
  // 팝업을 다시 열어도 서버에서 삭제 상태를 다시 읽어 유지해야 한다.
  await dialog.getByText("닫기", { exact: true }).click();
  await page.getByRole("button", { name: "관리 대상 후기 검토" }).click();
  await expect(comment).toContainText("삭제됨");
  await comment.getByRole("button", { name: "댓글 복구" }).click();
  await expect(
    comment.getByRole("button", { name: "댓글 삭제" }),
  ).toBeVisible();
  await expect(comment.getByText("삭제할 댓글", { exact: true })).toHaveCSS(
    "text-decoration-line",
    "none",
  );
  expect(restorations).toEqual([{ targetType: "COMMENT", targetId: "17" }]);
  await dialog.getByRole("button", { name: "후기 삭제", exact: true }).click();
  await dialog.getByLabel("삭제 사유 분류").selectOption("OTHER");
  await expect(
    dialog.getByRole("button", { name: "후기를 삭제합니다" }),
  ).toBeDisabled();
  await dialog.getByLabel("상세 사유").fill("운영 정책 위반");
  await dialog.getByRole("button", { name: "후기를 삭제합니다" }).click();
  await expect(dialog).toHaveCount(0);
  expect(requests).toEqual([
    { targetType: "COMMENT", targetId: "17", reasonCode: "SPAM" },
    {
      targetType: "POST",
      targetId: "review-42",
      reasonCode: "OTHER",
      reasonDetail: "운영 정책 위반",
    },
  ]);
});

test("deleted comments explain why restoration is unavailable", async ({
  page,
}) => {
  await setup(page, null);
  const post = {
    id: "review-42",
    title: "댓글 상태 확인",
    member: { nickname: "작성자" },
    content: "본문",
    date: "2026-10-19T10:00:00",
    deletedAt: null,
  };
  await page.route("**/api/v1/admin/posts?*", (route) =>
    route.fulfill({ json: ok(pageOf([post])) }),
  );
  await page.route("**/api/v1/posts/review-42", (route) =>
    route.fulfill({ json: ok(post) }),
  );
  await page.route("**/api/v1/admin/posts/review-42/comments?*", (route) =>
    route.fulfill({
      json: ok(
        pageOf([
          {
            id: 1,
            parentId: null,
            replies: [],
            member: post.member,
            content: "작성자 삭제 댓글",
            date: post.date,
            deletedAt: post.date,
            parentDeleted: false,
            deletion: {
              action: "DELETE",
              actorType: "AUTHOR",
              reasonLabel: "작성자 직접 삭제",
            },
          },
          {
            id: 2,
            parentId: null,
            replies: [],
            member: post.member,
            content: "부모 삭제 댓글",
            date: post.date,
            deletedAt: post.date,
            parentDeleted: true,
            deletion: {
              action: "DELETE",
              actorType: "ADMIN",
              reasonLabel: "도배·스팸",
            },
          },
          {
            id: 3,
            parentId: null,
            replies: [],
            member: post.member,
            content: "이력 없는 댓글",
            date: post.date,
            deletedAt: post.date,
            parentDeleted: false,
            deletion: null,
          },
        ]),
      ),
    }),
  );
  await page.getByRole("link", { name: "후기 관리", exact: true }).click();
  await page.getByRole("button", { name: "댓글 상태 확인 검토" }).click();
  for (const content of [
    "작성자 삭제 댓글",
    "부모 삭제 댓글",
    "이력 없는 댓글",
  ]) {
    await expect(
      page
        .getByRole("listitem")
        .filter({ hasText: content })
        .getByRole("button", { name: "댓글 복구" }),
    ).toBeDisabled();
  }
  await expect(
    page.getByText("작성자 삭제 · 복구 불가", { exact: true }),
  ).toBeVisible();
  await expect(
    page.getByText("후기를 먼저 복구해 주세요.", { exact: true }),
  ).toBeVisible();
  await expect(
    page.getByText("삭제 이력이 없어 복구할 수 없습니다.", { exact: true }),
  ).toBeVisible();
});

for (const targetType of [
  "POST",
  "COMMENT",
  "COMMUNITY_POST",
  "COMMUNITY_COMMENT",
] as const) {
  test(`public administrator deletion uses classified history API for ${targetType}`, async ({
    page,
  }) => {
    await setup(page, null);
    const member = {
      id: "writer",
      nickname: "작성자",
      profileImg: null,
      role: "ROLE_UNVERIFIED",
    };
    const post = {
      id: "review-42",
      member,
      title: "관리할 원문",
      content: "본문",
      category: "FREE",
      date: "2026-10-20T10:00:00",
      createdAt: "2026-10-20T10:00:00",
      viewCount: 0,
      likeCount: 0,
      commentCount: 1,
      festivalId: 42,
    };
    const comment = {
      id: 17,
      postId: post.id,
      member,
      content: "관리할 답글",
      parentId: 5,
      deleted: false,
      replies: [],
      date: post.date,
      createdAt: post.date,
    };
    await page.route("**/api/v1/posts/review-42", (route) =>
      route.fulfill({ json: ok(post) }),
    );
    await page.route("**/api/v1/community/posts/review-42", (route) =>
      route.fulfill({ json: ok(post) }),
    );
    for (const path of [
      "posts/review-42/comments",
      "community/posts/review-42/comments",
    ]) {
      await page.route(`**/api/v1/${path}?*`, (route) =>
        route.fulfill({
          json: ok(
            pageOf([
              {
                ...comment,
                id: 5,
                parentId: null,
                content: "부모 댓글",
                replies: [comment],
              },
            ]),
          ),
        }),
      );
    }
    let sent: unknown;
    await page.route("**/api/v1/admin/deletions", (route) => {
      sent = route.request().postDataJSON();
      return route.fulfill({ json: ok(null) });
    });
    await page.goto(
      targetType.startsWith("COMMUNITY")
        ? "/community/review-42"
        : "/reviews/review-42",
    );
    if (targetType.endsWith("COMMENT")) {
      const css =
        targetType === "COMMENT" ? ".comment-row" : ".community-comment";
      await page
        .locator(css)
        .filter({ has: page.getByText("관리할 답글", { exact: true }) })
        .getByRole("button", { name: "삭제", exact: true })
        .click();
    } else {
      await page
        .locator(
          targetType === "POST"
            ? ".article-actions"
            : ".community-owner-actions",
        )
        .getByRole("button", { name: "삭제", exact: true })
        .click();
    }
    const dialog = page.getByRole("dialog");
    await expect(
      dialog.getByRole("heading", { name: "관리자 권한으로 삭제할까요?" }),
    ).toBeVisible();
    await dialog.getByLabel("삭제 사유 분류").selectOption("SPAM");
    const response = page.waitForResponse(
      (r) =>
        r.url().endsWith("/admin/deletions") && r.request().method() === "POST",
    );
    await dialog.getByRole("button", { name: "삭제", exact: true }).click();
    await response;
    expect(sent).toEqual({
      targetType,
      targetId: targetType.endsWith("COMMENT") ? "17" : "review-42",
      reasonCode: "SPAM",
    });
  });
}

for (const actor of ["ADMIN", "AUTHOR", "RESTORED", "NO_HISTORY"] as const) {
  test(`deleted review restoration is based on server history ${actor}`, async ({
    page,
  }) => {
    await setup(page, null);
    let restored = false;
    const post = () => ({
      id: "deleted-review",
      title: "삭제된 후기 복구",
      member: { id: "writer", nickname: "작성자" },
      date: "2026-10-19T10:00:00",
      deletedAt: restored ? null : "2026-10-19T11:00:00",
    });
    await page.route("**/api/v1/admin/posts?*", (route) =>
      route.fulfill({ json: ok(pageOf([post()])) }),
    );
    await page.route("**/api/v1/admin/deletions/latest?*", (route) => {
      const query = new URL(route.request().url()).searchParams;
      expect(query.get("targetType")).toBe("POST");
      expect(query.get("targetId")).toBe("deleted-review");
      return route.fulfill({
        json: ok(
          actor === "NO_HISTORY"
            ? null
            : {
                action: actor === "RESTORED" ? "RESTORE" : "DELETE",
                actorType: actor === "AUTHOR" ? "AUTHOR" : "ADMIN",
                reasonCode: "SPAM",
                reasonLabel: "도배·스팸",
                reasonDetail: "삭제 사유 보존 확인",
                createdAt: "2026-10-19T11:00:00",
              },
        ),
      });
    });
    await page.route("**/api/v1/admin/deletions/restore", (route) => {
      expect(route.request().method()).toBe("POST");
      expect(route.request().postDataJSON()).toEqual({
        targetType: "POST",
        targetId: "deleted-review",
      });
      restored = true;
      return route.fulfill({ json: ok(null) });
    });
    await page.getByRole("link", { name: "후기 관리", exact: true }).click();
    await page.getByRole("button", { name: "삭제된 후기 복구 검토" }).click();
    const dialog = page.getByRole("dialog");
    const restore = dialog.getByRole("button", {
      name: "후기 복구",
      exact: true,
    });
    if (actor === "ADMIN") {
      await expect(dialog).toContainText("삭제 사유 보존 확인");
      await expect(restore).toBeEnabled();
      await restore.click();
      await expect(dialog).toHaveCount(0);
      expect(restored).toBe(true);
    } else {
      await expect(restore).toBeDisabled();
      expect(restored).toBe(false);
    }
  });
}

for (const targetType of [
  "FESTIVAL",
  "POST",
  "COMMENT",
  "COMMUNITY_POST",
  "COMMUNITY_COMMENT",
] as const) {
  test(`report deletion sends inquiryId and refreshes answer for ${targetType}`, async ({
    page,
  }) => {
    await setup(page, null);
    let answered = false;
    let restored = false;
    let rejectRestore = targetType === "COMMENT";
    let sent: unknown;
    const ticket = () => ({
      id: "report-42",
      category: "REPORT",
      targetType,
      targetId: "42",
      title: "삭제 요청 신고",
      writer: { id: "writer", nickname: "신고자" },
      status: answered ? "ANSWERED" : "PENDING",
      createdAt: "2026-10-19T10:00:00",
      deletedAt: null,
      targetPostId: "parent-42",
      content: "신고 내용",
      img: null,
      answer: answered ? "콘텐츠를 삭제했습니다. 도배·스팸" : null,
    });
    await page.route("**/api/v1/admin/inquiries?*", (route) =>
      route.fulfill({ json: ok(pageOf([ticket()])) }),
    );
    await page.route("**/api/v1/admin/inquiries/report-42", (route) =>
      route.fulfill({ json: ok(ticket()) }),
    );
    await page.route("**/api/v1/admin/festivals/42", (route) =>
      route.fulfill({
        json: ok({
          festivalId: 42,
          title: "대상 행사",
          festivalContent: "본문",
          referenceUrl: "",
          regionDetail: "",
        }),
      }),
    );
    await page.route("**/api/v1/posts/42", (route) =>
      answered && !restored
        ? route.fulfill({
            status: 404,
            json: { success: false, message: "삭제된 후기", data: null },
          })
        : route.fulfill({
            json: ok({
              id: "42",
              title: "대상 후기",
              member: { nickname: "작성자" },
              content: "본문",
            }),
          }),
    );
    await page.route("**/api/v1/community/posts/42", (route) =>
      answered && !restored
        ? route.fulfill({
            status: 404,
            json: { success: false, message: "삭제된 커뮤니티 글", data: null },
          })
        : route.fulfill({
            json: ok({ id: "42", title: "대상 글", content: "본문" }),
          }),
    );
    await page.route("**/api/v1/admin/deletions", (route) => {
      expect(route.request().method()).toBe("POST");
      sent = route.request().postDataJSON();
      answered = true;
      return route.fulfill({ json: ok(null) });
    });
    await page.route("**/api/v1/admin/deletions/latest?*", (route) =>
      route.fulfill({
        json: ok(
          answered
            ? {
                action: restored ? "RESTORE" : "DELETE",
                actorType: "ADMIN",
                reasonCode: "SPAM",
                reasonLabel: "도배·스팸",
                reasonDetail: "신고 삭제 사유",
                createdAt: "2026-10-19T11:00:00",
              }
            : null,
        ),
      }),
    );
    await page.route("**/api/v1/admin/deletions/restore", (route) => {
      expect(route.request().postDataJSON()).toEqual({
        targetType,
        targetId: "42",
      });
      if (rejectRestore) {
        rejectRestore = false;
        return route.fulfill({
          status: 409,
          json: {
            success: false,
            code: "PARENT_DELETED",
            message: "부모 글이 삭제되어 복구할 수 없습니다.",
            data: null,
          },
        });
      }
      restored = true;
      return route.fulfill({ json: ok(null) });
    });
    await page
      .getByRole("link", { name: "문의·신고·제보", exact: true })
      .click();
    await page.getByRole("button", { name: "신고", exact: true }).click();
    await page.getByRole("button", { name: "report-42 접수 상세" }).click();
    const dialog = page.getByRole("dialog");
    const actions = dialog.getByRole("region", { name: "대상 처리" });
    await actions.getByRole("button", { name: /삭제$/ }).click();
    await actions.getByLabel("삭제 사유 분류").selectOption("SPAM");
    await actions
      .getByRole("button", { name: "삭제합니다", exact: true })
      .click();
    await expect(dialog).toContainText("답변 완료");
    await expect(dialog).toContainText("콘텐츠를 삭제했습니다. 도배·스팸");
    expect(sent).toEqual({
      targetType,
      targetId: "42",
      reasonCode: "SPAM",
      inquiryId: "report-42",
    });
    await expect(actions).toContainText("신고 삭제 사유");
    const restore = actions.getByRole("button", {
      name: /복구$/,
      exact: false,
    });
    await expect(restore).toBeEnabled();
    await restore.click();
    if (targetType === "COMMENT") {
      await expect(actions.getByRole("alert")).toContainText(
        "부모 글이 삭제되어",
      );
      await expect(restore).toBeEnabled();
      await restore.click();
    }
    await expect(actions).toContainText("콘텐츠를 복구했습니다.");
    await expect(restore).toHaveCount(0);
    await expect(dialog).toContainText("콘텐츠를 삭제했습니다. 도배·스팸");
    expect(restored).toBe(true);
  });
}

test("duplicate restoration error stays visible inside event management dialog", async ({
  page,
}) => {
  await setup(page, "ADMIN");
  await page.route("**/api/v1/admin/deletions/restore", (route) =>
    route.fulfill({
      status: 409,
      json: {
        success: false,
        code: "FESTIVAL006",
        message: "이미 등록된 행사입니다.",
        data: null,
      },
    }),
  );
  await page.getByLabel("삭제된 행사 포함").check();
  await page
    .getByRole("button", { name: "삭제 정책 테스트 행사 관리하기" })
    .click();
  const dialog = page.getByRole("dialog");
  await dialog.getByRole("button", { name: "복구", exact: true }).click();
  await expect(dialog.getByRole("alert")).toContainText(
    "이미 등록된 행사입니다.",
  );
  await expect(
    dialog.getByRole("button", { name: "복구", exact: true }),
  ).toBeEnabled();
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
      .getByRole("button", { name: "삭제 정책 테스트 행사 관리하기" })
      .click();
    const dialog = page.getByRole("dialog");
    if (type === "UNKNOWN") {
      await expect(dialog).toContainText("삭제 주체를 확인할 수 없는");
      await expect(dialog).not.toContainText("기존 삭제 사유");
    } else {
      await expect(dialog).toContainText("기존 삭제 사유");
    }
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
