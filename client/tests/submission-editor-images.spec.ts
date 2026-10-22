import { test, expect, type Page } from "@playwright/test";

const member = {
  id: "11111111-1111-4111-8111-111111111111",
  email: "images@example.com",
  nickname: "제보자",
  role: "ROLE_UNVERIFIED",
  profileImg: null,
};
const submissionId = "22222222-2222-4222-8222-222222222222";
const image = {
  name: "body.png",
  mimeType: "image/png",
  buffer: Buffer.from(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=",
    "base64",
  ),
};
const bodyUrl = "https://images.example.com/festivals/body.png";
const coverUrl = "https://images.example.com/festivals/cover.png";
const ok = (data: unknown) => ({
  success: true,
  code: "0000",
  message: "",
  data,
});
const emptyPage = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  number: 0,
  size: 6,
  first: true,
  last: true,
};

async function setup(page: Page, uploadFailure = false) {
  let payload: Record<string, unknown> | undefined;
  const uploadTypes: string[] = [];
  let festival = {
    festivalId: 42,
    title: "본문 이미지 검증 행사",
    category: "축제",
    festivalContent: "행사 소개",
    referenceUrl: "https://example.com/event",
    imgUrl: "",
    region: "GYEONGGI",
    beginDe: "2026-10-01T00:00:00",
    endDe: "2026-10-03T23:59:59",
    status: "OPEN",
  };
  const detail = () => ({
    submission: {
      festivalSubmissionId: submissionId,
      createdAt: "2026-10-01T10:00:00",
      updatedAt: "2026-10-01T10:00:00",
      festival,
    },
  });
  await page.addInitScript(() => sessionStorage.setItem("eventus.mode", "api"));
  await page.route("https://images.example.com/**", (route) =>
    route.fulfill({ contentType: "image/png", body: image.buffer }),
  );
  await page.route("**/api/v1/**", async (route) => {
    const path = new URL(route.request().url()).pathname.replace("/api/v1", "");
    if (path === "/auth/refresh")
      return route.fulfill({ json: ok({ accessToken: "image-test-token" }) });
    if (path === "/members/me") return route.fulfill({ json: ok(member) });
    if (path === "/images") {
      const form = route.request().postDataBuffer()!.toString();
      expect(form).toMatch(/name="type"\r?\n\r?\nFESTIVAL/);
      uploadTypes.push("FESTIVAL");
      if (uploadFailure)
        return route.fulfill({
          status: 500,
          json: { success: false, message: "이미지를 업로드하지 못했습니다." },
        });
      const url = uploadTypes.length === 1 ? bodyUrl : coverUrl;
      return route.fulfill({
        json: ok({ key: url.replace("https://images.example.com/", ""), url }),
      });
    }
    if (
      path === "/festivals/submissions" ||
      (path === `/members/me/submissions/${submissionId}` &&
        route.request().method() === "PATCH")
    ) {
      payload = route.request().postDataJSON();
      festival = { ...festival, ...payload };
      return route.fulfill({
        json: ok({
          ...detail(),
          festivalId: 42,
          festivalSubmissionId: submissionId,
        }),
      });
    }
    if (path === `/members/me/submissions/${submissionId}`)
      return route.fulfill({ json: ok(detail()) });
    if (path === "/members/me/submissions")
      return route.fulfill({
        json: ok([{ ...festival, festivalSubmissionId: submissionId }]),
      });
    if (path === "/festivals" || path === "/festivals/42/posts")
      return route.fulfill({ json: ok(emptyPage) });
    return route.fulfill({
      status: 404,
      json: { success: false, message: `예상하지 않은 요청: ${path}` },
    });
  });
  return { payload: () => payload, uploadTypes };
}

