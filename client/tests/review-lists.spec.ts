import { test, expect, type Page, type Route } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

const post = {
  id: "11111111-1111-4111-8111-111111111111",
  title: "서버에서 받은 후기",
  festivalId: 72,
  festivalTitle: "가을 문화축제",
  member: { id: "writer", nickname: "주말 산책자", profileImg: null },
  thumbnail: null,
  date: "2026-09-23T10:30:00",
};
const adminPost = { ...post, deletedAt: null };
function postPage(
  content: unknown[],
  number = 0,
  size = 6,
  total = content.length,
) {
  return {
    content,
    number,
    size,
    totalElements: total,
    totalPages: Math.ceil(total / size),
    first: number === 0,
    last: (number + 1) * size >= total,
  };
}
async function respond(route: Route, data: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify({
      success: status < 400,
      data: status < 400 ? data : null,
      message: status < 400 ? "" : data,
    }),
  });
}
async function setup(
  page: Page,
  { role = "", preview = false }: { role?: string; preview?: boolean } = {},
) {
  if (!preview)
    await page.addInitScript(() =>
      sessionStorage.setItem("eventus.mode", "api"),
    );
  const requests: { url: URL; method: string; authorization?: string }[] = [];
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    requests.push({
      url,
      method: request.method(),
      authorization: request.headers().authorization,
    });
    if (url.pathname === "/api/v1/auth/refresh")
      return respond(
        route,
        role ? { accessToken: "review-contract-token" } : "로그인 필요",
        role ? 200 : 401,
      );
    if (url.pathname === "/api/v1/members/me")
      return respond(route, {
        id: "member",
        nickname: "조회 회원",
        role,
        email: "review@example.com",
      });
    if (
      url.pathname.startsWith("/api/v1/admin/") ||
      url.pathname === "/api/v1/festivals"
    )
      return respond(
        route,
        postPage([], 0, Number(url.searchParams.get("size") || 20)),
      );
    return respond(route, "예상하지 않은 요청", 404);
  });
  return requests;
}

test("public review list supports anonymous search, server pagination, sorting and URL restoration", async ({
  page,
}) => {
  await setup(page);
  const queries: URLSearchParams[] = [];
  await page.route("**/api/v1/posts?*", async (route) => {
    const query = new URL(route.request().url()).searchParams;
    queries.push(query);
    const number = Number(query.get("page"));
    const count = number === 0 ? 6 : 1;
    return respond(
      route,
      postPage(
        Array.from({ length: count }, (_, i) => ({
          ...post,
          id: `${post.id}-${number}-${i}`,
          title: `서버 후기 ${number * 6 + i + 1}`,
        })),
        number,
        6,
        7,
      ),
    );
  });
  await page.goto("/reviews");
  await expect(page.locator(".post-row")).toHaveCount(6);
  await expect(page.getByLabel("후기 정렬")).toHaveValue("createdAt,desc");
  await expect(page.locator(".count")).toHaveText("7");
  await page.getByRole("button", { name: "다음 페이지" }).click();
  await expect(page.locator(".post-row h3")).toHaveText(["서버 후기 7"]);
  await page.getByLabel("후기 검색 기준").selectOption("MEMBER_NICKNAME");
  await page.getByLabel("후기 검색어").fill("  산책 & 축제+  ");
  await page.getByRole("button", { name: "검색", exact: true }).click();
  await expect.poll(() => queries.at(-1)?.get("keyword")).toBe("산책 & 축제+");
  expect(queries.at(-1)?.get("type")).toBe("MEMBER_NICKNAME");
  expect(queries.at(-1)?.get("page")).toBe("0");
  await page.getByLabel("후기 정렬").selectOption("createdAt,asc");
  await expect.poll(() => queries.at(-1)?.get("sort")).toBe("createdAt,asc");
  await page.reload();
  await expect(page.getByLabel("후기 검색어")).toHaveValue("산책 & 축제+");
  await expect(page.getByLabel("후기 검색 기준")).toHaveValue(
    "MEMBER_NICKNAME",
  );
  await page.getByRole("button", { name: "초기화", exact: true }).click();
  await expect.poll(() => queries.at(-1)?.has("keyword")).toBe(false);
  expect(queries.at(-1)?.has("type")).toBe(false);
  await page.goBack();
  await expect(page.getByLabel("후기 검색어")).toHaveValue("산책 & 축제+");
  await page.getByLabel("후기 검색 기준").selectOption("FESTIVAL_TITLE");
  await page.getByRole("button", { name: "검색", exact: true }).click();
  await expect.poll(() => queries.at(-1)?.get("type")).toBe("FESTIVAL_TITLE");
  expect(
    queries.every(
      (q) =>
        q.get("size") === "6" &&
        ["createdAt,asc", "createdAt,desc"].includes(q.get("sort")!),
    ),
  ).toBe(true);
});

