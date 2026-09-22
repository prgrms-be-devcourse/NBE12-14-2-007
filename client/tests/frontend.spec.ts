import { test, expect, type Page, type Route } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

const member = {
  id: "11111111-1111-4111-8111-111111111111",
  email: "contract@example.com",
  nickname: "계약 검증 회원",
  profileImg: null,
  phone: null,
  role: "ROLE_NORMAL",
  createdAt: "2026-09-01T10:00:00",
  updatedAt: "2026-09-01T10:00:00",
};
const submissionId = "22222222-2222-4222-8222-222222222222";
const festival = {
  festivalId: 42,
  title: "API로 받은 행사",
  category: "축제",
  instNm: "문화재단",
  manager: "담당자",
  festivalContent: "실제 응답 구조를 사용하는 테스트 행사",
  referenceUrl: "https://example.com/event",
  region: "GYEONGGI_SUWON",
  regionDetail: "효원로 1",
  imgUrl: null,
  beginDe: "2026-10-01T10:00:00",
  endDe: "2026-10-03T18:00:00",
  eventTmInfo: "10:00~18:00",
  partcptExpnInfo: "무료",
  telnoInfo: null,
  hostInstNm: "주최기관",
  writngDe: null,
  status: "OPEN",
};
const summary = {
  ...festival,
  festivalSubmissionId: submissionId,
  createdAt: "2026-09-21T10:00:00",
};
const detail = {
  submission: {
    festivalSubmissionId: submissionId,
    createdAt: "2026-09-21T10:00:00",
    updatedAt: "2026-09-21T10:00:00",
    festival,
  },
};
const ok = (data: unknown) => ({
  success: true,
  code: "0000",
  message: "",
  data,
});
async function json(route: Route, data: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(data),
  });
}
async function apiMode(
  page: Page,
  handler?: (route: Route, path: string, method: string) => Promise<boolean>,
) {
  await page.addInitScript(() => sessionStorage.setItem("eventus.mode", "api"));
  await page.route("**/api/v1/**", async (route) => {
    const path = new URL(route.request().url()).pathname.replace("/api/v1", "");
    const method = route.request().method();
    if (handler && (await handler(route, path, method))) return;
    if (path === "/auth/refresh")
      return json(
        route,
        ok({ accessToken: "contract-token", tokenType: "Bearer" }),
      );
    if (path === "/members/me") return json(route, ok(member));
    if (path === "/members/me/submissions") return json(route, ok([summary]));
    if (path === `/members/me/submissions/${submissionId}`)
      return json(route, ok(detail));
    if (path === "/festivals/42/posts")
      return json(
        route,
        ok({
          content: [],
          totalElements: 0,
          totalPages: 0,
          number: 0,
          size: 6,
          last: true,
          first: true,
        }),
      );
    return json(
      route,
      {
        success: false,
        code: "NOT_MOCKED",
        message: `Unexpected request: ${method} ${path}`,
        data: null,
      },
      404,
    );
  });
}

test("preview stays isolated and all home images render", async ({ page }) => {
  const requests: string[] = [];
  const errors: string[] = [];
  page.on("request", (r) => {
    if (r.url().includes("/api/")) requests.push(r.url());
  });
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: "이번 주말, 어디로 떠나볼까요?" }),
  ).toBeVisible();
  await expect(page.locator(".home-grid .event-card")).toHaveCount(4);
  await page.evaluate(async () => {
    await document.fonts.ready;
    await Promise.all(
      [...document.images].map((img) => img.decode().catch(() => {})),
    );
  });
  expect(
    await page
      .locator("img")
      .evaluateAll((images) =>
        images.every((img) => (img as HTMLImageElement).naturalWidth > 0),
      ),
  ).toBeTruthy();
  expect(requests).toEqual([]);
  expect(errors).toEqual([]);
  await page.screenshot({
    path: "test-results/home-desktop.png",
    fullPage: true,
  });
});

