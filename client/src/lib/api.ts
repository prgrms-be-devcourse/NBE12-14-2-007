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
  PostSearch,
  AdminPostQuery,
  AdminPostSummary,
  Page,
  Comment,
  Inquiry,
  InquiryInput,
  AdminInquiryListItem,
  AdminInquiryDetail,
  AdminInquiryQuery,
  AdminMemberInfo,
  AdminMemberQuery,
  AdminFestivalQuery,
  AdminFestivalDetail,
  AdminStats,
  SyncResult,
  Role,
  EventView,
  FestivalAccuracyVote,
  FestivalDetailItem,
  FestivalSearchInput,
  FestivalSearchItem,
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
import { matchesRegion } from "./regions";

// 선택 항목을 비운 등록·수정 요청은 DB에 빈 문자열 대신 null로 저장합니다.
function submissionPayload(input: SubmissionInput) {
  const optional = (value?: string) => value?.trim() || null;
  return {
    ...input,
    festivalContent: optional(input.festivalContent),
    regionDetail: optional(input.regionDetail),
    eventTmInfo: optional(input.eventTmInfo),
    instNm: optional(input.instNm),
    referenceUrl: input.referenceUrl.trim(),
    imgUrl: optional(input.imgUrl),
    partcptExpnInfo: optional(input.partcptExpnInfo),
    telnoInfo: optional(input.telnoInfo),
    hostInstNm: optional(input.hostInstNm),
  };
}

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

function toEventView(item: FestivalSearchItem): EventView {
  return {
    festivalId: item.festivalId,
    source: item.providerType,
    title: item.title,
    category: item.category,
    instNm: item.instNm,
    manager: null,
    festivalContent: null,
    referenceUrl: null,
    region: item.region,
    regionDetail: "",
    imgUrl: item.imgUrl,
    beginDe: item.beginDe,
    endDe: item.endDe,
    eventTmInfo: null,
    partcptExpnInfo: null,
    telnoInfo: null,
    hostInstNm: null,
    writngDe: null,
    status: item.status,
  };
}

function toDetailEvent(item: FestivalDetailItem): EventView {
  return {
    festivalId: item.festivalId,
    source: item.providerType,
    title: item.title,
    category: item.category,
    instNm: item.instNm,
    manager: null,
    festivalContent: null,
    referenceUrl: item.url,
    region: item.region,
    regionDetail: "",
    imgUrl: item.imgUrl,
    beginDe: item.beginDe,
    endDe: item.endDe,
    eventTmInfo: item.eventTmInfo,
    partcptExpnInfo: item.partcptExpnInfo,
    telnoInfo: item.telnoInfo,
    hostInstNm: item.hostInstNm,
    writngDe: null,
    status: item.status,
  };
}