test("list failures clear prior data, retry the API and show an empty search without preview fallback", async ({
  page,
}) => {
  await setup(page);
  let fail = false;
  await page.route("**/api/v1/posts?*", (route) => {
    const keyword = new URL(route.request().url()).searchParams.get("keyword");
    return fail
      ? respond(route, "목록 조회 실패", 503)
      : respond(route, postPage(keyword ? [] : [post]));
  });
  await page.goto("/reviews");
  await expect(page.locator(".post-row")).toHaveCount(1);
  fail = true;
  await page.getByLabel("후기 검색어").fill("없는 후기");
  await page.getByRole("button", { name: "검색", exact: true }).click();
  await expect(page.getByText("목록 조회 실패", { exact: true })).toBeVisible();
  await expect(page.locator(".post-row")).toHaveCount(0);
  fail = false;
  await page.getByRole("button", { name: "다시 시도" }).click();
  await expect(page.getByText("검색 조건에 맞는 후기가 없어요")).toBeVisible();
  await expect(page.locator(".count")).toHaveText("0");
});

for (const role of ["", "ROLE_NORMAL"]) {
  test(`admin reviews do not request protected lists for ${role || "anonymous"} users`, async ({
    page,
  }) => {
    const requests = await setup(page, { role });
    await page.goto("/admin/reviews");
    await expect(
      page.getByRole("heading", {
        name: role ? "관리자 권한이 필요합니다" : "로그인하고 함께해요",
      }),
    ).toBeVisible();
    expect(
      requests.filter(({ url }) => url.pathname.startsWith("/api/v1/admin/")),
    ).toEqual([]);
    await expect(page.locator(".post-list")).toHaveCount(0);
  });
}

test("admin reviews use the protected API even in preview and include server totals, search and detail", async ({
  page,
}) => {
  await setup(page, { role: "ROLE_ADMIN", preview: true });
  const queries: URLSearchParams[] = [];
  await page.route("**/api/v1/admin/posts?*", (route) => {
    expect(route.request().headers().authorization).toBe(
      "Bearer review-contract-token",
    );
    const query = new URL(route.request().url()).searchParams;
    queries.push(query);
    const number = Number(query.get("page"));
    return respond(
      route,
      postPage(
        [{ ...adminPost, title: number ? "두 번째 페이지 후기" : post.title }],
        number,
        Number(query.get("size")),
        21,
      ),
    );
  });
  await page.route(`**/api/v1/posts/${post.id}`, (route) =>
    respond(route, { ...post, content: "서버에서 받은 후기 본문" }),
  );
  await page.goto("/admin/reviews");
  await expect(page.locator(".post-list")).toContainText(post.title);
  await expect(page.getByRole("status")).toContainText("21");
  await page.getByRole("button", { name: "다음 페이지" }).click();
  await expect(page.locator(".post-list")).toContainText("두 번째 페이지 후기");
  await page.getByLabel("후기 검색 기준").selectOption("FESTIVAL_TITLE");
  await page.getByLabel("후기 검색어").fill("문화축제");
  await page.getByRole("button", { name: "검색", exact: true }).click();
  await expect
    .poll(() =>
      queries.some(
        (q) =>
          q.get("keyword") === "문화축제" &&
          q.get("type") === "FESTIVAL_TITLE" &&
          q.get("page") === "0",
      ),
    )
    .toBe(true);
  await page.getByLabel("후기 정렬").selectOption("createdAt,asc");
  await expect
    .poll(() => queries.some((q) => q.get("sort") === "createdAt,asc"))
    .toBe(true);
  await page.reload();
  await expect(page.getByLabel("후기 검색어")).toHaveValue("문화축제");
  await page.getByRole("button", { name: `${post.title} 상세 보기` }).click();
  await expect(page.getByRole("dialog")).toContainText(
    "서버에서 받은 후기 본문",
  );
  await expect(
    page.getByRole("dialog").getByRole("link", { name: post.festivalTitle }),
  ).toHaveAttribute("href", "/events/72");
  await page.getByRole("button", { name: "닫기", exact: true }).last().click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  expect(queries.some((q) => q.get("size") === "1")).toBe(true);
  expect(queries.some((q) => q.get("size") === "6")).toBe(true);
  await expect(
    page.getByText("삭제된 후기도 포함됩니다.", { exact: false }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "처리 내용 저장" }),
  ).toHaveCount(0);
});