test("search, category, region, dates and URL navigation work", async ({
  page,
}) => {
  await page.goto("/explore");
  await page.getByRole("button", { name: "공연", exact: true }).click();
  await expect(page.locator(".event-card")).toHaveCount(1);
  await expect(page.locator(".event-card")).toContainText("음악회");
  await page.getByLabel("시·도 선택").selectOption("GYEONGGI");
  await page.getByLabel("시·군·구 선택").selectOption("GYEONGGI_SUWON");
  await expect(page.getByText("조건에 맞는 행사가 없어요")).toBeVisible();
  await page.getByRole("button", { name: "필터 초기화" }).click();
  await page.getByLabel("행사 검색", { exact: true }).fill("가을빛");
  await page.getByRole("button", { name: "검색", exact: true }).click();
  await expect(page.locator(".event-card")).toHaveCount(1);
  await expect(page).toHaveURL(/q=/);
  await page.reload();
  await expect(page.getByLabel("행사 검색", { exact: true })).toHaveValue(
    "가을빛",
  );
  await page.getByRole("button", { name: "필터 초기화" }).click();
  await page.getByLabel("날짜 필터").fill("2030-01-01");
  await expect(page.getByText("조건에 맞는 행사가 없어요")).toBeVisible();
});

test("preview submission can be created, edited and deleted", async ({
  page,
}) => {
  let saved = structuredClone(detail);
  await apiMode(page, async (route, path, method) => {
    if (path === "/festivals/submissions" && method === "POST") {
      const input = route.request().postDataJSON();
      saved.submission.festival.title = input.name;
      saved.submission.festival.category = input.category;
      saved.submission.festival.referenceUrl = input.referenceUrl;
      saved.submission.festival.region = input.region;
      saved.submission.festival.beginDe = input.beginDe;
      saved.submission.festival.endDe = input.endDe;
      await json(
        route,
        ok({ festivalId: 42, festivalSubmissionId: submissionId }),
        201,
      );
      return true;
    }
    if (path === `/members/me/submissions/${submissionId}`) {
      if (method === "GET") {
        await json(route, ok(saved));
        return true;
      }
      if (method === "PATCH") {
        const input = route.request().postDataJSON();
        saved.submission.festival.title = input.name;
        await json(route, ok(saved));
        return true;
      }
      if (method === "DELETE") {
        await route.fulfill({ status: 204 });
        return true;
      }
    }
    return false;
  });
  await page.goto("/submissions/new");
  await page.getByLabel("행사 이름").fill("브라우저 검증 행사");
  await page.getByRole("button", { name: "축제", exact: true }).click();
  await page.getByLabel(/^행사 소개/).fill("제보 작성 흐름 검증");
  await page.getByLabel("시작일").fill("2026-10-10");
  await page.getByLabel("종료일").fill("2026-10-10");
  await page
    .getByLabel("지역", { exact: false })
    .selectOption("GYEONGGI_SUWON");
  await page.getByLabel("운영 시간 안내 (선택)").fill("10시부터 18시");
  await page.getByLabel("상세 주소 (선택)").fill("테스트 공원");
  await page.getByLabel("행사 참고 링크").fill("https://example.com/event");
  await page.getByRole("checkbox").check();
  await page
    .getByRole("button", { name: "행사 제보하기", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "브라우저 검증 행사", exact: true }),
  ).toBeVisible();
  await page.getByRole("link", { name: "제보 수정" }).click();
  await page.getByLabel("행사 이름").fill("수정한 검증 행사");
  await page.getByRole("checkbox").check();
  await page.getByRole("button", { name: "수정한 내용 저장" }).click();
  await expect(
    page.getByRole("heading", { name: "수정한 검증 행사", exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "내 제보 삭제" }).click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "제보 삭제", exact: true })
    .click();
  await expect(page).toHaveURL("/mypage?tab=submissions");
  await expect(page.locator(".submission-list")).not.toContainText(
    "수정한 검증 행사",
  );
});

