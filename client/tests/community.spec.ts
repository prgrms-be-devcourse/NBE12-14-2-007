import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

test("live feed uses shared read APIs and does not request private submissions", async ({
  page,
}) => {
  const urls: URL[] = [];
  const event = {
    festivalId: 72,
    providerType: "MEMBER",
    title: "다른 회원의 낭독회",
    category: "체험",
    festivalContent: "공개 행사 소개",
    region: "GYEONGGI_SUWON",
    regionDetail: "수원 책방",
    beginDe: "2026-10-01T12:00:00",
    endDe: "2026-10-01T18:00:00",
    status: "OPEN",
    submitter: { id: "neighbor", nickname: "이웃" },
  };
  const summary = {
    id: "post-72",
    member: { id: "neighbor", nickname: "이웃", profileImg: null },
    festivalId: 72,
    festivalTitle: event.title,
    title: "낭독회에 다녀왔어요",
    thumbnail: null,
    date: "2026-09-21T15:24:00",
  };
  const paged = (content: unknown[]) => ({
    content,
    number: 0,
    size: 6,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    first: true,
    last: true,
  });
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
        role: "ROLE_NORMAL",
        email: "me@example.com",
      };
    else if (path === "/api/v1/posts") data = paged([summary]);
    else if (path === "/api/v1/festivals/submissions")
      data = paged(url.searchParams.get("status") === "CLOSED" ? [] : [event]);
    else if (path === "/api/v1/festivals/72") data = event;
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
    page.getByRole("heading", { name: "낭독회에 다녀왔어요", exact: true }),
  ).toBeVisible();
  await page.getByRole("link", { name: event.title, exact: true }).click();
  await expect(page).toHaveURL("/events/72");
  await expect(page.getByText("공개 행사 소개", { exact: true })).toBeVisible();
  await expect(page.getByRole("link", { name: "제보 수정" })).toHaveCount(0);
  await page.goto("/submissions");
  await expect(
    page.getByRole("heading", { name: event.title, exact: true }),
  ).toBeVisible();
  await page.getByLabel("제보된 행사 검색").fill("낭독회");
  await page.getByLabel("제보 행사 상태 필터").selectOption("CLOSED");
  await expect(
    page.getByRole("heading", { name: "조건에 맞는 제보가 없어요" }),
  ).toBeVisible();
  expect(
    urls.some(
      (url) =>
        url.searchParams.get("q") === "낭독회" &&
        url.searchParams.get("status") === "CLOSED",
    ),
  ).toBe(true);
  expect(
    urls.some((url) =>
      url.pathname.startsWith("/api/v1/members/me/submissions"),
    ),
  ).toBe(false);
});

test("review feed includes multiple events and keeps review and event links separate", async ({
  page,
}) => {
  await page.goto("/reviews");
  await expect(page.getByLabel("후기를 볼 행사")).toHaveCount(0);
  await expect(page.locator(".post-row")).toHaveCount(3);
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
  await expect(page.locator(".post-row")).toHaveCount(1);
  await page
    .getByRole("link", { name: "걷는 것만으로도 좋았던 저녁", exact: true })
    .click();
  await expect(page).toHaveURL("/reviews/demo-post-1");
  await page.locator(".review-event-title").click();
  await expect(page).toHaveURL("/events/1001");
});

test("community submissions include other members while private editing stays in mypage", async ({
  page,
}) => {
  await page.goto("/submissions");
  await expect(
    page.getByRole("heading", { name: /이웃이 전한 행사/ }),
  ).toContainText("4");
  await expect(
    page.getByRole("heading", { name: /내가 제보한 행사/ }),
  ).toHaveCount(0);
  await expect(page.getByText("제보자 · 소소한 여행자")).toBeVisible();
  await page.getByLabel("제보된 행사 검색").fill("낭독회");
  await expect(page.locator(".event-card")).toHaveCount(1);
  await page.getByRole("link", { name: /동네 책방, 가을 낭독회/ }).click();
  await expect(page).toHaveURL("/events/3001");
  await expect(
    page.getByRole("heading", { name: "동네 책방, 가을 낭독회", exact: true }),
  ).toBeVisible();
  await expect(page.getByRole("link", { name: "제보 수정" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "내 제보 삭제" })).toHaveCount(
    0,
  );
  await page.getByRole("tab", { name: "행사 후기", exact: true }).click();
  await page.getByRole("link", { name: "후기 쓰기", exact: true }).click();
  await expect(page).toHaveURL("/reviews/new?festival=3001");
  await page.getByLabel("후기 제목").fill("이웃의 낭독회 방문 후기");
  await page
    .getByLabel("후기 내용")
    .fill("다른 회원이 제보한 행사에도 후기를 남길 수 있어요.");
  await page.getByRole("button", { name: "후기 등록하기" }).click();
  await expect(page.locator(".review-event-title")).toHaveText(
    "동네 책방, 가을 낭독회",
  );
  await page.goto("/mypage?tab=submissions");
  await expect(page.locator(".submission-row")).toHaveCount(2);
  await expect(
    page.getByRole("link", { name: /동네 책방, 가을 낭독회/ }),
  ).toHaveCount(0);
  await page
    .getByRole("link", { name: /이웃과 함께하는 주말 플리마켓/ })
    .click();
  await expect(page.getByRole("link", { name: "제보 수정" })).toBeVisible();
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
      ["/submissions", "행사 제보"],
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
    ["/submissions", "community-submissions"],
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
      "/submissions",
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
