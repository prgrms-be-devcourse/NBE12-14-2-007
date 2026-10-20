import { test, expect, type Page, type Route } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { adminToken, loginAdmin } from "./helpers/admin";

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
    if (url.pathname === "/api/v1/auth/login")
      return respond(route, { accessToken: adminToken(role) });
    if (url.pathname === "/api/v1/admin/deletions/latest")
      return respond(route, null);
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

test("preview searches before pagination and preserves likes sorting", async ({
  page,
}) => {
  await setup(page, { role: "ROLE_UNVERIFIED", preview: true });
  await page.goto("/reviews");
  await expect(page.locator(".post-row")).toHaveCount(3);
  await expect(page.getByLabel("후기 정렬")).toHaveValue("likeCount,desc");
  await expect(page.locator(".post-row h3").first()).toHaveText(
    "걷는 것만으로도 좋았던 저녁",
  );
  await page.getByLabel("후기 검색 기준").selectOption("FESTIVAL_TITLE");
  await page.getByLabel("후기 검색어").fill("수원화성");
  await page.getByRole("button", { name: "검색", exact: true }).click();
  await expect(page.locator(".post-row")).toHaveCount(1);
  await expect(page.locator(".post-row")).toContainText("수원화성");
});

test("administrator review list supports search, pagination and original detail", async ({
  page,
}) => {
  await setup(page, { role: "ROLE_ADMIN" });
  const queries: URLSearchParams[] = [];
  await page.route("**/api/v1/admin/posts?*", (route) => {
    expect(route.request().headers().authorization).toBe(
      "Bearer " + adminToken(),
    );
    const query = new URL(route.request().url()).searchParams;
    queries.push(query);
    const number = Number(query.get("page"));
    return respond(
      route,
      postPage(
        [{ ...adminPost, title: number ? "두 번째 페이지 후기" : post.title }],
        number,
        20,
        21,
      ),
    );
  });
  await page.route(`**/api/v1/posts/${post.id}`, (route) =>
    respond(route, { ...post, content: "서버에서 받은 후기 본문" }),
  );
  await loginAdmin(page);
  await expect(page.getByRole("table")).toContainText(post.title);
  await expect(page.locator(".adm-result-count")).toContainText("21");
  await page.getByRole("button", { name: "다음", exact: true }).click();
  await expect(page.getByRole("table")).toContainText("두 번째 페이지 후기");
  await page.getByLabel("후기 검색 기준").selectOption("FESTIVAL_TITLE");
  await page.getByLabel("후기 검색", { exact: true }).fill("문화축제");
  await expect.poll(() => queries.at(-1)?.get("keyword")).toBe("문화축제");
  expect(queries.at(-1)?.get("type")).toBe("FESTIVAL_TITLE");
  expect(queries.at(-1)?.get("page")).toBe("0");
  await page.getByLabel("후기 정렬").selectOption("createdAt,asc");
  await expect.poll(() => queries.at(-1)?.get("sort")).toBe("createdAt,asc");
  await page.getByRole("button", { name: post.title + " 검토" }).click();
  await expect(page.getByRole("dialog")).toContainText(
    "서버에서 받은 후기 본문",
  );
  await expect(
    page.getByRole("dialog").getByRole("link", { name: post.festivalTitle }),
  ).toHaveAttribute("href", "/events/72");
});

test("administrator list reports query failure and missing detail without claiming restoration is available", async ({
  page,
}) => {
  await setup(page, { role: "ROLE_ADMIN" });
  let fail = true;
  await page.route("**/api/v1/admin/posts?*", (route) =>
    fail
      ? respond(route, "후기 조회 권한 없음", 403)
      : respond(route, postPage([adminPost])),
  );
  await page.route(`**/api/v1/posts/${post.id}`, (route) =>
    respond(route, "후기를 찾을 수 없습니다", 404),
  );
  await loginAdmin(page);
  await expect(page.getByRole("alert")).toContainText("후기 조회 권한 없음");
  fail = false;
  await page.getByRole("button", { name: "다시 시도" }).click();
  await page.getByRole("button", { name: post.title + " 검토" }).click();
  await expect(page.getByRole("dialog")).toContainText("삭제된 후기입니다.");
  await expect(
    page.getByRole("button", { name: "후기 복구", exact: true }),
  ).toBeDisabled();
});

test("deleted review rows remain manageable and administrator search is accessible on small screens", async ({
  page,
}) => {
  await setup(page, { role: "ROLE_ADMIN" });
  const removed = {
    ...adminPost,
    id: "removed-review",
    title: "삭제된 후기",
    deletedAt: "2026-09-23T11:00:00",
  };
  await page.route("**/api/v1/admin/posts?*", (route) =>
    respond(
      route,
      postPage(
        new URL(route.request().url()).searchParams.get("keyword")
          ? [removed]
          : [adminPost, removed],
      ),
    ),
  );
  await loginAdmin(page);
  const row = page.getByRole("row").filter({ hasText: removed.title });
  await expect(row).toContainText("삭제됨");
  await expect(
    row.getByRole("button", { name: removed.title + " 검토" }),
  ).toBeEnabled();
  await page.getByLabel("후기 검색", { exact: true }).fill(removed.title);
  await expect(page.getByRole("row")).toHaveCount(2);
  for (const width of [390, 768, 1440]) {
    await page.setViewportSize({ width, height: 1000 });
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true);
    await expect(page.getByLabel("후기 검색", { exact: true })).toBeVisible();
  }
  expect(
    (await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa"]).analyze())
      .violations,
  ).toEqual([]);
});
