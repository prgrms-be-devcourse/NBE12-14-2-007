import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { useApp } from "../lib/context";
import { dateText, errorText } from "../lib/format";
import type {
  AdminInquiryListItem,
  AdminMemberInfo,
  FestivalSearchItem,
  Role,
} from "../lib/types";

export const roleNames: Record<Role, string> = {
  ROLE_UNVERIFIED: "미인증",
  ROLE_NORMAL: "일반",
  ROLE_TRUSTED: "신뢰",
  ROLE_WARNING: "주의",
  ROLE_ADMIN: "관리자",
};
export type Visibility = "PUBLISHED" | "PENDING" | "HIDDEN";
export const visibilityNames: Record<Visibility, string> = {
  PUBLISHED: "공개",
  PENDING: "검토 대기",
  HIDDEN: "숨김",
};
export interface AdminMember {
  id: string;
  name: string;
  email: string;
  role: Role;
  joined: string;
  /** 공개 URL. 없으면 닉네임 첫 글자로 아바타를 그린다 */
  profileImg: string | null;
  /** 탈퇴 시각. 탈퇴하지 않았으면 null */
  deletedAt: string | null;
}
/**
 * 행사 진행 상태. 서버 FestivalStatus 와 같은 값으로, 종료일에서 계산된다.
 * 노출 상태(Visibility)와는 다른 축이다. 행사에는 아직 노출 상태 개념이 없다.
 */
export type RunState = "OPEN" | "CLOSED";
export const runStateNames: Record<RunState, string> = {
  OPEN: "진행중",
  CLOSED: "종료",
};
/** 상태 뱃지 하나로 둘 다 그리기 위한 통합 이름표. */
export const statusNames: Record<string, string> = {
  ...visibilityNames,
  ...runStateNames,
};
export interface ContentItem {
  id: string;
  title: string;
  author: string;
  source: "PUBLIC" | "MEMBER";
  category: string;
  date: string;
  /** 행사는 RunState(서버값), 후기는 Visibility(아직 mock) */
  status: Visibility | RunState;
  content: string;
  image?: string;
  reason?: string;
}
export interface Ticket {
  id: string;
  category: "QUESTION" | "REPORT";
  title: string;
  author: string;
  date: string;
  content: string;
  status: "PENDING" | "ANSWERED";
  answer: string;
  target?: { kind: "events" | "reviews"; id: string; title: string };
}
export interface Activity {
  id: string;
  at: string;
  area: string;
  action: string;
  target: string;
  reason: string;
}
/**
 * 아직 API가 없어 브라우저에만 두는 예시 데이터.
 * 회원·문의·행사는 실제 서버에서 오므로 여기 없다.
 */