test("submission body images and the separate cover survive creation and editing", async ({
  page,
}) => {
  const server = await setup(page);
  await page.goto("/submissions/new");
  const editor = page.getByRole("textbox", { name: "행사 소개", exact: true });
  await editor.fill("행사 소개와 본문 사진");
  const imageButton = page
    .getByRole("toolbar")
    .getByRole("button", { name: "이미지", exact: true });
  await expect(imageButton).toBeVisible();
  const chooser = page.waitForEvent("filechooser");
  await imageButton.click();
  await (await chooser).setFiles(image);
  await expect(editor.locator("img")).toHaveAttribute("src", bodyUrl);
  await page
    .locator(".upload-area input[type=file]")
    .setInputFiles({ ...image, name: "cover.png" });
  await expect(
    page.getByRole("img", { name: "업로드한 이미지", exact: true }),
  ).toHaveAttribute("src", coverUrl);
  await page.getByLabel("행사 이름").fill("본문 이미지 검증 행사");
  await page.getByRole("button", { name: "축제", exact: true }).click();
  await page.getByLabel("시작일").fill("2026-10-01");
  await page.getByLabel("종료일").fill("2026-10-03");
  await page.getByLabel("시·도 선택").selectOption("GYEONGGI");
  await page.getByLabel("행사 참고 링크").fill("https://example.com/event");
  await page.getByRole("checkbox").check();
  await page
    .getByRole("button", { name: "행사 제보하기", exact: true })
    .click();
  await expect(page).toHaveURL(`/submissions/${submissionId}`);
  expect(server.payload()?.festivalContent).toContain(`<img src="${bodyUrl}">`);
  expect(server.payload()?.imgUrl).toBe(coverUrl);
  await expect(page.locator(".rich-text-content img")).toHaveAttribute(
    "src",
    bodyUrl,
  );

  await page.getByRole("button", { name: "제보 수정", exact: true }).click();
  await expect(editor.locator("img")).toHaveAttribute("src", bodyUrl);
  await expect(
    page.getByRole("img", { name: "업로드한 이미지", exact: true }),
  ).toHaveAttribute("src", coverUrl);
  await page.getByLabel("행사 이름").fill("사진을 유지한 수정 행사");
  await page.getByRole("checkbox").check();
  await page
    .getByRole("button", { name: "수정한 내용 저장", exact: true })
    .click();
  await expect(page).toHaveURL(`/submissions/${submissionId}`);
  expect(server.payload()?.festivalContent).toContain(bodyUrl);
  expect(server.payload()?.imgUrl).toBe(coverUrl);
  expect(server.uploadTypes).toEqual(["FESTIVAL", "FESTIVAL"]);
});

for (const scenario of [
  {
    name: "upload failure",
    uploadFailure: true,
    file: image,
    message: "이미지를 업로드하지 못했습니다.",
    requests: 1,
  },
  {
    name: "unsupported file type",
    uploadFailure: false,
    file: { ...image, name: "unsupported.gif", mimeType: "image/gif" },
    message: "JPG, PNG, WEBP 이미지만 올릴 수 있어요.",
    requests: 0,
  },
]) {
  test(`submission editor preserves text and permits retry after ${scenario.name}`, async ({
    page,
  }) => {
    const server = await setup(page, scenario.uploadFailure);
    await page.goto("/submissions/new");
    const editor = page.getByRole("textbox", {
      name: "행사 소개",
      exact: true,
    });
    await editor.fill("업로드가 실패해도 유지할 소개");
    const imageButton = page
      .getByRole("toolbar")
      .getByRole("button", { name: "이미지", exact: true });
    const chooser = page.waitForEvent("filechooser");
    await imageButton.click();
    await (await chooser).setFiles(scenario.file);
    await expect(
      page.locator(".rich-text-editor").getByRole("alert"),
    ).toContainText(scenario.message);
    await expect(editor).toContainText("업로드가 실패해도 유지할 소개");
    await expect(editor.locator("img")).toHaveCount(0);
    await expect(imageButton).toBeEnabled();
    expect(server.uploadTypes).toHaveLength(scenario.requests);
  });
}
