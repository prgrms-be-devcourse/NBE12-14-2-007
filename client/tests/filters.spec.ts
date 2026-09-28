import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

test("two-level regions carry from home, restore URLs and reset dependent selections", async ({
  page,
}) => {
  await page.goto("/");
  const province = page.getByLabel("시·도 선택");
  const district = page.getByLabel("시·군·구 선택");
  await expect(district).toBeDisabled();
  await province.selectOption("GYEONGGI");
  await district.selectOption("GYEONGGI_SUWON");
  await page.getByRole("button", { name: "행사 찾기", exact: true }).click();
  await expect(page).toHaveURL(/region=GYEONGGI_SUWON/);
  await expect(province).toHaveValue("GYEONGGI");
  await expect(district).toHaveValue("GYEONGGI_SUWON");
  await expect(page.locator(".event-card")).toHaveCount(1);
  await page.reload();
  await expect(district).toHaveValue("GYEONGGI_SUWON");
  await district.selectOption("");
  await expect(page.locator(".event-card")).toHaveCount(4);
  await province.selectOption("INCHEON");
  await expect(district).toHaveValue("");
  await expect(district.locator("option")).not.toContainText(["수원시"]);
  await district.selectOption({ label: "연수구" });
  await expect(page.getByText("조건에 맞는 행사가 없어요")).toBeVisible();
  await page.reload();
  await expect(province).toHaveValue("INCHEON");
  await expect(district).toHaveValue("INCHEON:연수구");
  await province.selectOption("GANGWON");
  await expect(district).toHaveValue("");
  await district.selectOption({ label: "춘천시" });
  await province.selectOption("SEOUL");
  await expect(district).toHaveValue("");
  await district.selectOption({ label: "강남구" });
  await province.selectOption("SEJONG");
  await expect(district).toBeDisabled();
  await page.goBack();
  await expect(province).toHaveValue("SEOUL");
  await expect(district).toHaveValue("SEOUL:강남구");
  await page.getByRole("button", { name: "필터 초기화" }).click();
  await expect(province).toHaveValue("");
  await expect(district).toBeDisabled();
  await expect(page.locator(".event-card")).toHaveCount(4);
});

test("popular reviews sort before pagination, break ties by date and reflect likes", async ({
  page,
}) => {
  await page.goto("/reviews/demo-post-1");
  await page.getByRole("button", { name: /도움돼요/ }).click();
  await expect(
    page.getByRole("button", { name: /도움이 됐어요/ }),
  ).toBeVisible();
  await page.evaluate(() => {
    const data = JSON.parse(sessionStorage.getItem("eventus.preview.v1")!);
    const sample = data.posts[0];
    data.posts = Array.from({ length: 8 }, (_, i) => ({
      ...sample,
      id: `rank-${i}`,
      title: `정렬 테스트 ${i}`,
      date: `2026-09-${28 - i}T12:00:00`,
    }));
    data.likes = Object.fromEntries(
      [0, 5, 5, 1, 2, 3, 4, 100].map((count, i) => [`rank-${i}`, count]),
    );
    data.liked = [];
    sessionStorage.setItem("eventus.preview.v1", JSON.stringify(data));
  });
  await page.goto("/reviews");
  await expect(page.locator(".post-row h3")).toHaveText(
    [7, 1, 2, 6, 5, 4].map((i) => `정렬 테스트 ${i}`),
  );
  await page.getByRole("button", { name: "다음 페이지" }).click();
  await expect(page.locator(".post-row h3")).toHaveText([
    "정렬 테스트 3",
    "정렬 테스트 0",
  ]);
  await page.getByLabel("후기 정렬").selectOption("createdAt,desc");
  await expect(page.locator(".post-row h3").first()).toHaveText(
    "정렬 테스트 0",
  );
  await page.getByRole("link", { name: "정렬 테스트 2", exact: true }).click();
  await page.getByRole("button", { name: /도움돼요/ }).click();
  await expect(
    page.getByRole("button", { name: /도움이 됐어요/ }),
  ).toContainText("6");
  await page.goto("/reviews");
  await expect(page.locator(".post-row h3")).toHaveText(
    [7, 2, 1, 6, 5, 4].map((i) => `정렬 테스트 ${i}`),
  );
  await page.goto("/");
  await expect(page.locator(".review-teaser h3")).toHaveText(
    [7, 2, 1].map((i) => `정렬 테스트 ${i}`),
  );
});

test("two-level region controls fit small screens and remain accessible", async ({
  page,
}) => {
  for (const width of [320, 390, 768, 1024, 1194, 1440]) {
    await page.setViewportSize({ width, height: 1000 });
    await page.goto("/explore?region=GYEONGGI_SUWON");
    await expect(page.getByLabel("시·군·구 선택")).toHaveValue(
      "GYEONGGI_SUWON",
    );
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
      `overflow at ${width}px`,
    ).toBe(true);
    for (const label of ["시·도 선택", "시·군·구 선택"]) {
      await expect(page.getByLabel(label)).toBeInViewport();
      expect(
        (await page.getByLabel(label).boundingBox())!.width,
      ).toBeGreaterThan(65);
    }
    if ([390, 1440].includes(width)) {
      const result = await new AxeBuilder({ page })
        .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
        .analyze();
      expect(result.violations).toEqual([]);
      await page.screenshot({
        path: `docs/screenshots/regions-${width}.png`,
        fullPage: true,
      });
    }
  }
});