interface AdminState {
  version: 4;
  reviews: ContentItem[];
  activity: Activity[];
}
/** 화면이 받아 쓰는 데이터. reviews와 activity만 아직 예시다. */
interface AdminData extends AdminState {
  members: AdminMember[];
  tickets: Ticket[];
  events: ContentItem[];
}
const storageKey = "eventus.admin.mock.v4";
function seed(): AdminState {
  return {
    version: 4,
    reviews: [
      {
        id: "P-3005",
        title: "가을 정원에서 보낸 오후",
        author: "산책하는 하루",
        source: "MEMBER",
        category: "가을 정원 산책",
        date: "2026-09-21",
        status: "PUBLISHED",
        content:
          "산책로가 잘 정비되어 있어서 가족과 함께 걷기 좋았어요. 다음에는 조금 일찍 방문해 보려고 합니다.",
      },
      {
        id: "P-3004",
        title: "광고 링크가 반복되는 후기",
        author: "오늘의 행사",
        source: "MEMBER",
        category: "수원 화성의 가을밤",
        date: "2026-09-21",
        status: "PUBLISHED",
        content:
          "행사와 관련 없는 광고 문구와 외부 링크가 반복 게시되어 신고된 예시 후기입니다.",
      },
      {
        id: "P-3003",
        title: "밤에 보니 더 멋진 화성",
        author: "주말의 기록",
        source: "MEMBER",
        category: "수원 화성의 가을밤",
        date: "2026-09-20",
        status: "PUBLISHED",
        content:
          "조명이 켜진 성곽을 걸으며 사진을 남겼어요. 대중교통으로 방문하면 편해요.",
      },
      {
        id: "P-3002",
        title: "주말 장터 방문 기록",
        author: "가을바람",
        source: "MEMBER",
        category: "우리 동네 주말 플리마켓",
        date: "2026-09-19",
        status: "PUBLISHED",
        content:
          "작은 수공예품부터 먹거리까지 구경할 것이 많았습니다. 운영 시간이 안내와 달라 조금 아쉬웠어요.",
      },
      {
        id: "P-3001",
        title: "부적절한 표현이 포함된 후기",
        author: "오늘의 행사",
        source: "MEMBER",
        category: "작은 미술관 가을 전시",
        date: "2026-09-18",
        status: "HIDDEN",
        content:
          "다른 이용자를 비방하는 내용이 포함되어 숨김 처리한 예시입니다.",
        reason: "이용자 비방 표현 확인",
      },
    ],
    activity: [
      {
        id: "A-3",
        at: "2026-09-21T09:40:00+09:00",
        area: "회원",
        action: "등급 변경",
        target: "문화산책",
        reason: "정상적인 행사 제보 이력 확인 · 일반 → 신뢰",
      },
      {
        id: "A-2",
        at: "2026-09-19T14:20:00+09:00",
        area: "문의·신고",
        action: "답변 등록",
        target: "Q-4002",
        reason: "회원 등급 확인 경로 안내",
      },
      {
        id: "A-1",
        at: "2026-09-18T11:05:00+09:00",
        area: "행사",
        action: "숨김 처리",
        target: "중복 등록된 꽃 축제",
        reason: "기존 행사와 내용이 중복되어 숨김 처리",
      },
    ],
  };
}
function read(): AdminState {
  try {
    const data = JSON.parse(sessionStorage.getItem(storageKey) || "null");
    if (
      data?.version === 4 &&
      ["reviews", "activity"].every((k) => Array.isArray(data[k]))
    ) {
      return data as AdminState;
    }
  } catch {
    /* An unavailable or older demo store starts with fresh examples. */
  }
  return seed();
}
/** 서버 회원 한 줄을 화면이 쓰는 AdminMember 모양으로 바꾼다. */
function toMember(item: AdminMemberInfo): AdminMember {
  return {
    id: item.id,
    name: item.nickname,
    email: item.email,
    role: item.role,
    joined: dateText(item.createdAt),
    profileImg: item.profileImg,
    deletedAt: item.deletedAt,
  };
}
/**
 * 서버 행사 한 줄을 화면이 쓰는 ContentItem 모양으로 바꾼다.
 *
 * status 는 노출 상태가 아니라 진행 상태(OPEN/CLOSED)다.
 * 행사에는 아직 노출 상태 컬럼이 없어서 숨김 처리를 표현할 수 없다.
 */
