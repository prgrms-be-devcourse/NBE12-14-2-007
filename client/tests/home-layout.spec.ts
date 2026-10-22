import { test, expect } from "@playwright/test";

test("search fields remain readable across the flex regression breakpoints", async ({
  page,
}) => {
  await page.goto("/");
  await page.evaluate(() => document.fonts.ready);
  for (const width of [
    320, 375, 390, 560, 561, 640, 768, 790, 800, 801, 960, 961, 1024, 1194,
    1440,
  ]) {
    await page.setViewportSize({ width, height: 1000 });
    const fields = await page
      .locator(".discovery-search input, .discovery-search select")
      .evaluateAll((labels) =>
        labels.map((input) => {
          return {
            inputWidth: input.getBoundingClientRect().width,
          };
        }),
      );
    expect(fields).toHaveLength(3);
    for (const field of fields) {
      expect(
        field.inputWidth,
        `field crushed at ${width}px`,
      ).toBeGreaterThanOrEqual(100);
    }
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth,
      ),
      `page overflows at ${width}px`,
    ).toBe(false);
  }
  await page.setViewportSize({ width: 790, height: 1000 });
  await page.screenshot({
    path: "test-results/home-tablet.png",
    fullPage: true,
  });
  await page.getByLabel("지역 선택").selectOption("GYEONGGI");
  await page.getByLabel("행사 날짜").fill("2026-10-10");
  await page.getByLabel("행사 검색어").fill("축제");
  await page.getByRole("button", { name: "검색", exact: true }).click();
  await expect(page).toHaveURL(/region=GYEONGGI&date=2026-10-10&q=/);
});