export function createApi(mode: Mode) {
  const demo = mode === "preview";
  const demoAccuracyVotes = new Map<number, FestivalAccuracyVote>();
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
    async festivals(input: FestivalSearchInput = {}): Promise<Page<EventView>> {
      const page = Math.max(0, input.page || 0);
      const keyword = input.keyword?.trim().toLowerCase() || "";

      if (demo) {
        const items = demoEvents
          .filter(
            (event) =>
              (!keyword ||
                `${event.title} ${event.instNm || ""} ${event.regionDetail}`
                  .toLowerCase()
                  .includes(keyword)) &&
              (!input.region || matchesRegion(event.region, input.region)) &&
              (!input.providerType || event.source === input.providerType) &&
              (!input.category || event.category === input.category) &&
              (!input.date ||
                (event.beginDe.slice(0, 10) <= input.date &&
                  event.endDe.slice(0, 10) >= input.date)) &&
              (!input.excludeClosed || event.status !== "CLOSED"),
          )
          .sort((a, b) =>
            input.sort === "name"
              ? a.title.localeCompare(b.title, "ko")
              : a.beginDe.localeCompare(b.beginDe),
          );

        return pageOf(items, page, 9);
      }

      const params = new URLSearchParams({
        page: String(page),
        size: "9",
      });
      if (input.keyword?.trim()) params.set("keyword", input.keyword.trim());
      if (input.region) params.set("region", input.region);
      if (input.providerType) params.set("providerType", input.providerType);
      if (input.category) params.set("category", input.category);
      if (input.date) params.set("date", input.date);
      if (input.excludeClosed) params.set("excludeClosed", "true");
      if (input.sort === "name") params.set("sort", "title,asc");

      const result = await transport<Page<FestivalSearchItem>>(
        `/festivals?${params.toString()}`,
      );
      return {
        ...result,
        content: result.content.map(toEventView),
      };
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
    async event(festivalId: number): Promise<EventView> {
      if (demo) {
        const event = [
          ...demoEvents.filter((e) => e.source === "PUBLIC"),
          ...previewSubmittedEvents(),
        ].find((e) => e.festivalId === festivalId);
        if (!event) throw new ApiError("행사를 찾을 수 없어요.", 404);
        return event;
      }
      const result = await transport<FestivalDetailItem>(
        `/festivals/${encodeURIComponent(String(festivalId))}`,
      );
      return toDetailEvent(result);
    },
    async accuracyVotes(festivalId: number): Promise<FestivalAccuracyVote> {
      if (demo)
        return (
          demoAccuracyVotes.get(festivalId) ?? {
            accurateCount: 0,
            inaccurateCount: 0,
            myVote: null,
          }
        );
      return transport<FestivalAccuracyVote>(
        `/festivals/${encodeURIComponent(String(festivalId))}/accuracy-votes`,
      );
    },
    async voteAccuracy(
      festivalId: number,
      voteType: "ACCURATE" | "INACCURATE",
    ): Promise<FestivalAccuracyVote> {
      if (!demo)
        return transport<FestivalAccuracyVote>(
          `/festivals/${encodeURIComponent(String(festivalId))}/accuracy-votes/me`,
          "PUT",
          { voteType },
        );
      const current = await this.accuracyVotes(festivalId);
      const next = {
        accurateCount:
          current.accurateCount +
          (voteType === "ACCURATE" ? 1 : 0) -
          (current.myVote === "ACCURATE" ? 1 : 0),
        inaccurateCount:
          current.inaccurateCount +
          (voteType === "INACCURATE" ? 1 : 0) -
          (current.myVote === "INACCURATE" ? 1 : 0),
        myVote: voteType,
      };
      demoAccuracyVotes.set(festivalId, next);
      return next;
    },
    async cancelAccuracyVote(
      festivalId: number,
    ): Promise<FestivalAccuracyVote> {
      if (!demo)
        return transport<FestivalAccuracyVote>(
          `/festivals/${encodeURIComponent(String(festivalId))}/accuracy-votes/me`,
          "DELETE",
        );
      const current = await this.accuracyVotes(festivalId);
      const next = {
        accurateCount:
          current.accurateCount - (current.myVote === "ACCURATE" ? 1 : 0),
        inaccurateCount:
          current.inaccurateCount -
          (current.myVote === "INACCURATE" ? 1 : 0),
        myVote: null,
      };
      demoAccuracyVotes.set(festivalId, next);
      return next;
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
      }>("/festivals/submissions", "POST", submissionPayload(input));
    },
    async updateSubmission(submissionId: string, input: SubmissionInput) {
      return transport<SubmissionDetail>(
        `/members/me/submissions/${encodeURIComponent(submissionId)}`,
        "PATCH",
        submissionPayload(input),
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
      search: PostSearch = {},
    ): Promise<Page<PostSummary>> {
      const keyword = search.keyword?.trim();
      if (!demo) {
        if (sort === "likes,desc")
          throw new ApiError("좋아요순 정렬을 준비하고 있어요.");
        const params = new URLSearchParams({
          page: String(page),
          size: "6",
          sort,
        });
        if (festivalId === undefined && keyword) {
          params.set("type", search.type ?? "TITLE");
          params.set("keyword", keyword);
        }
        const path =
          festivalId === undefined
            ? "/posts"
            : `/festivals/${festivalId}/posts`;
        return transport(`${path}?${params}`);
      }
      const posts = previewPosts(festivalId, sort).filter((post) => {
        if (festivalId !== undefined || !keyword) return true;
        const value =
          search.type === "MEMBER_NICKNAME"
            ? post.member.nickname
            : search.type === "FESTIVAL_TITLE"
              ? post.festivalTitle
              : post.title;
        return value.toLocaleLowerCase().includes(keyword.toLocaleLowerCase());
      });
      return pageOf(posts, page, 6);
    },
    // 관리자 목록은 삭제된 후기를 포함한다. preview 모드에서도 실제 API를 사용한다.
    async adminPosts(query: AdminPostQuery = {}) {
      const {
        page = 0,
        size = 20,
        sort = "createdAt,desc",
        type = "TITLE",
        keyword,
      } = query;
      const params = new URLSearchParams({
        page: String(page),
        size: String(size),
        sort,
      });
      if (keyword?.trim()) {
        params.set("type", type);
        params.set("keyword", keyword.trim());
      }
      return transport<Page<AdminPostSummary>>(`/admin/posts?${params}`);
    },
    async adminPost(postId: string) {
      return transport<PostDetail>(`/posts/${encodeURIComponent(postId)}`);
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
    /**
     * [ADMIN] 문의·신고 목록. 관리자 화면 전용이며 ROLE_ADMIN이 아니면 403이 온다.
     *
     * 예시 데이터 모드에서도 실제 서버를 부른다. 관리자 화면은 로그인한
     * 관리자만 들어오므로 예시로 흉내 낼 이유가 없다.
     */
    async adminInquiries(query: AdminInquiryQuery = {}) {
      const {
        page = 0,
        size = 20,
        sort = "createdAt,desc",
        ...filters
      } = query;
      const params = new URLSearchParams({
        page: String(page),
        size: String(size),
        sort,
      });
      // 빈 문자열을 그대로 보내면 서버가 enum으로 변환하다 400을 낸다.
      for (const [key, value] of Object.entries(filters)) {
        if (value === undefined || value === "") continue;
        params.set(key, String(value));
      }
      return transport<Page<AdminInquiryListItem>>(
        `/admin/inquiries?${params}`,
      );
    },
    /**
     * [ADMIN] 공공 행사 수동 동기화.
     *
     * 공공 API를 통째로 훑어서 오래 걸린다. transport의 15초 타임아웃에 걸릴 수 있다.
     * 이미 실행 중이면 409(SYNC_ALREADY_RUNNING)가 온다.
     */
    async syncFestivals() {
      return transport<SyncResult>("/festivals/sync", "POST");
    },
    /**
     * [ADMIN] 행사 목록. 공개 화면과 같은 GET /festivals 를 쓴다.
     *
     * festivals()를 쓰지 않는 이유: 그쪽은 page size가 9로 고정이고
     * 예시 데이터 모드면 서버를 부르지 않는다. 관리자 화면은 둘 다 곤란하다.
     */
    async adminFestivals(query: AdminFestivalQuery = {}) {
      const {
        page = 0,
        size = 50,
        sort = "beginDe,desc",
        excludeClosed,
        ...filters
      } = query;
      const params = new URLSearchParams({
        page: String(page),
        size: String(size),
        sort,
      });
      // 빈 문자열을 그대로 보내면 서버가 enum으로 변환하다 400을 낸다.
      for (const [key, value] of Object.entries(filters)) {
        if (value === undefined || value === "") continue;
        params.set(key, String(value));
      }
      // false는 보내지 않는다. 서버 기본값이 false다.
      if (excludeClosed) params.set("excludeClosed", "true");
      return transport<Page<FestivalSearchItem>>(`/festivals?${params}`);
    },
    /**
     * [ADMIN] 운영 대시보드 집계.
     *
     * 전용 API가 없어 목록 API에 size=1을 던지고 totalElements만 쓴다.
     * 본문은 1건만 오고 서버는 COUNT만 돌리므로 가볍다.
     * 8번이 동시에 나가므로 체감은 한 번과 비슷하다.
     *
     * TODO 행사·후기에 노출 상태가 생기면 GET /admin/overview 하나로 합칠 것.
     */
    async adminStats(): Promise<AdminStats> {
      const total = (page: Page<unknown>) => page.totalElements;
      const [
        memberTotal,
        memberTrusted,
        memberWarning,
        inquiryPending,
        inquiryReport,
        festivalTotal,
        festivalOpen,
        postTotal,
      ] = await Promise.all([
        // 탈퇴 회원은 빼고 센다. 대시보드의 "전체 회원"은 활동 가능한 회원이다.
        this.adminMembers({ size: 1 }).then(total),
        this.adminMembers({ size: 1, role: "ROLE_TRUSTED" }).then(total),
        this.adminMembers({ size: 1, role: "ROLE_WARNING" }).then(total),
        this.adminInquiries({ size: 1, status: "PENDING" }).then(total),
        this.adminInquiries({
          size: 1,
          status: "PENDING",
          category: "REPORT",
        }).then(total),
        this.adminFestivals({ size: 1 }).then(total),
        this.adminFestivals({ size: 1, excludeClosed: true }).then(total),
        this.adminPosts({ size: 1 }).then(total),
      ]);
      return {
        memberTotal,
        memberTrusted,
        memberWarning,
        inquiryPending,
        inquiryReport,
        festivalTotal,
        festivalOpen,
        postTotal,
      };
    },
    /** [ADMIN] 행사 상세. 목록에 없는 소개 본문·연락처·참가비가 여기서 온다. */
    async adminFestival(festivalId: string | number) {
      return transport<AdminFestivalDetail>(
        `/festivals/${encodeURIComponent(String(festivalId))}`,
      );
    },
    /** [ADMIN] 회원 목록. 닉네임·이메일 부분 일치 검색. */
    async adminMembers(query: AdminMemberQuery = {}) {
      const {
        page = 0,
        size = 20,
        sort = "createdAt,desc",
        ...filters
      } = query;
      const params = new URLSearchParams({
        page: String(page),
        size: String(size),
        sort,
      });
      // 빈 문자열을 그대로 보내면 서버가 enum으로 변환하다 400을 낸다.
      for (const [key, value] of Object.entries(filters)) {
        if (value === undefined || value === "") continue;
        params.set(key, String(value));
      }
      return transport<Page<AdminMemberInfo>>(`/admin/members?${params}`);
    },
    /**
     * [ADMIN] 회원 등급 변경.
     * 본인·관리자 계정·탈퇴 회원은 서버가 거부한다(403/409).
     */
    async changeMemberRole(memberId: string, role: Role) {
      return transport<AdminMemberInfo>(
        `/admin/members/${encodeURIComponent(memberId)}/role`,
        "PATCH",
        { role },
      );
    },
    /** [ADMIN] 문의 상세. 목록에 없는 본문·첨부·답변이 여기서 온다. */
    async adminInquiry(inquiryId: string) {
      return transport<AdminInquiryDetail>(
        `/admin/inquiries/${encodeURIComponent(inquiryId)}`,
      );
    },
    /** [ADMIN] 답변 등록. 이미 답변이 있으면 덮어쓴다. */
    async answerInquiry(inquiryId: string, answer: string) {
      return transport<AdminInquiryDetail>(
        `/admin/inquiries/${encodeURIComponent(inquiryId)}/answer`,
        "PATCH",
        { answer },
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