function toEvent(item: FestivalSearchItem): ContentItem {
  return {
    id: String(item.festivalId),
    title: item.title,
    // 공공 행사는 주최 기관, 회원 제보는 기관명이 비어 있을 수 있다.
    author: item.instNm || (item.providerType === "MEMBER" ? "회원 제보" : "-"),
    source: item.providerType,
    category: item.category,
    date: dateText(item.beginDe),
    status: item.status,
    // 목록 응답에는 본문이 없다. 상세를 열 때 채워 넣는다.
    content: "",
    image: item.imgUrl ?? undefined,
  };
}
/** 서버 목록 한 줄을 화면이 쓰는 Ticket 모양으로 바꾼다. */
function toTicket(item: AdminInquiryListItem): Ticket {
  return {
    id: item.id,
    category: item.category,
    title: item.title,
    // 탈퇴하면 writer가 null로 내려온다.
    author: item.writer?.nickname ?? "탈퇴한 사용자",
    date: dateText(item.createdAt),
    // 목록 응답에는 본문과 답변이 없다. 상세를 열 때 채워 넣는다.
    content: "",
    status: item.status,
    answer: "",
  };
}
interface AdminContextValue {
  data: AdminData;
  /**
   * 등급 변경. 서버에 저장하므로 실패할 수 있다.
   *
   * TODO reason을 서버로 보낼 것. 저장할 이력 테이블이 없어 지금은
   *      입력칸을 잠가 뒀고(MemberDialog), 운영 기록(로컬)에만 남는다.
   *      파라미터는 이력 테이블이 생기면 바로 쓰려고 남겨 둔다.
   */
  changeRole: (id: string, role: Role, reason: string) => Promise<void>;
  /**
   * 노출 상태 변경.
   *
   * TODO 서버에 노출 상태 컬럼이 없어 아직 로컬에서만 동작한다.
   *      행사는 실제 데이터라 여기서 바꿔도 새로고침하면 되돌아가므로
   *      화면(ContentDialog)에서 아예 막아 뒀다. 후기는 아직 예시 데이터라 그대로 둔다.
   */
  moderate: (
    kind: "events" | "reviews",
    id: string,
    status: Visibility,
    reason: string,
  ) => void;
  /** 답변 등록. 서버에 저장하므로 실패할 수 있다. */
  answer: (id: string, answer: string) => Promise<void>;
  /** 행사 목록을 서버에서 다시 불러온다. */
  reloadEvents: () => void;
  eventsLoading: boolean;
  eventsError: string;
  /** 회원 목록을 서버에서 다시 불러온다. */
  reloadMembers: () => void;
  membersLoading: boolean;
  membersError: string;
  /** 문의 목록을 서버에서 다시 불러온다. */
  reloadTickets: () => void;
  ticketsLoading: boolean;
  ticketsError: string;
  reset: () => void;
}
const Context = createContext<AdminContextValue | null>(null);
export function AdminProvider({ children }: { children: ReactNode }) {
  // 아래 changeRole 등에서 member를 지역 변수로 쓰고 있어 이름을 구분한다.
  const { api, authLoading, member: signedInAdmin } = useApp();
  const [data, setData] = useState(read);
  const [message, setMessage] = useState("");
  const [storageError, setStorageError] = useState(false);
  const [events, setEvents] = useState<ContentItem[]>([]);
  const [eventsLoading, setEventsLoading] = useState(true);
  const [eventsError, setEventsError] = useState("");
  const [eventsRevision, setEventsRevision] = useState(0);
  const [members, setMembers] = useState<AdminMember[]>([]);
  const [membersLoading, setMembersLoading] = useState(true);
  const [membersError, setMembersError] = useState("");
  const [membersRevision, setMembersRevision] = useState(0);
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [ticketsLoading, setTicketsLoading] = useState(true);
  const [ticketsError, setTicketsError] = useState("");
  const [ticketsRevision, setTicketsRevision] = useState(0);
  // 토큰 복구가 끝나기 전에 부르면 첫 요청이 401로 한 번 헛돈다.
  const ready = !authLoading && signedInAdmin?.role === "ROLE_ADMIN";
  useEffect(() => {
    if (!ready) {
      setEvents([]);
      setEventsLoading(authLoading);
      return;
    }
    let active = true;
    setEventsLoading(true);
    setEventsError("");
    api
      // 검색·필터를 화면에서 하고 있어서 한 번에 받아 두고 거른다.
      // 행사는 수천 건이라 이 방식이 오래 못 간다.
      // TODO 검색어·유입 경로를 서버 파라미터(keyword/providerType)로 넘길 것.
      .adminFestivals({ size: 50 })
      .then((page) => {
        if (active) setEvents(page.content.map(toEvent));
      })
      .catch((error) => {
        if (active) setEventsError(errorText(error));
      })
      .finally(() => {
        if (active) setEventsLoading(false);
      });
    return () => {
      active = false;
    };
  }, [api, ready, authLoading, eventsRevision]);
  useEffect(() => {
    if (!ready) {
      setMembers([]);
      setMembersLoading(authLoading);
      return;
    }
    let active = true;
    setMembersLoading(true);
    setMembersError("");
    api
      // 문의와 같은 이유로 한 번에 받아 두고 화면에서 거른다.
      // size는 서버 max-page-size(50)가 상한이다.
      .adminMembers({ size: 50, includeDeleted: true })
      .then((page) => {
        if (active) setMembers(page.content.map(toMember));
      })
      .catch((error) => {
        if (active) setMembersError(errorText(error));
      })
      .finally(() => {
        if (active) setMembersLoading(false);
      });
    return () => {
      active = false;
    };
  }, [api, ready, authLoading, membersRevision]);
  useEffect(() => {
    if (!ready) {
      // 관리자가 아니면 목록을 부를 이유가 없다. 로딩 표시도 끝내야 한다.
      setTickets([]);
      setTicketsLoading(authLoading);
      return;
    }
    let active = true;
    setTicketsLoading(true);
    setTicketsError("");
    api
      // 검색·필터를 화면에서 하고 있어서 한 번에 받아 두고 거른다.
      // 건수가 늘면 서버 검색 파라미터(title/status/category)로 옮겨야 한다.
      // size는 서버 max-page-size(50)가 상한이다.
      .adminInquiries({ size: 50 })
      .then((page) => {
        if (active) setTickets(page.content.map(toTicket));
      })
      .catch((error) => {
        if (active) setTicketsError(errorText(error));
      })
      .finally(() => {
        if (active) setTicketsLoading(false);
      });
    return () => {
      active = false;
    };
  }, [api, ready, authLoading, ticketsRevision]);
  useEffect(() => {
    try {
      sessionStorage.setItem(storageKey, JSON.stringify(data));
      setStorageError(false);
    } catch {
      setStorageError(true);
    }
  }, [data]);
  useEffect(() => {
    if (!message) return;
    const timer = setTimeout(() => setMessage(""), 4000);
    return () => clearTimeout(timer);
  }, [message]);
  /** 운영 기록에 한 줄 남긴다. 감사 로그 API가 없어 탭 안에서만 유지된다. */
  function record(entry: Omit<Activity, "id" | "at">) {
    const log = {
      ...entry,
      id: crypto.randomUUID(),
      at: new Date().toISOString(),
    };
    setData((current) => ({
      ...current,
      activity: [log, ...current.activity],
    }));
  }
  function commit(
    update: (current: AdminState) => AdminState,
    entry: Omit<Activity, "id" | "at">,
  ) {
    setData(update);
    record(entry);
    setMessage("Mock 데이터에 반영했습니다.");
  }
  // 매 렌더마다 새 객체를 만들면 data를 의존성에 넣은 쪽이 계속 다시 돈다.
  const value = useMemo(
    () => ({ ...data, members, tickets, events }),
    [data, members, tickets, events],
  );
  return (
    <Context.Provider
      value={{
        data: value,
        eventsLoading,
        eventsError,
        reloadEvents() {
          setEventsRevision((current) => current + 1);
        },
        membersLoading,
        membersError,
        reloadMembers() {
          setMembersRevision((current) => current + 1);
        },
        ticketsLoading,
        ticketsError,
        reloadTickets() {
          setTicketsRevision((current) => current + 1);
        },
        async changeRole(id, role, reason) {
          const member = members.find((item) => item.id === id);
          // 아래 조건은 서버도 똑같이 막는다. 여기서 거르는 건 헛된 요청을 줄이기 위함이다.
          if (
            !member ||
            member.role === "ROLE_ADMIN" ||
            role === "ROLE_ADMIN" ||
            role === member.role
          )
            return;
          const saved = await api.changeMemberRole(id, role);
          // 서버가 돌려준 값으로 갈아끼운다. 낙관적 갱신을 하면
          // 저장에 실패했을 때 화면만 바뀐 채로 남는다.
          setMembers((current) =>
            current.map((item) => (item.id === id ? toMember(saved) : item)),
          );
          const change = `${roleNames[member.role]} → ${roleNames[role]}`;
          record({
            area: "회원",
            action: "등급 변경",
            target: member.name,
            // TODO 사유가 서버에 저장되면 항상 붙여서 남길 것.
            //      지금은 입력칸을 잠가 둬서 reason이 항상 빈 문자열로 들어온다.
            reason: reason.trim() ? `${change} · ${reason.trim()}` : change,
          });
          setMessage("회원 등급을 변경했습니다.");
        },
        moderate(kind, id, status, reason) {
          // 행사는 이제 실제 서버 데이터다. 노출 상태 컬럼이 없어서
          // 여기서 바꿔봤자 새로고침하면 되돌아간다. 화면에서도 막아 두었다.
          if (kind === "events") return;
          const item = data.reviews.find((item) => item.id === id);
          if (
            !item ||
            !reason.trim() ||
            item.status === status ||
            status === "PENDING"
          )
            return;
          commit(
            (current) => ({
              ...current,
              reviews: current.reviews.map((item) =>
                item.id === id
                  ? { ...item, status, reason: reason.trim() }
                  : item,
              ),
            }),
            {
              area: "후기",
              action: `${visibilityNames[status]} 처리`,
              target: item.title,
              reason: reason.trim(),
            },
          );
        },
        async answer(id, text) {
          const ticket = tickets.find((item) => item.id === id);
          if (!ticket || !text.trim()) return;
          const saved = await api.answerInquiry(id, text.trim());
          // 서버가 돌려준 값으로 갈아끼운다. 낙관적 갱신을 하면
          // 저장에 실패했을 때 화면만 답변 완료로 남는다.
          setTickets((current) =>
            current.map((item) =>
              item.id === id
                ? { ...item, status: saved.status, answer: saved.answer ?? "" }
                : item,
            ),
          );
          record({
            area: "문의·신고",
            action: "답변 등록",
            target: ticket.id,
            reason: ticket.title,
          });
          setMessage("답변을 등록했습니다.");
        },
        reset() {
          setData(seed());
          setMessage("관리자 예시 데이터를 초기화했습니다.");
        },
      }}
    >
      {children}
      {storageError && (
        <div className="adm-storage-warning" role="alert">
          브라우저 저장소를 사용할 수 없어 새로고침하면 변경 내용이 사라집니다.
        </div>
      )}
      {message && (
        <div className="toast" role="status">
          {message}
          <button onClick={() => setMessage("")} aria-label="알림 닫기">
            ×
          </button>
        </div>
      )}
    </Context.Provider>
  );
}
export function useAdmin() {
  const context = useContext(Context);
  if (!context) throw new Error("Missing AdminProvider");
  return context;
}
