import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import type { Role } from "../lib/types";

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
interface AdminState {
  version: 1;
  members: AdminMember[];
  events: ContentItem[];
  reviews: ContentItem[];
  tickets: Ticket[];
  activity: Activity[];
}
const storageKey = "eventus.admin.mock.v1";
function seed(): AdminState {
  return {
    version: 1,
    members: [
      {
        id: "M-1008",
        name: "산책하는 하루",
        email: "walk@example.com",
        role: "ROLE_NORMAL",
        joined: "2026-09-21",
      },
      {
        id: "M-1007",
        name: "동네탐험가",
        email: "explore@example.com",
        role: "ROLE_UNVERIFIED",
        joined: "2026-09-21",
      },
      {
        id: "M-1006",
        name: "주말의 기록",
        email: "weekend@example.com",
        role: "ROLE_TRUSTED",
        joined: "2026-09-20",
      },
      {
        id: "M-1005",
        name: "꽃길따라",
        email: "flower@example.com",
        role: "ROLE_NORMAL",
        joined: "2026-09-19",
      },
      {
        id: "M-1004",
        name: "문화산책",
        email: "culture@example.com",
        role: "ROLE_TRUSTED",
        joined: "2026-09-18",
      },
      {
        id: "M-1003",
        name: "오늘의 행사",
        email: "today@example.com",
        role: "ROLE_WARNING",
        joined: "2026-09-17",
      },
      {
        id: "M-1002",
        name: "가을바람",
        email: "autumn@example.com",
        role: "ROLE_NORMAL",
        joined: "2026-09-15",
      },
      {
        id: "M-1001",
        name: "방구석탈출 운영팀",
        email: "admin@example.com",
        role: "ROLE_ADMIN",
        joined: "2026-09-01",
      },
    ],
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
    tickets: [
      {
        id: "Q-4005",
        category: "REPORT",
        title: "무료 행사에서 개인 계좌 입금을 요구해요",
        author: "꽃길따라",
        date: "2026-09-21",
        status: "PENDING",
        answer: "",
        content:
          "무료 행사라고 되어 있는데 주최자가 개인 계좌로 예약금을 보내라고 안내합니다. 행사 정보를 확인해 주세요.",
        target: {
          kind: "events",
          id: "E-2004",
          title: "선입금 필수 무료 축제",
        },
      },
      {
        id: "Q-4004",
        category: "REPORT",
        title: "후기에 광고가 반복해서 올라옵니다",
        author: "주말의 기록",
        date: "2026-09-21",
        status: "PENDING",
        answer: "",
        content:
          "행사와 상관없는 광고 링크가 같은 작성자의 후기에 반복됩니다. 검토 부탁드립니다.",
        target: {
          kind: "reviews",
          id: "P-3004",
          title: "광고 링크가 반복되는 후기",
        },
      },
      {
        id: "Q-4003",
        category: "QUESTION",
        title: "제보한 행사 정보를 수정하고 싶어요",
        author: "동네탐험가",
        date: "2026-09-20",
        status: "PENDING",
        answer: "",
        content:
          "행사 장소가 변경되었습니다. 제가 제보한 행사 정보를 어디에서 수정할 수 있나요?",
      },
      {
        id: "Q-4002",
        category: "QUESTION",
        title: "회원 등급은 어디에서 확인하나요?",
        author: "가을바람",
        date: "2026-09-19",
        status: "ANSWERED",
        answer:
          "마이페이지의 내 정보에서 현재 회원 등급을 확인하실 수 있습니다.",
        content: "현재 제 회원 등급을 알고 싶어요.",
      },
      {
        id: "Q-4001",
        category: "REPORT",
        title: "같은 행사가 두 번 등록되어 있어요",
        author: "문화산책",
        date: "2026-09-18",
        status: "ANSWERED",
        answer:
          "중복 등록을 확인하여 해당 제보를 숨김 처리했습니다. 제보해 주셔서 감사합니다.",
        content: "동일한 장소와 일정의 꽃 축제가 중복으로 보입니다.",
        target: { kind: "events", id: "E-2002", title: "중복 등록된 꽃 축제" },
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
      data?.version === 1 &&
      ["members", "events", "reviews", "tickets", "activity"].every((k) =>
        Array.isArray(data[k]),
      )
    ) {
      // Keep existing preview edits while updating the built-in operator name.
      return {
        ...data,
        members: data.members.map((member: AdminMember) =>
          member.id === "M-1001" && member.role === "ROLE_ADMIN"
            ? { ...member, name: "방구석탈출 운영팀" }
            : member,
        ),
      };
    }
  } catch {
    /* An unavailable or older demo store starts with fresh examples. */
  }
  return seed();
}
interface AdminContextValue {
  data: AdminState;
  changeRole: (id: string, role: Role, reason: string) => void;
  moderate: (
    kind: "events" | "reviews",
    id: string,
    status: Visibility,
    reason: string,
  ) => void;
  answer: (id: string, answer: string) => void;
  reset: () => void;
}
const Context = createContext<AdminContextValue | null>(null);
export function AdminProvider({ children }: { children: ReactNode }) {
  const [data, setData] = useState(read);
  const [message, setMessage] = useState("");
  const [storageError, setStorageError] = useState(false);
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
  function commit(
    update: (current: AdminState) => AdminState,
    entry: Omit<Activity, "id" | "at">,
  ) {
    const log = {
      ...entry,
      id: crypto.randomUUID(),
      at: new Date().toISOString(),
    };
    setData((current) => ({
      ...update(current),
      activity: [log, ...current.activity],
    }));
    setMessage("Mock 데이터에 반영했습니다.");
  }
  return (
    <Context.Provider
      value={{
        data,
        changeRole(id, role, reason) {
          const member = data.members.find((item) => item.id === id);
          if (
            !member ||
            member.role === "ROLE_ADMIN" ||
            role === "ROLE_ADMIN" ||
            role === member.role ||
            !reason.trim()
          )
            return;
          commit(
            (current) => ({
              ...current,
              members: current.members.map((item) =>
                item.id === id ? { ...item, role } : item,
              ),
            }),
            {
              area: "회원",
              action: "등급 변경",
              target: member.name,
              reason: `${roleNames[member.role]} → ${roleNames[role]} · ${reason.trim()}`,
            },
          );
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
        answer(id, answer) {
          const ticket = data.tickets.find((item) => item.id === id);
          if (!ticket || ticket.status === "ANSWERED" || !answer.trim()) return;
          commit(
            (current) => ({
              ...current,
              tickets: current.tickets.map((item) =>
                item.id === id
                  ? { ...item, status: "ANSWERED", answer: answer.trim() }
                  : item,
              ),
            }),
            {
              area: "문의·신고",
              action: "답변 등록",
              target: ticket.id,
              reason: ticket.title,
            },
          );
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
