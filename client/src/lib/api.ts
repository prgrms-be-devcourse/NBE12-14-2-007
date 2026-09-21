import type {
  Mode,
  Member,
  SignupInput,
  SubmissionInput,
  SubmissionDetail,
  SubmissionSummary,
  PostInput,
  PostDetail,
  PostSummary,
  Page,
  Comment,
  Inquiry,
  InquiryInput,
  EventView,
} from "./types";
import {
  demoEvents,
  previewSubmittedEvents,
  previewPosts,
  readDemo,
  updateDemo,
  type ReviewSort,
} from "./demo";
import { parseDateTime } from "./format";

export class ApiError extends Error {
  constructor(
    message: string,
    public status = 0,
    public code = "",
  ) {
    super(message);
  }
}
let accessToken: string | null = null;
let currentMember: Member | null = null;
let refreshPromise: Promise<void> | null = null;
let sessionVersion = 0;
const now = () => new Date().toISOString();
const id = () => crypto.randomUUID();
/** 예시 데이터로 남아 있는 후기·댓글의 작성자를 실제 로그인 회원으로 맞춘다. */
export function setCurrentMember(member: Member | null) {
  currentMember = member;
}
const demoAuthor = () => currentMember ?? readDemo().member;
export function clearSession() {
  accessToken = null;
  refreshPromise = null;
  sessionVersion += 1;
}
async function transport<T>(
  path: string,
  method = "GET",
  body?: unknown,
  retry = true,
): Promise<T> {
  const requestSession = sessionVersion;
  const form = body instanceof FormData;
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), 15000);
  let response: Response;
  try {
    response = await fetch(`/api/v1${path}`, {
      method,
      credentials: "include",
      signal: controller.signal,
      headers: {
        ...(!form && body !== undefined
          ? { "Content-Type": "application/json" }
          : {}),
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      },
      body: body === undefined ? undefined : form ? body : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(
      "서버에 연결하지 못했어요. 연결 상태를 확인하고 다시 시도해 주세요.",
    );
  } finally {
    clearTimeout(timeout);
  }
  if (requestSession !== sessionVersion) {
    throw new ApiError("세션이 변경되어 이전 요청을 취소했어요.");
  }
  if (response.status === 401 && retry && !path.startsWith("/auth/")) {
    try {
      await refresh();
    } catch {
      if (requestSession === sessionVersion) {
        clearSession();
        window.dispatchEvent(new Event("eventus:session-expired"));
      }
      throw new ApiError("로그인이 필요해요. 다시 로그인해 주세요.", 401);
    }
    return transport<T>(path, method, body, false);
  }
  if (response.status === 401 && !retry && !path.startsWith("/auth/")) {
    clearSession();
    window.dispatchEvent(new Event("eventus:session-expired"));
  }
  if (response.status === 204) return undefined as T;
  const result = await response.json().catch(() => null);
  if (!response.ok || !result?.success) {
    throw new ApiError(
      result?.message ||
        (response.status === 404
          ? "요청한 정보를 찾을 수 없어요."
          : response.status === 403
            ? "이 작업을 할 수 있는 권한이 없어요."
            : "요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요."),
      response.status,
      result?.code,
    );
  }
  return result.data as T;
}
async function refresh() {
  const refreshSession = sessionVersion;
  if (!refreshPromise)
    refreshPromise = transport<{ accessToken: string }>(
      "/auth/refresh",
      "POST",
      undefined,
      false,
    )
      .then((t) => {
        accessToken = t.accessToken;
      })
      .finally(() => {
        if (refreshSession === sessionVersion) refreshPromise = null;
      });
  return refreshPromise;
}
function getPost(postId: string): PostDetail {
  const post = readDemo().posts.find((p) => p.id === postId);
  if (!post) throw new ApiError("후기를 찾을 수 없어요.", 404);
  return post;
}
function pageOf<T>(items: T[], page: number, size: number): Page<T> {
  return {
    content: items.slice(page * size, (page + 1) * size),
    totalElements: items.length,
    totalPages: Math.ceil(items.length / size),
    number: page,
    size,
    first: page === 0,
    last: (page + 1) * size >= items.length,
  };
}

export function createApi(mode: Mode) {
  const demo = mode === "preview";
  return {
    async restore() {
      await refresh();
      return transport<Member>("/members/me");
    },
    async login(email: string, password: string) {
      const token = await transport<{ accessToken: string }>(
        "/auth/login",
        "POST",
        { email, password },
      );
      accessToken = token.accessToken;
      return transport<Member>("/members/me");
    },
    async signup(input: SignupInput) {
      return transport<void>("/auth/signup", "POST", input);
    },
    async logout() {
      try {
        await transport("/auth/logout", "POST");
      } finally {
        clearSession();
      }
    },
    async me() {
      return transport<Member>("/members/me");
    },
    async updateMe(input: {
      nickname?: string;
      phone?: string;
      profileImg?: string;
    }) {
      return transport<Member>("/members/me", "PATCH", input);
    },
    async sendPasswordCode() {
      await transport("/members/me/password/verification-code", "POST");
    },
    async verifyPasswordCode(code: string) {
      await transport("/members/me/password/verify", "POST", { code });
    },
    async changePassword(newPassword: string) {
      await transport("/members/me/password", "PATCH", { newPassword });
      clearSession();
    },
    async submissions(): Promise<SubmissionSummary[]> {
      return transport("/members/me/submissions");
    },
    async submittedEvents(
      page = 0,
      query = "",
      status = "ALL",
    ): Promise<Page<EventView>> {
      if (!demo) {
        throw new ApiError("전체 행사 제보 조회 기능을 준비하고 있어요.");
      }
      return pageOf(
        previewSubmittedEvents().filter(
          (event) =>
            (!query.trim() ||
              event.title.toLowerCase().includes(query.trim().toLowerCase())) &&
            (status === "ALL" || event.status === status),
        ),
        page,
        6,
      );
    },
    async event(festivalId: number): Promise<EventView> {
      if (!demo) {
        throw new ApiError("행사 상세 조회 기능을 준비하고 있어요.");
      }
      const event = [
        ...demoEvents.filter((e) => e.source === "PUBLIC"),
        ...previewSubmittedEvents(),
      ].find((e) => e.festivalId === festivalId);
      if (!event) throw new ApiError("행사를 찾을 수 없어요.", 404);
      return event;
    },
    async submission(submissionId: string) {
      return transport<SubmissionDetail>(
        `/members/me/submissions/${encodeURIComponent(submissionId)}`,
      );
    },
    async createSubmission(input: SubmissionInput) {
      return transport<{
        festivalId: number;
        festivalSubmissionId: string;
        status: string;
      }>("/festivals/submissions", "POST", input);
    },
    async updateSubmission(submissionId: string, input: SubmissionInput) {
      return transport<SubmissionDetail>(
        `/members/me/submissions/${encodeURIComponent(submissionId)}`,
        "PATCH",
        input,
      );
    },
    async deleteSubmission(submissionId: string) {
      return transport<void>(
        `/members/me/submissions/${encodeURIComponent(submissionId)}`,
        "DELETE",
      );
    },
    async posts(
      festivalId?: number,
      page = 0,
      sort: ReviewSort = demo ? "likes,desc" : "createdAt,desc",
    ): Promise<Page<PostSummary>> {
      if (!demo) {
        if (festivalId === undefined)
          throw new ApiError("전체 후기 조회 기능을 준비하고 있어요.");
        if (sort === "likes,desc")
          throw new ApiError("좋아요순 정렬을 준비하고 있어요.");
        return transport(
          `/festivals/${festivalId}/posts?page=${page}&size=6&sort=${encodeURIComponent(sort)}`,
        );
      }
      return pageOf(previewPosts(festivalId, sort), page, 6);
    },
    async post(postId: string) {
      return demo
        ? getPost(postId)
        : transport<PostDetail>(`/posts/${encodeURIComponent(postId)}`);
    },
    async createPost(festivalId: number, input: PostInput) {
      if (!demo)
        return transport<{ id: string }>(
          `/festivals/${festivalId}/posts`,
          "POST",
          input,
        );
      return updateDemo((d) => {
        const postId = id();
        const festivalTitle =
          demoEvents.find((e) => e.festivalId === festivalId)?.title ||
          previewSubmittedEvents().find((e) => e.festivalId === festivalId)
            ?.title ||
          d.submissions.find(
            (s) => s.submission.festival.festivalId === festivalId,
          )?.submission.festival.title ||
          "행사 후기";
        d.posts.unshift({
          ...input,
          thumbnail: input.thumbnail || null,
          id: postId,
          festivalId,
          festivalTitle,
          member: demoAuthor(),
          date: now(),
        });
        return { id: postId };
      });
    },
    async updatePost(postId: string, input: PostInput) {
      if (!demo)
        return transport<PostDetail>(
          `/posts/${encodeURIComponent(postId)}`,
          "PATCH",
          input,
        );
      return updateDemo((d) => {
        const post = d.posts.find((p) => p.id === postId)!;
        Object.assign(post, input, { date: now() });
        return post;
      });
    },
    async deletePost(postId: string) {
      if (!demo)
        return transport<void>(
          `/posts/${encodeURIComponent(postId)}`,
          "DELETE",
        );
      updateDemo((d) => {
        d.posts = d.posts.filter((p) => p.id !== postId);
      });
    },
    async comments(postId: string, page = 0) {
      return demo
        ? readDemo()
            .comments.filter((c) => c.postId === postId)
            .sort(
              (a, b) =>
                parseDateTime(b.date).getTime() -
                parseDateTime(a.date).getTime(),
            )
            .slice(page * 20, (page + 1) * 20)
        : transport<Comment[]>(
            `/posts/${encodeURIComponent(postId)}/comments?page=${page}&size=20&sort=createdAt,desc`,
          );
    },
    async createComment(postId: string, content: string) {
      if (!demo)
        return transport<Comment>(
          `/posts/${encodeURIComponent(postId)}/comments`,
          "POST",
          { content },
        );
      return updateDemo((d) => {
        const author = demoAuthor();
        const c = {
          id: Date.now(),
          postId,
          memberId: author.id,
          nickname: author.nickname,
          profile_img: author.profileImg,
          content,
          date: now(),
        };
        d.comments.unshift(c);
        return c;
      });
    },
    async updateComment(commentId: number, content: string) {
      if (!demo)
        return transport<Comment>(`/comments/${commentId}`, "PATCH", {
          content,
        });
      return updateDemo((d) => {
        const c = d.comments.find((c) => c.id === commentId)!;
        c.content = content;
        return c;
      });
    },
    async deleteComment(commentId: number) {
      if (!demo) return transport<void>(`/comments/${commentId}`, "DELETE");
      updateDemo((d) => {
        d.comments = d.comments.filter((c) => c.id !== commentId);
      });
    },
    async likeCount(postId: string) {
      return demo
        ? readDemo().likes[postId] || 0
        : transport<number>(`/posts/${encodeURIComponent(postId)}/likes`);
    },
    async like(postId: string, remove = false) {
      if (!demo)
        return transport<{ likeCount: number }>(
          `/posts/${encodeURIComponent(postId)}/likes/me`,
          remove ? "DELETE" : "POST",
        );
      return updateDemo((d) => {
        const has = d.liked.includes(postId);
        if (!remove && !has) {
          d.liked.push(postId);
          d.likes[postId] = (d.likes[postId] || 0) + 1;
        }
        if (remove && has) {
          d.liked = d.liked.filter((x) => x !== postId);
          d.likes[postId] = Math.max(0, (d.likes[postId] || 0) - 1);
        }
        return { likeCount: d.likes[postId] || 0 };
      });
    },
    async inquiries() {
      return transport<Inquiry[]>("/inquiries/me");
    },
    async inquiry(inquiryId: string) {
      return transport<Inquiry>(`/inquiries/${encodeURIComponent(inquiryId)}`);
    },
    async createInquiry(input: InquiryInput) {
      return transport<Inquiry>("/inquiries", "POST", input);
    },
    async updateInquiry(inquiryId: string, input: InquiryInput) {
      return transport<Inquiry>(
        `/inquiries/${encodeURIComponent(inquiryId)}`,
        "PATCH",
        input,
      );
    },
    async deleteInquiry(inquiryId: string) {
      return transport<void>(
        `/inquiries/${encodeURIComponent(inquiryId)}`,
        "DELETE",
      );
    },
    async upload(
      file: File,
      type: "POST" | "PROFILE" | "INQUIRY" | "FESTIVAL",
    ) {
      if (file.size > 5 * 1024 * 1024)
        throw new ApiError("이미지는 5MB 이하로 올려 주세요.");
      if (!["image/jpeg", "image/png", "image/webp"].includes(file.type))
        throw new ApiError("JPG, PNG, WEBP 이미지만 올릴 수 있어요.");
      const form = new FormData();
      form.append("file", file);
      form.append("type", type);
      return transport<{ key: string; url: string }>("/images", "POST", form);
    },
  };
}
export type Api = ReturnType<typeof createApi>;