test("admin list reports forbidden and deleted-detail errors without inventing visibility", async ({
  page,
}) => {
  await setup(page, { role: "ROLE_ADMIN" });
  let forbidden = true;
  await page.route("**/api/v1/admin/posts?*", (route) =>
    forbidden
      ? respond(route, "후기 조회 권한 없음", 403)
      : respond(route, postPage([adminPost])),
  );
  await page.route(`**/api/v1/posts/${post.id}`, (route) =>
    respond(route, "후기를 찾을 수 없습니다", 404),
  );
  await page.goto("/admin/reviews");
  await expect(
    page.getByText("후기 조회 권한 없음", { exact: true }),
  ).toBeVisible();
  await expect(page.locator(".post-list")).toHaveCount(0);
  forbidden = false;
  await page.getByRole("button", { name: "다시 시도" }).click();
  await page.getByRole("button", { name: `${post.title} 상세 보기` }).click();
  await expect(page.getByRole("dialog")).toContainText(
    "삭제되었거나 찾을 수 없는 후기입니다.",
  );
  await expect(
    page.getByRole("columnheader", { name: "노출 상태" }),
  ).toHaveCount(0);
});

test("preview searches before pagination and preserves likes sorting", async ({
  page,
}) => {
  await setup(page, { role: "ROLE_NORMAL", preview: true });
  await page.goto("/reviews");
  await expect(page.locator(".post-row")).toHaveCount(3);
  await expect(page.getByLabel("후기 정렬")).toHaveValue("likes,desc");
  await expect(page.locator(".post-row h3").first()).toHaveText(
    "걷는 것만으로도 좋았던 저녁",
  );
  await page.getByLabel("후기 검색 기준").selectOption("FESTIVAL_TITLE");
  await page.getByLabel("후기 검색어").fill("수원화성");
  await page.getByRole("button", { name: "검색", exact: true }).click();
  await expect(page.locator(".post-row")).toHaveCount(1);
  await expect(page.locator(".post-row")).toContainText("수원화성");
});

test("local search test opens without login and only calls the review list", async ({
  page,
}) => {
  const requests = await setup(page);
  const queries: URLSearchParams[] = [];
  await page.route("**/api/v1/admin/posts?*", (route) => {
    expect(route.request().method()).toBe("GET");
    expect(route.request().headers().authorization).toBeUndefined();
    const query = new URL(route.request().url()).searchParams;
    queries.push(query);
    return respond(
      route,
      postPage(query.get("keyword") === "없음" ? [] : [adminPost]),
    );
  });
  await page.goto("/admin/reviews");
  await page.getByRole("link", { name: "로그인 없이 검색 테스트" }).click();
  await expect(page).toHaveURL(/test=1/);
  await expect(page.locator(".post-list")).toContainText(post.title);
  await expect(
    page.getByText("로그인 없는 로컬 검색 테스트입니다.", { exact: false }),
  ).toBeVisible();
  await expect(page.getByRole("button", { name: /상세 보기/ })).toHaveCount(0);
  await page.getByLabel("후기 검색 기준").selectOption("MEMBER_NICKNAME");
  await page.getByLabel("후기 검색어").fill("주말");
  await page.getByRole("button", { name: "검색", exact: true }).click();
  await expect.poll(() => queries.at(-1)?.get("type")).toBe("MEMBER_NICKNAME");
  await page.reload();
  await expect(page.locator(".post-list")).toContainText(post.title);
  await page.getByLabel("후기 검색어").fill("없음");
  await page.getByRole("button", { name: "검색", exact: true }).click();
  await expect(page.getByText("조건에 맞는 후기가 없습니다")).toBeVisible();
  expect(
    requests.filter(({ url }) => url.pathname.startsWith("/api/v1/admin/")),
  ).toEqual([]);
  await page.getByRole("link", { name: "테스트 종료" }).click();
  await expect(
    page.getByRole("heading", { name: "로그인하고 함께해요" }),
  ).toBeVisible();
});

