import type { Page } from "@playwright/test";
import { demoMember } from "../../src/lib/demo";

// 미리보기 콘텐츠와 로그인 상태는 별개다. 쓰기 동작에 필요한 로그인만 재현한다.
export async function previewSession(page: Page) {
  await page.route("**/api/v1/auth/refresh", (route) =>
    route.fulfill({
      json: { success: true, data: { accessToken: "preview-test-token" } },
    }),
  );
  await page.route("**/api/v1/members/me", (route) =>
    route.fulfill({ json: { success: true, data: demoMember } }),
  );
}
