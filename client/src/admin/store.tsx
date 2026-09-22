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
export interface ContentItem {
  id: string;
  title: string;
  author: string;
  source: "PUBLIC" | "MEMBER";
  category: string;
  date: string;
  status: Visibility;
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
 * 회원(members)과 문의(tickets)는 실제 서버에서 오므로 여기 없다.
 */
interface AdminState {
  version: 3;
  events: ContentItem[];
  reviews: ContentItem[];
  activity: Activity[];
}
/** 화면이 받아 쓰는 데이터. members와 tickets는 실제 서버에서 온다. */
interface AdminData extends AdminState {
  members: AdminMember[];
  tickets: Ticket[];
}
const storageKey = "eventus.admin.mock.v3";
function seed(): AdminState {
  return {
    version: 3,
    events: [
      {
        id: "E-2008",
        title: "가을 정원 산책",
        author: "경기도 문화행사 데이터",
        source: "PUBLIC",
        category: "축제",
        date: "2026-09-21",
        status: "PUBLISHED",
        image: "/images/garden.jpg",
        content:
          "가을꽃이 피어난 정원을 산책하는 예시 문화행사입니다. 일정: 10월 3일 ~ 10월 12일 · 가평군.",
      },
      {
        id: "E-2007",
        title: "우리 동네 주말 플리마켓",
        author: "동네탐험가",
        source: "MEMBER",
        category: "장터",
        date: "2026-09-21",
        status: "PENDING",
        image: "/images/market.jpg",
        content:
          "동네 주민과 함께하는 주말 장터를 제보합니다. 일정: 10월 10일 · 수원시. 장소 및 운영 시간 확인이 필요합니다.",
      },
      {
        id: "E-2006",
        title: "수원 화성의 가을밤",
        author: "경기도 문화행사 데이터",
        source: "PUBLIC",
        category: "문화",
        date: "2026-09-20",
        status: "PUBLISHED",
        image: "/images/palace.jpg",
        content:
          "수원 화성에서 즐기는 야간 문화 산책 예시입니다. 일정: 10월 9일 ~ 10월 11일 · 수원시.",
      },
      {
        id: "E-2005",
        title: "호수공원 작은 음악회",
        author: "문화산책",
        source: "MEMBER",
        category: "공연",
        date: "2026-09-20",
        status: "PENDING",
        image: "/images/music.jpg",
        content:
          "가을 저녁, 호숫가에서 열리는 어쿠스틱 공연입니다. 일정: 10월 17일 · 고양시.",
      },
      {
        id: "E-2004",
        title: "선입금 필수 무료 축제",
        author: "오늘의 행사",
        source: "MEMBER",
        category: "축제",
        date: "2026-09-19",
        status: "PUBLISHED",
        image: "/images/flowers.jpg",
        content:
          "무료 축제로 소개되어 있지만 개인 계좌로 선입금을 안내하고 있어 신고가 접수된 예시 행사입니다.",
      },
      {
        id: "E-2003",
        title: "작은 미술관 가을 전시",
        author: "경기도 문화행사 데이터",
        source: "PUBLIC",
        category: "전시",
        date: "2026-09-18",
        status: "PUBLISHED",
        image: "/images/art.jpg",
        content:
          "지역 작가들의 그림을 만나보는 예시 전시입니다. 일정: 10월 1일 ~ 10월 25일 · 파주시.",
      },
      {
        id: "E-2002",
        title: "중복 등록된 꽃 축제",
        author: "꽃길따라",
        source: "MEMBER",
        category: "축제",
        date: "2026-09-17",
        status: "HIDDEN",
        content: "동일한 일정과 장소로 중복 제보된 예시 행사입니다.",
        reason: "기존 행사와 내용이 중복되어 숨김 처리",
      },
    ],
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
      data?.version === 3 &&
      ["events", "reviews", "activity"].every((k) => Array.isArray(data[k]))
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
  moderate: (
    kind: "events" | "reviews",
    id: string,
    status: Visibility,
    reason: string,
  ) => void;
  /** 답변 등록. 서버에 저장하므로 실패할 수 있다. */
  answer: (id: string, answer: string) => Promise<void>;
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
    () => ({ ...data, members, tickets }),
    [data, members, tickets],
  );
  return (
    <Context.Provider
      value={{
        data: value,
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
          const item = data[kind].find((item) => item.id === id);
          if (
            !item ||
            !reason.trim() ||
            item.status === status ||
            (kind === "reviews" && status === "PENDING")
          )
            return;
          commit(
            (current) => ({
              ...current,
              [kind]: current[kind].map((item) =>
                item.id === id
                  ? { ...item, status, reason: reason.trim() }
                  : item,
              ),
            }),
            {
              area: kind === "events" ? "행사" : "후기",
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
