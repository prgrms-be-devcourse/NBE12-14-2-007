import { test, expect, type Route } from "@playwright/test";

const postId = "33333333-3333-4333-8333-333333333333";
const me = {
  id: "member",
  nickname: "나",
  profileImg: null,
  role: "ROLE_UNVERIFIED",
};
const neighbor = { ...me, id: "neighbor", nickname: "이웃" };
const emptyPage = {
  content: [],
  number: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
};

async function respond(route: Route, data: unknown) {
  await route.fulfill({
    contentType: "application/json",
    body: JSON.stringify({ success: true, data, message: "" }),
  });
}

test("review detail shows the like count from the detail response and updates it from the like response", async ({
  page,
}) => {
  const countRequests: string[] = [];
  await page.addInitScript(() => sessionStorage.setItem("eventus.mode", "api"));
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();
    if (url.pathname === "/api/v1/auth/refresh")
      return respond(route, { accessToken: "like-token" });
    if (url.pathname === "/api/v1/members/me")
      return respond(route, { ...me, email: "me@example.com" });
    if (url.pathname === `/api/v1/posts/${postId}`)
      return respond(route, {
        id: postId,
        title: "좋아요 확인용 후기",
        festivalId: 72,
        festivalTitle: "가을 문화축제",
        member: neighbor,
        thumbnail: null,
        content: "<p>본문</p>",
        date: "2026-09-30T10:00:00",
        likeCount: 5,
        likedByMe: false,
      });
    // 지운 개수 조회 API를 다시 부르면 기록해 둔다
    if (url.pathname === `/api/v1/posts/${postId}/likes`) {
      countRequests.push(method);
      return respond(route, 0);
    }
    if (url.pathname === `/api/v1/posts/${postId}/likes/me`)
      return respond(route, { likeCount: method === "POST" ? 6 : 5 });
    return respond(route, emptyPage);
  });

  await page.goto(`/reviews/${postId}`);
  const button = page.getByRole("button", { name: /도움돼요|도움이 됐어요/ });
  await expect(button).toHaveText(/도움돼요\s*5/);

  await button.click();
  await expect(button).toHaveText(/도움이 됐어요\s*6/);
  await expect(button).toHaveAttribute("aria-pressed", "true");

  await button.click();
  await expect(button).toHaveText(/도움돼요\s*5/);
  expect(countRequests).toEqual([]);
});