test("preview review, comment and like lifecycle works", async ({ page }) => {
  await page.goto("/reviews/new?festival=1001");
  await page.getByLabel("후기 제목").fill("검증용 산책 후기");
  await page.getByLabel("후기 내용").fill("테스트로 작성한 후기입니다.");
  await page.getByRole("button", { name: "후기 등록하기" }).click();
  await expect(
    page.getByRole("heading", { name: "검증용 산책 후기", exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: /도움돼요/ }).click();
  await expect(
    page.getByRole("button", { name: /도움이 됐어요/ }),
  ).toContainText("1");
  await page.getByRole("button", { name: /도움이 됐어요/ }).click();
  await expect(page.getByRole("button", { name: /도움돼요/ })).toContainText(
    "0",
  );
  await page.getByLabel("댓글 내용").fill("직접 확인한 댓글");
  await page.getByRole("button", { name: "댓글 남기기" }).click();
  await expect(page.locator(".comment-row")).toContainText("직접 확인한 댓글");
  await page
    .locator(".comment-row")
    .getByRole("button", { name: "수정", exact: true })
    .click();
  await page.getByLabel("수정할 댓글").fill("수정한 댓글");
  await page
    .locator(".comment-row")
    .getByRole("button", { name: "저장", exact: true })
    .click();
  await expect(page.locator(".comment-row")).toContainText("수정한 댓글");
  await page
    .locator(".comment-row")
    .getByRole("button", { name: "삭제", exact: true })
    .click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "삭제", exact: true })
    .click();
  await expect(page.getByText("첫 번째 댓글을 남겨보세요.")).toBeVisible();
  await page.getByRole("link", { name: "수정", exact: true }).click();
  await page.getByLabel("후기 제목").fill("수정한 산책 후기");
  await page.getByRole("button", { name: "수정 내용 저장" }).click();
  await expect(
    page.getByRole("heading", { name: "수정한 산책 후기" }),
  ).toBeVisible();
  await page
    .locator(".article-actions")
    .getByRole("button", { name: "삭제", exact: true })
    .click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "삭제하기" })
    .click();
  await expect(page).toHaveURL(/\/reviews\?festival=1001/);
});

