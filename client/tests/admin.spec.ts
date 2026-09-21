import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

test("admin uses only its mock store even when the public site is in API mode", async ({
  page,
}) => {
  const requests: string[] = [];
  const errors: string[] = [];
  await page.addInitScript(() => sessionStorage.setItem("eventus.mode", "api"));
  page.on("request", (request) => {
    if (new URL(request.url()).pathname.startsWith("/api/"))
      requests.push(request.url());
  });
  page.on("pageerror", (error) => errors.push(error.message));
  await page.goto("/admin");
  await expect(
    page.getByRole("heading", { name: "운영 대시보드", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByText("모든 데이터와 처리 결과는 Mock입니다."),
  ).toBeVisible();
  await page.getByRole("link", { name: "회원 관리", exact: true }).click();
  await expect(page.getByRole("row")).toHaveCount(9);
  await page.getByRole("button", { name: "산책하는 하루 회원 관리" }).click();
  await page.getByLabel("변경할 등급").selectOption("ROLE_TRUSTED");
  await page.getByLabel("처리 사유").fill("정상적인 제보 이력을 확인했습니다.");
  await page.getByRole("button", { name: "등급 변경", exact: true }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  expect(requests).toEqual([]);
  expect(errors).toEqual([]);
  expect(
    await page.evaluate(() => sessionStorage.getItem("eventus.preview.v1")),
  ).toBeNull();
  expect(
    await page.evaluate(() => sessionStorage.getItem("eventus.mode")),
  ).toBe("api");
});

test("member moderation persists, updates the audit log, and resets explicitly", async ({
  page,
}) => {
  await page.goto("/admin/members");
  await page.getByRole("button", { name: "산책하는 하루 회원 관리" }).click();
  await expect(
    page.getByRole("button", { name: "등급 변경", exact: true }),
  ).toBeDisabled();
  await page.getByLabel("변경할 등급").selectOption("ROLE_WARNING");
  await page.getByLabel("처리 사유").fill("반복된 허위 제보 확인");
  await page.getByRole("button", { name: "등급 변경", exact: true }).click();
  await page.reload();
  await page.getByLabel("회원 등급 필터").selectOption("ROLE_WARNING");
  await expect(
    page.getByRole("row").filter({ hasText: "산책하는 하루" }),
  ).toContainText("주의");
  await page.getByLabel("닉네임, 이메일, 회원 ID 검색").fill("없는회원");
  await expect(
    page.getByRole("heading", { name: "조건에 맞는 결과가 없습니다" }),
  ).toBeVisible();
  await page.getByRole("link", { name: "운영 기록", exact: true }).click();
  await expect(
    page.getByRole("row").filter({ hasText: "산책하는 하루" }),
  ).toContainText("일반 → 주의 · 반복된 허위 제보 확인");
  await page.getByRole("button", { name: "예시 초기화" }).click();
  await page.getByRole("button", { name: "취소", exact: true }).click();
  await expect(
    page.getByRole("row").filter({ hasText: "산책하는 하루" }),
  ).toHaveCount(1);
  await page.getByRole("button", { name: "예시 초기화" }).click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "초기화", exact: true })
    .click();
  await expect(
    page.getByRole("row").filter({ hasText: "산책하는 하루" }),
  ).toHaveCount(0);
  await page.getByRole("link", { name: "회원 관리", exact: true }).click();
  await page
    .getByRole("button", { name: "방구석탈출 운영팀 회원 관리" })
    .click();
  await expect(page.getByRole("dialog")).toContainText(
    "시스템 관리자 계정의 권한은 이 미리보기에서 변경하지 않습니다.",
  );
  await expect(page.getByLabel("변경할 등급")).toHaveCount(0);
});

test("event publication and review restoration update filtered lists and counts", async ({
  page,
}) => {
  await page.goto("/admin/events?status=PENDING");
  await expect(page.getByRole("row")).toHaveCount(3);
  await page
    .getByRole("button", { name: "우리 동네 주말 플리마켓 검토" })
    .click();
  await expect(
    page.getByRole("button", { name: "처리 내용 저장" }),
  ).toBeDisabled();
  await page.getByLabel("처리 사유").fill("장소와 운영 시간 확인 완료");
  await page.getByRole("button", { name: "처리 내용 저장" }).click();
  await expect(page.getByRole("row")).toHaveCount(2);
  await page.getByRole("button", { name: "공개 5", exact: true }).click();
  await expect(
    page.getByRole("row").filter({ hasText: "우리 동네 주말 플리마켓" }),
  ).toContainText("공개");
  await page.goto("/admin/reviews?status=HIDDEN");
  await page
    .getByRole("button", { name: "부적절한 표현이 포함된 후기 검토" })
    .click();
  await page
    .getByLabel("처리 사유")
    .fill("작성자의 수정 내용을 재확인하여 복구");
  await page.getByRole("button", { name: "처리 내용 저장" }).click();
  await expect(
    page.getByRole("heading", { name: "조건에 맞는 결과가 없습니다" }),
  ).toBeVisible();
  await page.goto("/admin/activity");
  await expect(
    page.getByRole("row").filter({ hasText: "부적절한 표현이 포함된 후기" }),
  ).toContainText("공개 처리");
});

test("reported content can be hidden and the separate report answered", async ({
  page,
}) => {
  await page.goto("/admin/inquiries?item=Q-4004");
  await page.getByRole("link", { name: /신고된 콘텐츠/ }).click();
  await expect(page.getByRole("dialog", { name: "후기 검토" })).toBeVisible();
  await page.getByLabel("처리 사유").fill("행사와 무관한 반복 광고 확인");
  await page.getByRole("button", { name: "처리 내용 저장" }).click();
  await expect(
    page.getByRole("row").filter({ hasText: "광고 링크가 반복되는 후기" }),
  ).toContainText("숨김");
  await page.goto("/admin/inquiries?item=Q-4004");
  await expect(page.getByRole("link", { name: /신고된 콘텐츠/ })).toContainText(
    "숨김",
  );
  await page
    .getByLabel("운영팀 답변")
    .fill("광고성 후기를 확인하여 숨김 처리했습니다.");
  await page.getByRole("button", { name: "답변 등록", exact: true }).click();
  await expect(
    page.getByRole("row").filter({ hasText: "Q-4004" }),
  ).toContainText("답변 완료");
  await page.reload();
  await page.getByRole("button", { name: "Q-4004 접수 상세" }).click();
  await expect(page.getByRole("dialog")).toContainText(
    "광고성 후기를 확인하여 숨김 처리했습니다.",
  );
  await expect(
    page.getByRole("button", { name: "답변 등록", exact: true }),
  ).toHaveCount(0);
  await page.keyboard.press("Escape");
  await page.getByRole("link", { name: "운영 대시보드", exact: true }).click();
  await expect(
    page.getByRole("link").filter({ hasText: "미처리 문의·신고" }),
  ).toContainText("2건");
});

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
      .locator(".discovery-search > label")
      .evaluateAll((labels) =>
        labels.map((label) => {
          const text = label.querySelector("span")!;
          const input = label.querySelector("input, select")!;
          return {
            textHeight: text.getBoundingClientRect().height,
            fontSize: Number.parseFloat(getComputedStyle(text).fontSize),
            inputWidth: input.getBoundingClientRect().width,
            textOverflow: text.scrollWidth > text.clientWidth + 1,
          };
        }),
      );
    for (const field of fields) {
      expect(field.textHeight, `label wraps at ${width}px`).toBeLessThanOrEqual(
        field.fontSize * 1.6,
      );
      expect(
        field.inputWidth,
        `field crushed at ${width}px`,
      ).toBeGreaterThanOrEqual(100);
      expect(field.textOverflow, `label overflows at ${width}px`).toBe(false);
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
    path: "docs/screenshots/home-tablet.png",
    fullPage: true,
  });
  await page.getByLabel("찾을 지역").selectOption("GYEONGGI_SUWON");
  await page.getByLabel("행사 날짜").fill("2026-10-10");
  await page.getByLabel("행사 검색어").fill("축제");
  await page.getByRole("button", { name: "행사 찾기", exact: true }).click();
  await expect(page).toHaveURL(/region=GYEONGGI_SUWON&date=2026-10-10&q=/);
});

