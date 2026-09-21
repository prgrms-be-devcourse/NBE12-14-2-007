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
  readDemo,
  updateDemo,
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
let refreshPromise: Promise<void> | null = null;
let sessionVersion = 0;
const now = () => new Date().toISOString();
const id = () => crypto.randomUUID();
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
function getSubmission(submissionId: string): SubmissionDetail {
  const item = readDemo().submissions.find(
    (s) => s.submission.festivalSubmissionId === submissionId,
  );
  if (!item) throw new ApiError("제보를 찾을 수 없어요.", 404);
  return item;
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
      if (demo) return readDemo().member;
      await refresh();
      return transport<Member>("/members/me");
    },
    async login(email: string, password: string) {
      if (demo) return readDemo().member;
      const token = await transport<{ accessToken: string }>(
        "/auth/login",
        "POST",
        { email, password },
      );
      accessToken = token.accessToken;
      return transport<Member>("/members/me");
    },
    async signup(input: SignupInput) {
      if (demo) return;
      return transport<void>("/auth/signup", "POST", input);
    },
    async logout() {
      if (!demo) {
        try {
          await transport("/auth/logout", "POST");
        } finally {
          clearSession();
        }
      }
    },
    async me() {
      return demo ? readDemo().member : transport<Member>("/members/me");
    },
    async updateMe(input: {
      nickname?: string;
      phone?: string;
      profileImg?: string;
    }) {
      return demo
        ? updateDemo(
            (d) => (d.member = { ...d.member, ...input, updatedAt: now() }),
          )
        : transport<Member>("/members/me", "PATCH", input);
    },
    async sendPasswordCode() {
      if (!demo)
        await transport("/members/me/password/verification-code", "POST");
    },
    async verifyPasswordCode(code: string) {
      if (!demo)
        await transport("/members/me/password/verify", "POST", { code });
    },
    async changePassword(newPassword: string) {
      if (!demo) {
        await transport("/members/me/password", "PATCH", { newPassword });
        clearSession();
      }
    },
    async submissions(): Promise<SubmissionSummary[]> {
      return demo
        ? readDemo().submissions.map(({ submission: s }) => ({
            ...s.festival,
            festivalSubmissionId: s.festivalSubmissionId,
            createdAt: s.createdAt,
          }))
        : transport("/members/me/submissions");
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
      return demo
        ? getSubmission(submissionId)
        : transport<SubmissionDetail>(
            `/members/me/submissions/${encodeURIComponent(submissionId)}`,
          );
    },
    async createSubmission(input: SubmissionInput) {
      if (!demo)
        return transport<{
          festivalId: number;
          festivalSubmissionId: string;
          status: string;
        }>("/festivals/submissions", "POST", input);
      return updateDemo((d) => {
        const festivalId =
          Math.max(
            2002,
            ...d.submissions.map((s) => s.submission.festival.festivalId),
          ) + 1;
        const festivalSubmissionId = id();
        const status = new Date(input.endDe) < new Date() ? "CLOSED" : "OPEN";
        d.submissions.unshift({
          submission: {
            festivalSubmissionId,
            submissionContent: input.submissionContent,
            createdAt: now(),
            updatedAt: now(),
            festival: {
              instNm: null,
              referenceUrl: null,
              imgUrl: null,
              partcptExpnInfo: null,
              telnoInfo: null,
              hostInstNm: null,
              ...input,
              festivalId,
              status,
              writngDe: now(),
            },
          },
        });
        return { festivalId, festivalSubmissionId, status };
      });
    },
    async updateSubmission(submissionId: string, input: SubmissionInput) {
      if (!demo)
        return transport<SubmissionDetail>(
          `/members/me/submissions/${encodeURIComponent(submissionId)}`,
          "PATCH",
          input,
        );
      return updateDemo((d) => {
        const s = d.submissions.find(
          (s) => s.submission.festivalSubmissionId === submissionId,
        )!;
        Object.assign(s.submission.festival, input, {
          status: new Date(input.endDe) < new Date() ? "CLOSED" : "OPEN",
        });
        s.submission.submissionContent = input.submissionContent;
        s.submission.updatedAt = now();
        return s;
      });
    },
    async deleteSubmission(submissionId: string) {
      if (!demo)
        return transport<void>(
          `/members/me/submissions/${encodeURIComponent(submissionId)}`,
          "DELETE",
        );
      updateDemo((d) => {
        d.submissions = d.submissions.filter(
          (s) => s.submission.festivalSubmissionId !== submissionId,
        );
      });
    },
    async posts(
      festivalId?: number,
      page = 0,
      sort = "createdAt,desc",
    ): Promise<Page<PostSummary>> {
      if (!demo) {
        if (festivalId === undefined)
          throw new ApiError("전체 후기 조회 기능을 준비하고 있어요.");
        return transport(
          `/festivals/${festivalId}/posts?page=${page}&size=6&sort=${encodeURIComponent(sort)}`,
        );
      }
      const posts = readDemo()
        .posts.filter(
          (p) => festivalId === undefined || p.festivalId === festivalId,
        )
        .sort((a, b) =>
          sort.endsWith("asc")
            ? parseDateTime(a.date).getTime() - parseDateTime(b.date).getTime()
            : parseDateTime(b.date).getTime() - parseDateTime(a.date).getTime(),
        );
      return pageOf(posts, page, 6);
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
          member: d.member,
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
        const c = {
          id: Date.now(),
          postId,
          memberId: d.member.id,
          nickname: d.member.nickname,
          profile_img: d.member.profileImg,
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
      return demo
        ? readDemo().inquiries
        : transport<Inquiry[]>("/inquiries/me");
    },
    async inquiry(inquiryId: string) {
      if (!demo)
        return transport<Inquiry>(
          `/inquiries/${encodeURIComponent(inquiryId)}`,
        );
      const q = readDemo().inquiries.find((q) => q.id === inquiryId);
      if (!q) throw new ApiError("문의를 찾을 수 없어요.", 404);
      return q;
    },
    async createInquiry(input: InquiryInput) {
      if (!demo) return transport<Inquiry>("/inquiries", "POST", input);
      return updateDemo((d) => {
        const q: Inquiry = {
          ...input,
          img: input.img || null,
          id: id(),
          status: "PENDING",
          answer: null,
          createdAt: now(),
        };
        d.inquiries.unshift(q);
        return q;
      });
    },
    async updateInquiry(inquiryId: string, input: InquiryInput) {
      if (!demo)
        return transport<Inquiry>(
          `/inquiries/${encodeURIComponent(inquiryId)}`,
          "PATCH",
          input,
        );
      return updateDemo((d) => {
        const q = d.inquiries.find((q) => q.id === inquiryId)!;
        Object.assign(q, input);
        return q;
      });
    },
    async deleteInquiry(inquiryId: string) {
      if (!demo)
        return transport<void>(
          `/inquiries/${encodeURIComponent(inquiryId)}`,
          "DELETE",
        );
      updateDemo((d) => {
        d.inquiries = d.inquiries.filter((q) => q.id !== inquiryId);
      });
    },
    async upload(
      file: File,
      type: "POST" | "PROFILE" | "INQUIRY" | "FESTIVAL",
    ) {
      if (file.size > 5 * 1024 * 1024)
        throw new ApiError("이미지는 5MB 이하로 올려 주세요.");
      if (!["image/jpeg", "image/png", "image/webp"].includes(file.type))
        throw new ApiError("JPG, PNG, WEBP 이미지만 올릴 수 있어요.");
      if (demo) {
        const url = URL.createObjectURL(file);
        return { key: url, url };
      }
      const form = new FormData();
      form.append("file", file);
      form.append("type", type);
      return transport<{ key: string; url: string }>("/images", "POST", form);
    },
  };
}
export type Api = ReturnType<typeof createApi>;