test("live mode keeps private submissions in mypage and loads the festival catalog", async ({
  page,
}) => {
  const requested: string[] = [];
  await apiMode(page, async (route, path) => {
    requested.push(path);
    if (path === "/festivals") {
      await json(
        route,
        ok({
          content: [
            {
              festivalId: festival.festivalId,
              providerType: "MEMBER",
              title: festival.title,
              category: festival.category,
              instNm: festival.instNm,
              imgUrl: festival.imgUrl,
              beginDe: festival.beginDe,
              endDe: festival.endDe,
              region: festival.region,
              status: festival.status,
            },
          ],
          totalElements: 1,
          totalPages: 1,
          number: 0,
          size: 9,
          last: true,
          first: true,
        }),
      );
      return true;
    }
    return false;
  });
  await page.goto("/mypage?tab=submissions");
  await expect(
    page.getByRole("heading", { name: "API로 받은 행사", exact: true }),
  ).toBeVisible();
  await page.getByRole("link", { name: /API로 받은 행사/ }).click();
  await expect(
    page.getByRole("heading", { name: "API로 받은 행사", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByText("실제 응답 구조를 사용하는 테스트 행사"),
  ).toBeVisible();
  await page.goto("/explore");
  await expect(page.locator(".event-card")).toHaveCount(1);
  await expect(
    page.getByText("API로 받은 행사", { exact: true }),
  ).toBeVisible();
  expect(requested).toContain("/festivals");
});

test("live festival search displays nine events per page", async ({ page }) => {
  const festivals = Array.from({ length: 10 }, (_, index) => ({
    festivalId: index + 1,
    providerType: index % 2 === 0 ? "PUBLIC" : "MEMBER",
    title: `페이지 행사 ${index + 1}`,
    category: "축제",
    instNm: "문화재단",
    imgUrl: null,
    beginDe: `2026-10-${String(index + 1).padStart(2, "0")}T10:00:00`,
    endDe: `2026-10-${String(index + 1).padStart(2, "0")}T18:00:00`,
    region: "GYEONGGI_SUWON",
    status: "OPEN",
  }));

  await apiMode(page, async (route, path) => {
    if (path !== "/festivals") return false;

    const searchParams = new URL(route.request().url()).searchParams;
    const requestedPage = Number(searchParams.get("page") || 0);
    const requestedSize = Number(searchParams.get("size") || 0);
    expect(requestedSize).toBe(9);

    await json(
      route,
      ok({
        content: festivals.slice(
          requestedPage * requestedSize,
          (requestedPage + 1) * requestedSize,
        ),
        totalElements: festivals.length,
        totalPages: 2,
        number: requestedPage,
        size: requestedSize,
        first: requestedPage === 0,
        last: requestedPage === 1,
      }),
    );
    return true;
  });

  await page.goto("/explore");
  await expect(page.locator(".event-card")).toHaveCount(9);
  await page.getByRole("button", { name: "다음 페이지" }).click();
  await expect(page).toHaveURL(/page=1/);
  await expect(page.locator(".event-card")).toHaveCount(1);
});

test("live festival reviews only send supported date sort fields", async ({
  page,
}) => {
  const sorts: string[] = [];
  await apiMode(page, async (route, path) => {
    if (path === "/festivals/42/posts")
      sorts.push(new URL(route.request().url()).searchParams.get("sort") || "");
    return false;
  });
  await page.goto(`/submissions/${submissionId}`);
  await page.getByRole("tab", { name: "행사 후기", exact: true }).click();
  await expect(page.getByLabel("후기 정렬")).toHaveValue("createdAt,desc");
  await expect(
    page.getByLabel("후기 정렬").locator('option[value="likes,desc"]'),
  ).toBeDisabled();
  await expect(
    page.getByText("좋아요순 정렬은 준비 중이에요.", { exact: false }),
  ).toBeVisible();
  await expect(page.getByText("이 행사의 첫 이야기를 기다려요")).toBeVisible();
  await page.getByLabel("후기 정렬").selectOption("createdAt,asc");
  await expect.poll(() => sorts.includes("createdAt,asc")).toBe(true);
  expect(
    sorts.every((sort) => ["createdAt,desc", "createdAt,asc"].includes(sort)),
  ).toBe(true);
});

test("live form sends actual DTO names and ISO local datetimes", async ({
  page,
}) => {
  let payload: Record<string, unknown> | null = null;
  await apiMode(page, async (route, path, method) => {
    if (
      path === `/members/me/submissions/${submissionId}` &&
      method === "PATCH"
    ) {
      payload = route.request().postDataJSON();
      expect(route.request().headers().authorization).toBe(
        "Bearer contract-token",
      );
      await json(route, ok(detail));
      return true;
    }
    return false;
  });
  await page.goto(`/submissions/${submissionId}/edit`);
  await page.getByLabel("행사 이름").fill("수정한 API 행사");
  await page.getByRole("checkbox").check();
  await page.getByRole("button", { name: "수정한 내용 저장" }).click();
  await expect(page).toHaveURL(`/submissions/${submissionId}`);
  expect(payload).toMatchObject({
    name: "수정한 API 행사",
    festivalContent: festival.festivalContent,
    referenceUrl: "https://example.com/event",
    region: "GYEONGGI",
    beginDe: "2026-10-01T00:00:00",
    endDe: "2026-10-03T23:59:59",
  });
  expect(payload).not.toHaveProperty("manager");
  expect(payload).not.toHaveProperty("providerType");
  expect(payload).not.toHaveProperty("memberId");
});

test("submission accepts required fields only and sends optional fields as null", async ({
  page,
}) => {
  let payload: Record<string, unknown> | undefined;
  await apiMode(page, async (route, path, method) => {
    if (path === "/festivals/submissions" && method === "POST") {
      payload = route.request().postDataJSON();
      await json(
        route,
        ok({ festivalId: 42, festivalSubmissionId: submissionId }),
        201,
      );
      return true;
    }
    return false;
  });
  await page.goto("/submissions/new");
  await page.getByLabel("행사 이름").fill("   ");
  await page.getByRole("button", { name: "기타", exact: true }).click();
  await page.getByLabel("시작일").fill("2026-10-10");
  await page.getByLabel("종료일").fill("2026-10-10");
  await page.getByLabel("지역", { exact: false }).selectOption("GYEONGGI");
  await page.getByLabel("행사 참고 링크").fill("https://example.com/event");
  await page.getByRole("checkbox").check();
  await page
    .getByRole("button", { name: "행사 제보하기", exact: true })
    .click();
  await expect(page.getByText("행사 이름 항목을 입력해 주세요.")).toBeVisible();
  expect(payload).toBeUndefined();
  await page.getByLabel("행사 이름").fill(" 동네 축제 ");
  await page
    .getByRole("button", { name: "행사 제보하기", exact: true })
    .click();
  await expect(page).toHaveURL(`/submissions/${submissionId}`);
  expect(payload).toMatchObject({
    name: "동네 축제",
    category: "기타",
    region: "GYEONGGI",
    beginDe: "2026-10-10T00:00:00",
    endDe: "2026-10-10T23:59:59",
    instNm: null,
    festivalContent: null,
    regionDetail: null,
    eventTmInfo: null,
    referenceUrl: "https://example.com/event",
    imgUrl: null,
    partcptExpnInfo: null,
    telnoInfo: null,
    hostInstNm: null,
  });
});

test("server failure stays an error, with no demo fallback", async ({
  page,
}) => {
  await apiMode(page, async (route, path) => {
    if (path === "/members/me/submissions") {
      await json(
        route,
        {
          success: false,
          code: "SERVER",
          message: "연동 검증 서버 오류",
          data: null,
        },
        500,
      );
      return true;
    }
    return false;
  });
  await page.goto("/mypage?tab=submissions");
  await expect(page.getByRole("alert")).toContainText("연동 검증 서버 오류");
  await expect(page.locator(".submission-row")).toHaveCount(0);
  await expect(
    page.getByText("이웃과 함께하는 주말 플리마켓", { exact: true }),
  ).toHaveCount(0);
});

test("401 renews access token and retries the authorized request", async ({
  page,
}) => {
  let refreshes = 0;
  let attempts = 0;
  await apiMode(page, async (route, path) => {
    if (path === "/auth/refresh") {
      refreshes++;
      await json(route, ok({ accessToken: `contract-token-${refreshes}` }));
      return true;
    }
    if (path === "/members/me/submissions") {
      attempts++;
      if (route.request().headers().authorization === "Bearer contract-token-1")
        await json(
          route,
          { success: false, code: "EXPIRED", message: "Expired", data: null },
          401,
        );
      else await json(route, ok([summary]));
      return true;
    }
    return false;
  });
  await page.goto("/mypage?tab=submissions");
  await expect(
    page.getByRole("heading", { name: "API로 받은 행사", exact: true }),
  ).toBeVisible();
  expect(refreshes).toBe(2);
  expect(attempts).toBeGreaterThanOrEqual(2);
});

test("login sends exact credentials and does not persist access tokens", async ({
  page,
}) => {
  let loggedIn = false;
  let credentials: unknown;
  await apiMode(page, async (route, path, method) => {
    if (path === "/auth/refresh" && !loggedIn) {
      await json(
        route,
        {
          success: false,
          code: "NO_SESSION",
          message: "로그인 필요",
          data: null,
        },
        401,
      );
      return true;
    }
    if (path === "/auth/login" && method === "POST") {
      credentials = route.request().postDataJSON();
      loggedIn = true;
      await json(
        route,
        ok({ accessToken: "private-access-token", tokenType: "Bearer" }),
      );
      return true;
    }
    return false;
  });
  await page.goto("/login");
  await page.getByLabel("이메일").fill("contract@example.com");
  await page
    .getByLabel("비밀번호", { exact: false })
    .first()
    .fill("Sample123!");
  await page.getByRole("button", { name: "로그인", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "마이페이지", exact: true }),
  ).toBeVisible();
  expect(credentials).toEqual({
    email: "contract@example.com",
    password: "Sample123!",
  });
  expect(
    await page.evaluate(() =>
      JSON.stringify({ ...localStorage, ...sessionStorage }),
    ),
  ).not.toContain("private-access-token");
});

test("restricted members cannot enter the submission editor", async ({
  page,
}) => {
  await apiMode(page, async (route, path) => {
    if (path === "/members/me") {
      await json(route, ok({ ...member, role: "ROLE_WARNING" }));
      return true;
    }
    return false;
  });
  await page.goto("/submissions/new");
  await expect(
    page.getByText("현재 행사 제보가 제한되어 있어요"),
  ).toBeVisible();
  await expect(page.getByLabel("행사 이름")).toHaveCount(0);
});

test("inquiries use QUESTION/REPORT and answered inquiries cannot be edited", async ({
  page,
}) => {
  let input: unknown;
  const answered = {
    id: "inquiry-1",
    category: "QUESTION",
    title: "답변된 문의",
    content: "문의 내용",
    img: null,
    status: "ANSWERED",
    answer: "안내 답변입니다.",
    createdAt: "2026-09-20T09:00:00",
  };
  await apiMode(page, async (route, path, method) => {
    if (path === "/inquiries/me") {
      await json(route, ok([answered]));
      return true;
    }
    if (path === "/inquiries/inquiry-1") {
      await json(route, ok(answered));
      return true;
    }
    if (path === "/inquiries" && method === "POST") {
      input = route.request().postDataJSON();
      await json(
        route,
        ok({
          ...answered,
          ...(input as object),
          status: "PENDING",
          answer: null,
        }),
        201,
      );
      return true;
    }
    return false;
  });
  await page.goto("/mypage?tab=inquiries");
  await page.getByRole("button", { name: /답변된 문의/ }).click();
  await expect(page.getByRole("dialog")).toContainText("안내 답변입니다.");
  await expect(
    page.getByRole("dialog").getByRole("button", { name: "수정하기" }),
  ).toHaveCount(0);
  await page.getByRole("button", { name: "닫기", exact: true }).click();
  await page.getByRole("button", { name: "문의하기", exact: true }).click();
  await page.getByLabel("문의 종류").selectOption("REPORT");
  await page.getByLabel("제목", { exact: false }).fill("신고 제목");
  await page.getByLabel("내용", { exact: false }).fill("확인할 내용");
  await page.getByRole("button", { name: "문의 남기기" }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  expect(input).toEqual({
    category: "REPORT",
    title: "신고 제목",
    content: "확인할 내용",
  });
});

test("mobile pages have no horizontal overflow and navigation works", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  for (const path of [
    "/",
    "/explore",
    "/events/1001",
    "/submissions/new",
    "/reviews",
    "/reviews/demo-post-1",
    "/mypage",
    "/login",
  ]) {
    await page.goto(path);
    await page.waitForLoadState("networkidle");
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
      path,
    ).toBeTruthy();
    if (path === "/")
      await page.screenshot({
        path: "test-results/home-mobile.png",
        fullPage: true,
      });
    if (path === "/events/1001")
      await page.screenshot({
        path: "test-results/detail-mobile.png",
        fullPage: true,
      });
  }
  await page.getByRole("button", { name: "메뉴 열기" }).click();
  await page
    .getByRole("navigation", { name: "메인 메뉴" })
    .getByRole("link", { name: "지역 문화행사" })
    .click();
  await expect(page).toHaveURL("/explore");
});