test("admin pages are accessible and fit desktop, tablet and mobile", async ({
  page,
}) => {
  test.setTimeout(60_000);
  const paths = [
    "/admin",
    "/admin/members",
    "/admin/events",
    "/admin/reviews",
    "/admin/inquiries",
    "/admin/activity",
  ];
  for (const path of paths) {
    await page.goto(path);
    await page.evaluate(() => document.fonts.ready);
    const results = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
      .analyze();
    expect(results.violations, path).toEqual([]);
  }
  await page.goto("/admin/members");
  await page.getByRole("button", { name: "산책하는 하루 회원 관리" }).click();
  expect(
    (
      await new AxeBuilder({ page })
        .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
        .analyze()
    ).violations,
  ).toEqual([]);
  await page.keyboard.press("Escape");
  await page.goto("/admin");
  await page.screenshot({
    path: "docs/screenshots/admin-desktop.png",
    fullPage: true,
  });
  await page.goto("/admin/members");
  await page.screenshot({
    path: "docs/screenshots/admin-members.png",
    fullPage: true,
  });
  for (const width of [320, 390, 768, 1024]) {
    await page.setViewportSize({ width, height: 900 });
    for (const path of paths) {
      await page.goto(path);
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth > innerWidth,
        ),
        `${path} at ${width}px`,
      ).toBe(false);
    }
  }
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/admin");
  await page.screenshot({
    path: "docs/screenshots/admin-mobile.png",
    fullPage: true,
  });
});
