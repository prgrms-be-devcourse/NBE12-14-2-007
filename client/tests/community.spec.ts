import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

test("API mode retrieves the public review feed without private browse APIs", async ({
  page,
}) => {
  const urls: URL[] = [];
  await page.addInitScript(() => sessionStorage.setItem("eventus.mode", "api"));
  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    urls.push(url);
    const path = url.pathname;
    let data: unknown;
    if (path === "/api/v1/auth/refresh")
      data = { accessToken: "mock-session-token" };
    else if (path === "/api/v1/members/me")
      data = {
        id: "me",
        nickname: "내 계정",
        role: "ROLE_UNVERIFIED",
        email: "me@example.com",
      };
    else if (path === "/api/v1/posts")
      data = {
        content: [],
        totalElements: 0,
        totalPages: 0,
        number: 0,
        size: 6,
        first: true,
        last: true,
      };
    else
      return route.fulfill({
        status: 404,
        contentType: "application/json",
        body: JSON.stringify({
          success: false,
          message: `Unexpected request: ${path}`,
        }),
      });
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({ success: true, data }),
    });
  });
  await page.goto("/reviews");
  await expect(
    page.getByRole("heading", { name: "첫 번째 이야기를 기다려요" }),
  ).toBeVisible();
  await expect(page.locator(".post-row")).toHaveCount(0);
  expect(urls.some((url) => url.pathname === "/api/v1/posts")).toBe(true);
  expect(
    urls.filter(
      (url) =>
        ![
          "/api/v1/auth/refresh",
          "/api/v1/members/me",
          "/api/v1/posts",
        ].includes(url.pathname),
    ),
  ).toEqual([]);
});

test("review feed includes multiple events and keeps review and event links separate", async ({
  page,
}) => {
  await page.goto("/reviews");
  await expect(page.getByLabel("후기를 볼 행사")).toHaveCount(0);
  await expect(page.locator(".post-row")).toHaveCount(3);
  await expect(page.getByLabel("후기 정렬")).toHaveValue("likes,desc");
  await expect(page.locator(".post-row h3").first()).toHaveText(
    "걷는 것만으로도 좋았던 저녁",
  );
  await expect(
    page.locator(".post-row").first().getByLabel("좋아요 12개"),
  ).toBeVisible();
  await page.getByLabel("후기 정렬").selectOption("createdAt,desc");
  await expect(page.locator(".post-row h3").first()).toHaveText(
    "가을 색을 가득 담아 왔어요",
  );
  await page.getByLabel("후기 정렬").selectOption("createdAt,asc");
  await expect(page.locator(".post-row h3").first()).toHaveText(
    "음악과 함께 쉬어가는 하루",
  );
  await page
    .locator(".post-row")
    .getByRole("link", { name: "달빛 아래, 수원화성 문화산책", exact: true })
    .click();
  await expect(page).toHaveURL("/events/1001");
  await expect(
    page.getByRole("heading", {
      name: "달빛 아래, 수원화성 문화산책",
      exact: true,
    }),
  ).toBeVisible();
  await page.getByRole("tab", { name: "행사 후기", exact: true }).click();
  await expect(page.getByLabel("후기 정렬")).toHaveValue("likes,desc");
  await expect(page.locator(".post-row")).toHaveCount(1);
  await page
    .getByRole("link", { name: "걷는 것만으로도 좋았던 저녁", exact: true })
    .click();
  await expect(page).toHaveURL("/reviews/demo-post-1");
  await page.locator(".review-event-title").click();
  await expect(page).toHaveURL("/events/1001");
});

test("submission menu opens the registration form", async ({ page }) => {
  await page.goto("/submissions");
  await expect(page).toHaveURL("/submissions/new");
  await expect(
    page.getByRole("heading", {
      name: "당신의 제보가 새로운 탈출의 시작이에요",
    }),
  ).toBeVisible();
});

test("comment timestamps show Seoul hours and minutes for local and UTC data", async ({
  page,
}) => {
  await page.clock.setFixedTime(new Date("2026-09-21T06:24:00Z"));
  await page.goto("/reviews/demo-post-1");
  await expect(page.locator(".comment-row time").first()).toHaveText(
    "2026.09.21 09:00",
  );
  await page
    .getByLabel("댓글 내용", { exact: true })
    .fill("시간 표시를 확인하는 댓글입니다.");
  await page.getByRole("button", { name: "댓글 남기기" }).click();
  const comment = page
    .locator(".comment-row")
    .filter({ hasText: "시간 표시를 확인하는 댓글입니다." });
  await expect(comment.locator("time")).toHaveText("2026.09.21 15:24");
  await expect(page.locator(".comment-row").first()).toContainText(
    "시간 표시를 확인하는 댓글입니다.",
  );
  await page.setViewportSize({ width: 320, height: 900 });
  await expect(
    comment.getByRole("button", { name: "수정", exact: true }),
  ).toBeVisible();
  await expect(
    comment.getByRole("button", { name: "삭제", exact: true }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.screenshot({
    path: "docs/screenshots/comments-time.png",
    fullPage: true,
  });
});

test("active navigation underline stays below the text at desktop and tablet widths", async ({
  page,
}) => {
  for (const width of [561, 790, 1024, 1440]) {
    await page.setViewportSize({ width, height: 1000 });
    for (const [path, label] of [
      ["/", "홈"],
      ["/explore", "지역 문화행사"],
      ["/submissions/new", "행사 제보"],
      ["/reviews", "행사 후기"],
    ]) {
      await page.goto(path);
      await page.evaluate(() => document.fonts.ready);
      const link = page
        .getByRole("navigation", { name: "메인 메뉴", exact: true })
        .getByRole("link", { name: label, exact: true });
      await expect(link).toHaveAttribute("aria-current", "page");
      const gap = await link.evaluate((element) => {
        const range = document.createRange();
        range.selectNodeContents(element);
        const textBottom = range.getBoundingClientRect().bottom;
        const marker = getComputedStyle(element, "::after");
        return (
          element.getBoundingClientRect().bottom -
          parseFloat(marker.bottom) -
          parseFloat(marker.height) -
          textBottom
        );
      });
      expect(gap, `${path} at ${width}px`).toBeGreaterThanOrEqual(8);
    }
  }
});

test("community pages remain accessible and fit small screens", async ({
  page,
}) => {
  for (const [path, name] of [
    ["/reviews", "reviews-feed"],
    ["/submissions/new", "submission-form"],
    ["/events/3001", "community-detail"],
  ]) {
    await page.goto(path);
    await page.evaluate(() => document.fonts.ready);
    const result = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
      .analyze();
    expect(result.violations, path).toEqual([]);
    await page.screenshot({
      path: `docs/screenshots/${name}.png`,
      fullPage: true,
    });
  }
  for (const width of [320, 390, 768]) {
    await page.setViewportSize({ width, height: 900 });
    for (const path of [
      "/reviews",
      "/reviews/new",
      "/submissions/new",
      "/events/3001",
      "/reviews/demo-post-1",
    ]) {
      await page.goto(path);
      await expect(page.locator(".loading")).toHaveCount(0);
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
        `${path} at ${width}px`,
      ).toBe(true);
    }
  }
});