test("desktop detail, submissions and profile render cleanly", async ({
  page,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  for (const [path, name] of [
    ["/events/1001", "detail"],
    ["/submissions/new", "submission-form"],
    ["/mypage", "mypage"],
  ]) {
    await page.goto(path);
    await page.waitForLoadState("networkidle");
    await page.screenshot({
      path: `test-results/${name}-desktop.png`,
      fullPage: true,
    });
  }
  expect(errors).toEqual([]);
});

test("essential pages meet automated accessibility checks", async ({
  page,
}, testInfo) => {
  for (const path of [
    "/",
    "/submissions/new",
    "/mypage",
    "/login",
    "/events/1001",
  ]) {
    await page.goto(path);
    await page.waitForLoadState("networkidle");
    const result = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
      .analyze();
    if (result.violations.length)
      await testInfo.attach(`accessibility-${path.replaceAll("/", "_")}`, {
        body: JSON.stringify(result.violations, null, 2),
        contentType: "application/json",
      });
    expect(
      result.violations.map((v) => ({
        id: v.id,
        nodes: v.nodes.map((n) => ({
          target: n.target,
          summary: n.failureSummary,
        })),
      })),
      path,
    ).toEqual([]);
  }
});

test("compact phone and tablet layouts keep controls within the viewport", async ({
  page,
}) => {
  for (const width of [320, 768]) {
    await page.setViewportSize({ width, height: 900 });
    for (const path of [
      "/",
      "/explore",
      "/submissions/new",
      "/mypage",
      "/events/1001",
    ]) {
      await page.goto(path);
      await page.waitForLoadState("networkidle");
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= window.innerWidth,
        ),
        `${width}px ${path}`,
      ).toBeTruthy();
    }
  }
});