test("review lists stay accessible and fit mobile and desktop widths", async ({
  page,
}) => {
  await setup(page, { role: "ROLE_ADMIN" });
  await page.route("**/api/v1/posts?*", (route) =>
    respond(route, postPage([post])),
  );
  await page.route("**/api/v1/admin/posts?*", (route) =>
    respond(
      route,
      postPage([
        adminPost,
        {
          ...adminPost,
          id: "deleted",
          title: "삭제된 후기",
          deletedAt: "2026-09-23T11:00:00",
        },
      ]),
    ),
  );
  for (const path of ["/reviews", "/admin/reviews"]) {
    await page.goto(path);
    await expect(page.getByText(post.title, { exact: true })).toBeVisible();
    for (const width of [320, 390, 768, 1440]) {
      await page.setViewportSize({ width, height: 1000 });
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
      ).toBe(true);
      await expect(page.getByLabel("후기 검색어")).toBeVisible();
      if (width === 390 || width === 1440)
        await page.screenshot({
          path: `test-results/${path.includes("admin") ? "admin" : "public"}-reviews-${width}.png`,
          fullPage: true,
        });
    }
    const audit = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa"])
      .analyze();
    expect(audit.violations).toEqual([]);
  }
});

test("admin uses shared review rows and clearly marks soft-deleted search results", async ({
  page,
}) => {
  const requests = await setup(page, { role: "ROLE_ADMIN" });
  const removed = {
    ...adminPost,
    id: "soft-deleted",
    title: "soft 삭제 확인용 후기",
    deletedAt: "2026-09-23T11:00:00",
  };
  await page.route("**/api/v1/admin/posts?*", (route) => {
    const keyword = new URL(route.request().url()).searchParams.get("keyword");
    return respond(route, postPage(keyword ? [removed] : [adminPost, removed]));
  });
  await page.route("**/api/v1/posts?*", (route) => {
    const keyword = new URL(route.request().url()).searchParams.get("keyword");
    return respond(route, postPage(keyword ? [] : [post]));
  });
  await page.goto("/admin/reviews");
  await expect(page.getByRole("table")).toHaveCount(0);
  await expect(page.locator(".post-row")).toHaveCount(2);
  await expect(page.getByRole("article", { name: post.title })).toContainText(
    "게시 중",
  );
  const deletedRow = page.getByRole("article", { name: removed.title });
  await expect(deletedRow).toContainText("삭제됨");
  await expect(deletedRow).toContainText("삭제 시각 2026.09.23 11:00");
  await expect(deletedRow.getByRole("button")).toHaveCount(0);
  await expect(
    deletedRow.getByRole("link", { name: removed.title }),
  ).toHaveCount(0);
  await page.getByLabel("후기 검색어").fill(removed.title);
  await page.getByRole("button", { name: "검색", exact: true }).click();
  await expect(page.locator(".post-row")).toHaveCount(1);
  await expect(deletedRow).toBeVisible();
  expect(
    requests.some(({ url }) => url.pathname === "/api/v1/posts/soft-deleted"),
  ).toBe(false);
  await page.goto("/reviews");
  await expect(page.locator(".post-row")).toHaveCount(1);
  await expect(page.getByText(removed.title, { exact: true })).toHaveCount(0);
  await page.getByLabel("후기 검색어").fill(removed.title);
  await page.getByRole("button", { name: "검색", exact: true }).click();
  await expect(page.getByText("검색 조건에 맞는 후기가 없어요")).toBeVisible();
});
