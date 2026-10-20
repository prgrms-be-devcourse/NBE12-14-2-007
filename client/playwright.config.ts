import { defineConfig } from "@playwright/test";

const port = Number(process.env.PLAYWRIGHT_PORT || 3001);
const baseURL = `http://localhost:${port}`;

export default defineConfig({
  testDir: "./tests",
  timeout: 30_000,
  expect: { timeout: 5_000 },
  fullyParallel: true,
  workers: 2,
  reporter: [["list"]],
  use: {
    baseURL,
    viewport: { width: 1440, height: 1000 },
    locale: "ko-KR",
    timezoneId: "Asia/Seoul",
    screenshot: "only-on-failure",
    trace: "retain-on-failure",
  },
  webServer: {
    command: `corepack pnpm dev --port ${port}`,
    url: baseURL,
    // 로컬 .env가 api여도 미리보기 검증을 재현한다. API 테스트는 sessionStorage로 전환한다.
    env: { VITE_CONTENT_MODE: "preview" },
    reuseExistingServer: false,
    timeout: 30_000,
  },
});
