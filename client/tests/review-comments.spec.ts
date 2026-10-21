import { test, expect, type Page, type Route } from "@playwright/test";

const postId = "22222222-2222-4222-8222-222222222222";
const me = {
  id: "member",
  nickname: "나",
  profileImg: null,
  role: "ROLE_UNVERIFIED",
};
const neighbor = {
  id: "neighbor",
  nickname: "이웃",
  profileImg: null,
  role: "ROLE_UNVERIFIED",
};

interface StoredComment {
  id: number;
  parentId: number | null;
  deleted: boolean;
  member: typeof me;
  content: string;
  date: string;
}

async function respond(route: Route, data: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify({
      success: status < 400,
      data: status < 400 ? data : null,
      message: status < 400 ? "" : data,
    }),
  });
}

/** 서버의 댓글 목록 응답을 흉내 낸다: 일반 댓글 기준 오래된 순 페이지, 대댓글은 replies, 삭제된 댓글은 문구만. */
function commentPage(store: StoredComment[], page: number, size: number) {
  const view = (c: StoredComment) => ({
    id: c.id,
    postId,
    parentId: c.parentId,
    deleted: c.deleted,
    member: c.deleted ? null : c.member,
    content: c.deleted ? "삭제된 댓글입니다" : c.content,
    date: c.date,
    replies: [] as unknown[],
  });
  const roots = store.filter((c) => c.parentId === null);
  const content = roots.slice(page * size, (page + 1) * size).map((root) => ({
    ...view(root),
    replies: store.filter((c) => c.parentId === root.id).map(view),
  }));
  return {
    content,
    number: page,
    size,
    totalElements: roots.length,
    totalPages: Math.ceil(roots.length / size),
    first: page === 0,
    last: (page + 1) * size >= roots.length,
  };
}

async function setup(page: Page) {
  // 이미 삭제된 부모 댓글(답글 있음), 내가 쓴 댓글 순서로 오래된 것부터 저장돼 있다.
  const store: StoredComment[] = [
    {
      id: 1,
      parentId: null,
      deleted: true,
      member: neighbor,
      content: "지워진 원래 내용",
      date: "2026-10-01T10:00:00",
    },
    {
      id: 2,
      parentId: 1,
      deleted: false,
      member: neighbor,
      content: "지워진 댓글에 남은 답글",
      date: "2026-10-01T11:00:00",
    },
    {
      id: 3,
      parentId: null,
      deleted: false,
      member: me,
      content: "내가 쓴 댓글",
      date: "2026-10-02T09:00:00",
    },
  ];
  const created: unknown[] = [];
  const listQueries: URLSearchParams[] = [];
  await page.addInitScript(() => sessionStorage.setItem("eventus.mode", "api"));
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();
    if (url.pathname === "/api/v1/auth/refresh")
      return respond(route, { accessToken: "comment-token" });
    if (url.pathname === "/api/v1/members/me")
      return respond(route, { ...me, email: "me@example.com" });
    if (url.pathname === `/api/v1/posts/${postId}`)
      return respond(route, {
        id: postId,
        title: "댓글 확인용 후기",
        festivalId: 72,
        festivalTitle: "가을 문화축제",
        member: neighbor,
        thumbnail: null,
        content: "<p>본문</p>",
        date: "2026-09-30T10:00:00",
        likeCount: 0,
        likedByMe: false,
      });
    if (
      url.pathname === `/api/v1/posts/${postId}/comments` &&
      method === "GET"
    ) {
      listQueries.push(url.searchParams);
      return respond(
        route,
        commentPage(
          store,
          Number(url.searchParams.get("page") || 0),
          Number(url.searchParams.get("size") || 20),
        ),
      );
    }
    if (
      url.pathname === `/api/v1/posts/${postId}/comments` &&
      method === "POST"
    ) {
      const body = request.postDataJSON() as {
        content: string;
        parentId: number | null;
      };
      created.push(body);
      const comment: StoredComment = {
        id: store.length + 1,
        parentId: body.parentId,
        deleted: false,
        member: me,
        content: body.content,
        date: "2026-10-03T09:00:00",
      };
      store.push(comment);
      return respond(route, { ...comment, postId, replies: [] });
    }
    const deleteMatch = url.pathname.match(/^\/api\/v1\/comments\/(\d+)$/);
    if (deleteMatch && method === "DELETE") {
      const target = store.find((c) => c.id === Number(deleteMatch[1]));
      if (target) target.deleted = true;
      return respond(route, null);
    }
    return respond(route, "예상하지 않은 요청", 404);
  });
  return { created, listQueries };
}

test("review comments show oldest first, keep deleted comments as placeholders and post replies", async ({
  page,
}) => {
  const { created, listQueries } = await setup(page);
  await page.goto(`/reviews/${postId}`);

  const threads = page.locator(".comment-thread");
  await expect(threads).toHaveCount(2);
  // 정렬은 서버가 고정한다. 프론트는 sort를 보내지 않는다.
  expect(listQueries[0].get("sort")).toBeNull();
  expect(listQueries[0].get("size")).toBe("20");

  // 오래된 댓글이 위에 온다. 지워진 댓글은 문구만 남고 작성자·버튼이 없다.
  const deletedRoot = threads.nth(0).locator(":scope > .comment-row");
  await expect(deletedRoot).toHaveClass(/deleted/);
  await expect(deletedRoot).toHaveText("삭제된 댓글입니다");
  await expect(deletedRoot.getByRole("button")).toHaveCount(0);
  // 부모가 지워져도 답글은 그대로 보인다.
  await expect(
    threads.nth(0).locator(".comment-replies .comment-row"),
  ).toContainText("지워진 댓글에 남은 답글");
  await expect(threads.nth(1)).toContainText("내가 쓴 댓글");

  // 답글에 다시 답글을 달면 부모 댓글 ID로 보내고, 상대 닉네임을 미리 넣어 준다.
  await threads
    .nth(0)
    .locator(".comment-replies .comment-row")
    .getByRole("button", { name: "답글", exact: true })
    .click();
  await expect(page.getByLabel("답글 내용")).toHaveValue("@이웃 ");
  await page.getByLabel("답글 내용").fill("@이웃 저도 궁금해요");
  await page.getByRole("button", { name: "답글 남기기" }).click();
  await expect(
    threads.nth(0).locator(".comment-replies .comment-row"),
  ).toHaveCount(2);
  expect(created).toEqual([{ content: "@이웃 저도 궁금해요", parentId: 1 }]);

  // 일반 댓글은 parentId 없이 보내고, 맨 아래에 붙는다.
  await page.getByLabel("댓글 내용", { exact: true }).fill("새로 쓴 댓글");
  await page.getByRole("button", { name: "댓글 남기기" }).click();
  await expect(threads).toHaveCount(3);
  await expect(threads.nth(2)).toContainText("새로 쓴 댓글");
  expect(created.at(-1)).toEqual({ content: "새로 쓴 댓글", parentId: null });

  // 내 댓글을 지우면 사라지지 않고 같은 자리에 문구가 남는다.
  const mine = threads.nth(1).locator(":scope > .comment-row");
  await mine.getByRole("button", { name: "삭제", exact: true }).click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "삭제", exact: true })
    .click();
  await expect(mine).toHaveText("삭제된 댓글입니다");
  await expect(threads).toHaveCount(3);
});
