import { useEffect, useState, type ReactNode } from "react";
import {
  Link,
  NavLink,
  Outlet,
  useLocation,
  useSearchParams,
} from "react-router-dom";
import {
  Activity,
  ArrowDownLeft,
  ArrowRight,
  CalendarDays,
  Check,
  ChevronRight,
  ClipboardCheck,
  ExternalLink,
  Eye,
  Flag,
  LayoutDashboard,
  MessageSquare,
  RefreshCw,
  RotateCcw,
  Search,
  ShieldCheck,
  UsersRound,
} from "lucide-react";
import { Badge, Empty, Field, Logo, Modal } from "../components/ui";
import { RichTextContent } from "../components/RichText";
import { LazyRichTextEditor } from "../components/LazyRichTextEditor";
import { useApp, useLoad } from "../lib/context";
import { errorText, period, regions, safeUrl } from "../lib/format";
import type { AdminFestivalDetail, InquiryCategory, Role } from "../lib/types";
import { AdminGuard } from "./AdminAuth";
import {
  AdminProvider,
  roleNames,
  statusNames,
  useAdmin,
  type AdminMember,
  type ContentItem,
  type RunState,
  type Ticket,
  type Visibility,
} from "./store";
import "./admin.css";

const navigation = [
  { to: "/admin", label: "운영 대시보드", icon: LayoutDashboard },
  { to: "/admin/members", label: "회원 관리", icon: UsersRound },
  { to: "/admin/events", label: "행사 관리", icon: CalendarDays },
  { to: "/admin/reviews", label: "후기 관리", icon: MessageSquare },
  { to: "/admin/inquiries", label: "문의·신고·제보", icon: Flag },
  { to: "/admin/activity", label: "운영 기록", icon: Activity },
];
const tone = (status: string) =>
  status === "PUBLISHED" || status === "ANSWERED" || status === "ROLE_TRUSTED"
    ? "green"
    : status === "PENDING" || status === "ROLE_WARNING"
      ? "orange"
      : "gray";
const includes = (query: string, ...values: string[]) =>
  values.join(" ").toLowerCase().includes(query.trim().toLowerCase());
const ticketCategoryNames = {
  QUESTION: "일반 문의",
  REPORT: "신고",
  TIP: "제보",
} as const;
const ticketCategoryTone = (category: Ticket["category"]) =>
  category === "REPORT" ? "orange" : category === "TIP" ? "blue" : "gray";
const stamp = (date: string) =>
  new Date(date).toLocaleString("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });

export function AdminLayout() {
  // 토큰과 로그인 정보는 AdminRoot의 AppProvider가 들고 있다.
  // 가드를 통과해야(관리자 토큰이 있어야) AdminProvider가 어드민 API를 부른다.
  return (
    <AdminGuard>
      <AdminProvider>
        <AdminShell />
      </AdminProvider>
    </AdminGuard>
  );
}
function AdminShell() {
  const { reset, stats } = useAdmin();
  const [resetOpen, setResetOpen] = useState(false);
  const location = useLocation();
  // 서버가 준 전체 미처리 건수. 받아온 목록에서 세면 현재 페이지만 세게 된다.
  const pending = stats?.inquiryPending ?? 0;
  const title =
    navigation.find((item) => item.to === location.pathname)?.label ||
    "운영 대시보드";
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "instant" });
  }, [location.pathname]);
  return (
    <div className="adm-shell">
      <a className="skip-link" href="#admin-main">
        본문으로 바로가기
      </a>
      <aside className="adm-sidebar">
        <Link
          to="/admin"
          className="adm-brand"
          aria-label="방구석탈출 관리자 홈"
        >
          <Logo />
          <span>ADMIN CONSOLE</span>
        </Link>
        <div className="adm-workspace">
          <span className="adm-workspace-icon">
            <ShieldCheck size={20} />
          </span>
          <div>
            <strong>시스템 관리</strong>
            <small>방구석탈출 운영 워크스페이스</small>
          </div>
        </div>
        <p className="adm-nav-label">WORKSPACE</p>
        <nav aria-label="관리자 메뉴">
          {navigation.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} end={to === "/admin"}>
              <Icon size={19} />
              <span>{label}</span>
              {to === "/admin/inquiries" && pending > 0 && (
                <b>
                  {pending}
                  <span className="sr-only">건 미처리</span>
                </b>
              )}
            </NavLink>
          ))}
        </nav>
        <div className="adm-sidebar-bottom">
          <div className="adm-demo-note">
            <span className="adm-live-dot" />
            <strong>관리자 워크스페이스</strong>
            <p>회원·행사·후기·문의는 실제 서비스 데이터입니다.</p>
          </div>
          <Link to="/">
            <ArrowDownLeft size={17} />
            서비스로 돌아가기
          </Link>
        </div>
      </aside>
      <div className="adm-body">
        <header className="adm-topbar">
          <div className="adm-breadcrumb">
            <span>워크스페이스</span>
            <ChevronRight size={14} />
            <strong>{title}</strong>
          </div>
          <div className="adm-operator">
            <span className="adm-live-dot" />
            <span>시스템 관리자</span>
            <span className="adm-avatar small">A</span>
          </div>
        </header>
        <div className="adm-preview-strip">
          <span>
            <ShieldCheck size={15} />
            <strong>서비스 관리</strong>
            <span>목록 조회와 회원·문의 처리는 실제 서비스에 연결됩니다.</span>
          </span>
          <button onClick={() => setResetOpen(true)}>
            <RotateCcw size={14} />
            로컬 기록 초기화
          </button>
        </div>
        <main id="admin-main" className="adm-main">
          <Outlet />
        </main>
        <footer className="adm-footer">
          <Link to="/">방구석탈출 · 서비스로 돌아가기</Link>
          <span>운영 기록은 현재 탭에만 저장됩니다.</span>
        </footer>
      </div>
      {resetOpen && (
        <Modal
          title="로컬 기록을 초기화할까요?"
          onClose={() => setResetOpen(false)}
        >
          <p className="adm-dialog-copy">
            현재 탭의 예시 데이터와 운영 기록을 초기화합니다. 서버에 저장된
            회원·행사·후기·문의는 변경되지 않습니다.
          </p>
          <div className="adm-dialog-actions">
            <button
              className="btn secondary"
              onClick={() => setResetOpen(false)}
            >
              취소
            </button>
            <button
              className="btn primary"
              onClick={() => {
                reset();
                setResetOpen(false);
              }}
            >
              초기화
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}
function Heading({
  eyebrow,
  title,
  description,
  children,
}: {
  eyebrow: string;
  title: string;
  description: string;
  children?: ReactNode;
}) {
  return (
    <div className="adm-heading">
      <div>
        <span className="adm-eyebrow">{eyebrow}</span>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {children}
    </div>
  );
}
function PanelTitle({
  title,
  description,
  to,
}: {
  title: string;
  description?: string;
  to?: string;
}) {
  return (
    <div className="adm-panel-heading">
      <div>
        <h2>{title}</h2>
        {description && <p>{description}</p>}
      </div>
      {to && (
        <Link to={to}>
          전체 보기
          <ArrowRight size={15} />
        </Link>
      )}
    </div>
  );
}
function Status({ status }: { status: Visibility | RunState }) {
  return <Badge tone={tone(status)}>{statusNames[status]}</Badge>;
}
/**
 * 서버 페이징 이동. 관리자는 "전체 몇 건 중 몇 페이지"를 알아야 해서
 * 더보기 대신 페이지 번호로 둔다.
 */
function Pagination({
  page,
  totalPages,
  onChange,
}: {
  /** 0부터 시작 */
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;
  // 현재 페이지 주변 5개만 보여준다. 3000건이면 150페이지라 다 그릴 수 없다.
  const start = Math.max(0, Math.min(page - 2, totalPages - 5));
  const numbers = Array.from(
    { length: Math.min(5, totalPages) },
    (_, index) => start + index,
  );
  return (
    <nav className="adm-pagination" aria-label="페이지 이동">
      <button
        type="button"
        disabled={page === 0}
        onClick={() => onChange(page - 1)}
      >
        이전
      </button>
      {numbers.map((value) => (
        <button
          key={value}
          type="button"
          className={value === page ? "active" : ""}
          aria-current={value === page ? "page" : undefined}
          onClick={() => onChange(value)}
        >
          {value + 1}
        </button>
      ))}
      <button
        type="button"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        다음
      </button>
    </nav>
  );
}
function Table({
  label,
  headers,
  empty,
  children,
}: {
  label: string;
  headers: string[];
  empty: boolean;
  children: ReactNode;
}) {
  return empty ? (
    <Empty
      title="조건에 맞는 결과가 없습니다"
      description="검색어나 필터를 변경해 주세요."
    />
  ) : (
    <div
      className="adm-table-scroll"
      role="region"
      aria-label={label}
      tabIndex={0}
    >
      <table className="adm-table">
        <thead>
          <tr>
            {headers.map((h) => (
              <th scope="col" key={h}>
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>{children}</tbody>
      </table>
    </div>
  );
}
function Toolbar({
  query,
  setQuery,
  placeholder,
  children,
}: {
  query: string;
  setQuery: (query: string) => void;
  placeholder: string;
  children?: ReactNode;
}) {
  return (
    <div className="adm-toolbar">
      <label className="adm-search">
        <Search size={17} />
        <input
          type="search"
          aria-label={placeholder}
          placeholder={placeholder}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </label>
      <div className="adm-filters">{children}</div>
    </div>
  );
}
function Tabs({
  value,
  onChange,
  options,
  action,
}: {
  value: string;
  onChange: (value: string) => void;
  /** count는 선택이다. 서버 페이징이면 현재 페이지만 세게 되어 틀린 숫자가 된다. */
  options: { value: string; label: string; count?: number }[];
  /** 탭 줄 오른쪽 끝에 붙는 액션. 상태 필터가 아니므로 group 밖에 둔다. */
  action?: ReactNode;
}) {
  return (
    <div className="adm-tab-row">
      <div className="adm-tabs" role="group" aria-label="상태 필터">
        {options.map((option) => (
          <button
            key={option.value}
            className={value === option.value ? "active" : ""}
            aria-pressed={value === option.value}
            onClick={() => onChange(option.value)}
          >
            {option.label}
            {option.count !== undefined && <span>{option.count}</span>}
          </button>
        ))}
      </div>
      {action}
    </div>
  );
}
function DetailLine({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  return (
    <div className="adm-detail-line">
      <dt>{label}</dt>
      <dd>{children}</dd>
    </div>
  );
}
function Reason({
  value,
  onChange,
  disabled = false,
}: {
  value: string;
  onChange: (value: string) => void;
  /** 저장할 곳이 없어 잠가 둔 경우. 입력은 막고 자리만 보여준다. */
  disabled?: boolean;
}) {
  return (
    <Field label="처리 사유" required={!disabled}>
      <textarea
        required={!disabled}
        disabled={disabled}
        maxLength={500}
        rows={3}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={
          disabled
            ? "사유 기록 기능은 준비 중입니다."
            : "운영 기록에 남길 사유를 입력해 주세요."
        }
      />
    </Field>
  );
}

export function AdminDashboard() {
  const { data, stats, statsLoading, statsError } = useAdmin();
  const pending = data.tickets.filter((t) => t.status === "PENDING");
  // 카드 숫자는 서버가 준 전체 건수다. 목록은 한 페이지만 받아오므로 길이를 세면 안 된다.
  const cards = [
    {
      label: "전체 회원",
      value: stats?.memberTotal,
      unit: "명",
      detail:
        stats &&
        `신뢰 ${stats.memberTrusted}명 · 주의 ${stats.memberWarning}명`,
      icon: UsersRound,
      to: "/admin/members",
      color: "sage",
    },
    {
      label: "등록된 행사",
      value: stats?.festivalTotal,
      unit: "건",
      detail: stats && `진행중 ${stats.festivalOpen}건`,
      icon: CalendarDays,
      to: "/admin/events",
      color: "peach",
    },
    {
      label: "행사 후기",
      value: stats?.postTotal,
      unit: "건",
      // 후기에는 아직 노출 상태가 없어 공개/숨김을 나눌 수 없다.
      detail: "삭제된 후기 포함",
      icon: MessageSquare,
      to: "/admin/reviews",
      color: "blue",
    },
    {
      label: "미처리 문의·신고·제보",
      value: stats?.inquiryPending,
      unit: "건",
      detail:
        stats && `신고 ${stats.inquiryReport}건 · 제보 ${stats.inquiryTip}건`,
      icon: Flag,
      to: "/admin/inquiries?status=PENDING",
      color: "orange",
    },
  ];
  return (
    <>
      <Heading
        eyebrow="OVERVIEW"
        title="운영 대시보드"
        description="서비스 전반의 현황을 살피고, 필요한 조치를 한곳에서 처리하세요."
      >
        <span className="adm-date">
          <CalendarDays size={15} />
          {/* 고정 문구였다. 실제로 언제 집계한 값인지 보여야 한다. */}
          {new Intl.DateTimeFormat("ko-KR", {
            timeZone: "Asia/Seoul",
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
          }).format(new Date())}{" "}
          기준
        </span>
      </Heading>
      <section className="adm-welcome">
        <div>
          <span className="adm-eyebrow">BETTER EXPERIENCES, TOGETHER</span>
          <h2>좋은 경험이 이어지는 공간을 만듭니다.</h2>
          <p>
            {stats ? (
              <>
                확인이 필요한 문의·신고·제보{" "}
                <strong>{stats.inquiryPending}건</strong>이 있습니다.
                {stats.inquiryReport + stats.inquiryTip > 0 &&
                  ` 그중 신고 ${stats.inquiryReport}건, 제보 ${stats.inquiryTip}건입니다.`}
              </>
            ) : (
              "처리할 업무를 불러오는 중입니다."
            )}
          </p>
          <Link to="/admin/inquiries?status=PENDING">
            처리할 업무 확인하기
            <ArrowRight size={17} />
          </Link>
        </div>
        <div className="adm-welcome-art" aria-hidden="true">
          <div />
          <ShieldCheck size={64} />
          <span>
            <Check size={17} /> TRUST & SAFETY
          </span>
        </div>
      </section>
      {statsError && (
        <p className="adm-dialog-note" role="alert">
          현황을 불러오지 못했습니다. {statsError}
        </p>
      )}
      <section className="adm-stats" aria-label="서비스 현황">
        {cards.map(({ label, value, unit, detail, icon: Icon, to, color }) => (
          <Link className="adm-stat" to={to} key={label}>
            <div>
              <span>{label}</span>
              <span className={`adm-stat-icon ${color}`}>
                <Icon size={20} />
              </span>
            </div>
            <p>
              {/* 아직 못 받았으면 0을 보여주면 안 된다. 0건으로 오해한다. */}
              <strong>{value ?? (statsLoading ? "—" : "?")}</strong>
              <span>{unit}</span>
            </p>
            <div className="adm-stat-bottom">
              <span>{detail || ""}</span>
              <ArrowRight size={15} />
            </div>
          </Link>
        ))}
      </section>
      <div className="adm-dashboard-grid">
        <section className="adm-panel">
          <PanelTitle
            title="우선 확인할 문의·신고·제보"
            description="답변을 기다리는 이용자의 목소리입니다."
            to="/admin/inquiries?status=PENDING"
          />
          {pending.length ? (
            <div className="adm-priority-list">
              {pending.map((ticket) => (
                <Link key={ticket.id} to={`/admin/inquiries?item=${ticket.id}`}>
                  <span
                    className={`adm-ticket-icon ${ticket.category.toLowerCase()}`}
                  >
                    {ticket.category === "REPORT" ? (
                      <Flag size={18} />
                    ) : ticket.category === "TIP" ? (
                      <ClipboardCheck size={18} />
                    ) : (
                      <MessageSquare size={18} />
                    )}
                  </span>
                  <div>
                    <span className="adm-item-meta">
                      {ticketCategoryNames[ticket.category]} · {ticket.id}
                    </span>
                    <h3>{ticket.title}</h3>
                    <p>
                      {ticket.author} · {ticket.date}
                    </p>
                  </div>
                  <ChevronRight size={18} />
                </Link>
              ))}
            </div>
          ) : (
            <Empty title="대기 중인 문의가 없습니다" icon={<Check />} />
          )}
        </section>
        <section className="adm-panel">
          <PanelTitle
            title="콘텐츠 현황"
            description="서비스에 등록된 전체 건수입니다."
          />
          <div className="adm-content-summary">
            {/*
              원래 공개/검토 대기/숨김 비율을 보여주던 자리다.
              행사·후기에 노출 상태 컬럼이 없어 그 숫자를 만들 수 없으므로
              지금은 진행 상태와 전체 건수만 보여준다.
              TODO visibility 컬럼이 생기면 공개·숨김 비율로 되돌릴 것.
            */}
            {[
              {
                name: "진행중인 행사",
                count: stats?.festivalOpen ?? 0,
                max: stats?.festivalTotal ?? 0,
                color: "green",
              },
              {
                name: "신뢰 등급 회원",
                count: stats?.memberTrusted ?? 0,
                max: stats?.memberTotal ?? 0,
                color: "green",
              },
              {
                name: "주의 등급 회원",
                count: stats?.memberWarning ?? 0,
                max: stats?.memberTotal ?? 0,
                color: "orange",
              },
            ].map((item) => (
              <div key={item.name}>
                <div>
                  <span>{item.name}</span>
                  <strong>
                    {item.count}
                    <small> / {item.max}건</small>
                  </strong>
                </div>
                <div className={`adm-progress ${item.color}`}>
                  <span
                    style={{
                      width: `${(item.count / Math.max(1, item.max)) * 100}%`,
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
          {/* 예전 링크(?status=PENDING)는 이제 없는 탭이라 전체 목록으로 보낸다. */}
          <Link className="adm-review-callout" to="/admin/events">
            <ClipboardCheck size={23} />
            <span>
              <strong>행사 목록 확인</strong>
              <small>수집된 행사와 회원 제보를 검색해 보세요.</small>
            </span>
            <ArrowRight size={17} />
          </Link>
        </section>
      </div>
      <section className="adm-panel adm-recent">
        <PanelTitle
          title="최근 운영 기록"
          description="관리자의 변경 사항을 확인하세요."
          to="/admin/activity"
        />
        <div className="adm-activity-list">
          {data.activity.slice(0, 4).map((log) => (
            <div key={log.id}>
              <span className="adm-log-icon">
                <Activity size={17} />
              </span>
              <div>
                <strong>{log.action}</strong>
                <span>{log.target}</span>
                <p>{log.reason}</p>
              </div>
              <time dateTime={log.at}>{stamp(log.at)}</time>
            </div>
          ))}
        </div>
      </section>
    </>
  );
}

export function AdminMembers() {
  const { data, membersLoading, membersError, reloadMembers, stats } =
    useAdmin();
  const [query, setQuery] = useState("");
  const [role, setRole] = useState("");
  const [selected, setSelected] = useState<string | null>(null);
  const items = data.members.filter(
    (item) =>
      includes(query, item.name, item.email, item.id) &&
      (!role || item.role === role),
  );
  const member = data.members.find((item) => item.id === selected);
  return (
    <>
      <Heading
        eyebrow="MEMBERS"
        title="회원 관리"
        description="전체 회원의 가입 정보와 신뢰 등급을 관리합니다."
      />
      {/* 서버가 준 전체 건수. 목록은 한 페이지만 받아오므로 길이를 세면 안 된다. */}
      <div className="adm-summary-line">
        <span>
          <UsersRound size={18} />
          전체 회원 <strong>{stats ? `${stats.memberTotal}명` : "—"}</strong>
        </span>
        <span>
          주의 등급 <strong>{stats ? `${stats.memberWarning}명` : "—"}</strong>
        </span>
      </div>
      <section className="adm-panel">
        <Toolbar
          query={query}
          setQuery={setQuery}
          placeholder="닉네임, 이메일, 회원 ID 검색"
        >
          <select
            aria-label="회원 등급 필터"
            value={role}
            onChange={(e) => setRole(e.target.value)}
          >
            <option value="">모든 등급</option>
            {Object.entries(roleNames).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </Toolbar>
        <div className="adm-result-count">
          {membersLoading ? (
            "불러오는 중…"
          ) : (
            <>
              검색 결과 <strong>{items.length}</strong>명
            </>
          )}
        </div>
        {membersError && (
          <p className="adm-dialog-note" role="alert">
            {membersError}{" "}
            <button
              type="button"
              className="adm-row-button"
              onClick={reloadMembers}
            >
              다시 시도
            </button>
          </p>
        )}
        <Table
          label="회원 목록"
          headers={["회원", "이메일", "신뢰 등급", "가입일", "관리"]}
          empty={!items.length}
        >
          {items.map((member) => (
            <tr key={member.id}>
              <td>
                <div className="adm-person">
                  {member.profileImg ? (
                    <img
                      className="adm-avatar"
                      src={member.profileImg}
                      alt=""
                    />
                  ) : (
                    <span
                      className={`adm-avatar ${member.role === "ROLE_ADMIN" ? "admin" : ""}`}
                    >
                      {member.name[0]}
                    </span>
                  )}
                  <div>
                    <strong>
                      {member.name}
                      {member.deletedAt && " (탈퇴)"}
                    </strong>
                    <small>{member.id}</small>
                  </div>
                </div>
              </td>
              <td>{member.email}</td>
              <td>
                <Badge tone={tone(member.role)}>{roleNames[member.role]}</Badge>
              </td>
              <td>{member.joined}</td>
              <td>
                <button
                  className="adm-row-button"
                  aria-label={`${member.name} 회원 관리`}
                  onClick={() => setSelected(member.id)}
                >
                  상세 관리
                  <ChevronRight size={14} />
                </button>
              </td>
            </tr>
          ))}
        </Table>
      </section>
      {member && (
        <MemberDialog member={member} onClose={() => setSelected(null)} />
      )}
    </>
  );
}
function MemberDialog({
  member,
  onClose,
}: {
  member: AdminMember;
  onClose: () => void;
}) {
  const { changeRole } = useAdmin();
  const { member: signedInAdmin } = useApp();
  const [role, setRole] = useState<Role>(member.role);
  const [reason, setReason] = useState("");
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState("");
  const isAdmin = member.role === "ROLE_ADMIN";
  const isSelf = signedInAdmin?.id === member.id;
  const isDeleted = !!member.deletedAt;
  // 서버가 막는 조건과 같다. 여기서 먼저 걸러 헛된 요청을 줄인다.
  const blocked = isAdmin
    ? "관리자 계정의 등급은 변경할 수 없습니다."
    : isSelf
      ? "본인의 등급은 변경할 수 없습니다."
      : isDeleted
        ? "탈퇴한 회원의 등급은 변경할 수 없습니다."
        : "";
  return (
    <Modal title="회원 상세 관리" onClose={onClose}>
      <dl className="adm-details">
        <DetailLine label="닉네임">{member.name}</DetailLine>
        <DetailLine label="이메일">{member.email}</DetailLine>
        <DetailLine label="회원 ID">{member.id}</DetailLine>
        <DetailLine label="가입일">{member.joined}</DetailLine>
        <DetailLine label="현재 등급">
          <Badge tone={tone(member.role)}>{roleNames[member.role]}</Badge>
        </DetailLine>
      </dl>
      {blocked ? (
        <p className="adm-dialog-note">{blocked}</p>
      ) : (
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            setSaving(true);
            setSaveError("");
            try {
              await changeRole(member.id, role, reason);
              onClose();
            } catch (error) {
              // 실패하면 닫지 않는다. 고른 등급이 남아 있어야 다시 시도한다.
              setSaveError(errorText(error));
            } finally {
              setSaving(false);
            }
          }}
        >
          <Field label="변경할 등급">
            <select
              value={role}
              onChange={(e) => setRole(e.target.value as Role)}
            >
              {Object.entries(roleNames)
                .filter(([key]) => key !== "ROLE_ADMIN")
                .map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
            </select>
          </Field>
          {/*
            TODO 사유 입력칸 살리기.
                 저장할 이력 테이블이 없어서 지금은 잠가 두고 등급만 바꾼다.
                 서버에 PATCH /admin/members/{id}/role 의 body로 reason을 받는
                 이력 테이블이 생기면 disabled를 떼고 required로 되돌릴 것.
                 백엔드 쪽 TODO는 Member.changeRole() 과 AdminMemberService 참고.
          */}
          <Reason value={reason} onChange={setReason} disabled />
          {role === "ROLE_WARNING" && (
            <p className="adm-dialog-note">
              주의 등급은 행사 등록 등 주요 기능이 제한되는 등급입니다.
            </p>
          )}
          {saveError && (
            <p className="adm-dialog-note" role="alert">
              {saveError}
            </p>
          )}
          <div className="adm-dialog-actions">
            <button type="button" className="btn secondary" onClick={onClose}>
              취소
            </button>
            <button
              className="btn primary"
              disabled={role === member.role || saving}
            >
              {saving ? "변경 중…" : "등급 변경"}
            </button>
          </div>
        </form>
      )}
    </Modal>
  );
}

/**
 * 공공 행사 수동 동기화 버튼.
 *
 * 바꾸는 건 유입 경로가 '지역 문화행사'인 행사뿐이고 회원 제보는 건드리지 않는다.
 * 그래서 라벨에 '공공'을 붙여 범위를 드러낸다.
 */
function SyncButton() {
  const { api, toast } = useApp();
  const [running, setRunning] = useState(false);
  return (
    <button
      type="button"
      className="adm-tab-action"
      // 공공 API를 통째로 훑어서 오래 걸린다. 연타하면 서버가 409로 튕긴다.
      disabled={running}
      onClick={async () => {
        setRunning(true);
        try {
          const result = await api.syncFestivals();
          // 무엇이 바뀌었는지 알려주지 않으면 눌러도 결과를 알 수 없다.
          toast(
            `동기화 완료 · 새 행사 ${result.savedFestivals.length}건 저장, ${result.closedFestivals.length}건 종료 처리`,
          );
        } catch (error) {
          toast(errorText(error));
        } finally {
          setRunning(false);
        }
      }}
    >
      {/* .spin 은 styles.css 전역 유틸이다. */}
      <RefreshCw size={14} className={running ? "spin" : ""} />
      {running ? "동기화 중…" : "공공 행사 동기화"}
    </button>
  );
}
export function AdminContent({ kind }: { kind: "events" | "reviews" }) {
  const {
    data,
    eventsLoading,
    eventsError,
    reloadEvents,
    eventsPage,
    eventQuery,
    setEventQuery,
  } = useAdmin();
  const [params, setParams] = useSearchParams();
  const [query, setQuery] = useState("");
  const [source, setSource] = useState("");
  // 삭제된 행사는 기본적으로 숨긴다. 복구할 때만 켜서 본다.
  const [showDeleted, setShowDeleted] = useState(false);
  const status = params.get("status") || "";
  const isEvent = kind === "events";
  const records = data[kind];
  /*
    행사는 서버가 진행 상태를 계산해서 내려준다.
    검색 파라미터에는 excludeClosed(종료 제외)만 있고 "종료만 보기"가 없어서
    탭도 [전체][진행중]까지만 둔다.
    TODO 서버에 status 필터가 생기면 [종료] 탭을 되살릴 것.
  */
  const statuses: (Visibility | RunState)[] = isEvent
    ? ["OPEN"]
    : ["PUBLISHED", "HIDDEN"];
  /**
   * 행사는 검색·필터를 서버가 한다. 타이핑할 때마다 요청이 나가지 않도록
   * 잠깐 멈춘 뒤에 보낸다.
   */
  useEffect(() => {
    if (!isEvent) return;
    const timer = setTimeout(() => {
      setEventQuery({
        keyword: query.trim() || undefined,
        providerType: (source as "PUBLIC" | "MEMBER") || undefined,
        excludeClosed: status === "OPEN",
        includeDeleted: showDeleted,
      });
    }, 300);
    return () => clearTimeout(timer);
  }, [isEvent, query, source, status, showDeleted, setEventQuery]);
  // 행사는 서버가 이미 걸러서 보냈다. 후기는 아직 예시 데이터라 화면에서 거른다.
  const items = isEvent
    ? records
    : records.filter(
        (item) =>
          includes(query, item.title, item.author, item.id) &&
          (!status || item.status === status) &&
          (!source || item.source === source),
      );
  const selected = records.find((item) => item.id === params.get("item"));
  function setParam(key: string, value: string) {
    setParams((current) => {
      const next = new URLSearchParams(current);
      value ? next.set(key, value) : next.delete(key);
      return next;
    });
  }
  return (
    <>
      <Heading
        eyebrow={isEvent ? "EVENTS" : "REVIEWS"}
        title={isEvent ? "행사 관리" : "후기 관리"}
        description={
          isEvent
            ? "수집된 문화행사와 회원 제보를 검토하고 서비스 노출을 관리합니다."
            : "서비스 전체 후기를 살피고 부적절한 콘텐츠를 관리합니다."
        }
      />
      <section className="adm-panel">
        <Tabs
          value={status}
          onChange={(v) => setParam("status", v)}
          options={[
            // 행사는 서버 페이징이라 화면에 있는 건 현재 페이지뿐이다.
            // 여기서 세면 "전체 20건"처럼 틀린 숫자가 나오므로 건수를 빼고,
            // 총계는 아래 결과 줄에서 서버가 준 값으로 보여준다.
            {
              value: "",
              label: "전체",
              count: isEvent ? undefined : records.length,
            },
            ...statuses.map((value) => ({
              value,
              label: statusNames[value],
              count: isEvent
                ? undefined
                : records.filter((r) => r.status === value).length,
            })),
          ]}
          // 후기 탭에는 동기화할 공공 데이터가 없다.
          action={isEvent && <SyncButton />}
        />
        <Toolbar
          query={query}
          setQuery={setQuery}
          placeholder={
            isEvent
              ? "행사명, 제보자, 행사 ID 검색"
              : "후기 제목, 작성자, 후기 ID 검색"
          }
        >
          {isEvent && (
            <select
              aria-label="행사 유입 경로"
              value={source}
              onChange={(e) => setSource(e.target.value)}
            >
              <option value="">모든 유입 경로</option>
              <option value="PUBLIC">지역 문화행사</option>
              <option value="MEMBER">회원 제보</option>
            </select>
          )}
          {isEvent && (
            <label className="adm-toggle">
              <input
                type="checkbox"
                checked={showDeleted}
                onChange={(e) => setShowDeleted(e.target.checked)}
              />
              삭제된 행사 포함
            </label>
          )}
        </Toolbar>
        <div className="adm-result-count">
          {isEvent && eventsLoading ? (
            "불러오는 중…"
          ) : isEvent ? (
            // 서버가 준 전체 건수. 화면에 보이는 20건이 아니다.
            <>
              검색 결과 <strong>{eventsPage.totalElements}</strong>건
              {eventsPage.totalPages > 1 && (
                <span className="adm-page-hint">
                  {" "}
                  · {eventsPage.number + 1} / {eventsPage.totalPages} 페이지
                </span>
              )}
            </>
          ) : (
            <>
              검색 결과 <strong>{items.length}</strong>건
            </>
          )}
        </div>
        {isEvent && eventsError && (
          <p className="adm-dialog-note" role="alert">
            {eventsError}{" "}
            <button
              type="button"
              className="adm-row-button"
              onClick={reloadEvents}
            >
              다시 시도
            </button>
          </p>
        )}
        <Table
          label={isEvent ? "행사 목록" : "후기 목록"}
          headers={[
            isEvent ? "행사 정보" : "후기 / 행사",
            isEvent ? "유입 경로" : "작성자",
            "노출 상태",
            "등록일",
            "관리",
          ]}
          empty={!items.length}
        >
          {items.map((item) => (
            <tr key={item.id}>
              <td>
                <div className="adm-content-cell">
                  {isEvent && (
                    <div className="adm-thumbnail">
                      {item.image ? (
                        <img src={item.image} alt="" />
                      ) : (
                        <CalendarDays size={20} />
                      )}
                    </div>
                  )}
                  <div>
                    <strong>
                      {item.title}
                      {item.deletedAt && " (삭제됨)"}
                    </strong>
                    <small>
                      {item.id} · {item.category}
                    </small>
                  </div>
                </div>
              </td>
              <td>
                {isEvent ? (
                  <span className="adm-source">
                    {item.source === "PUBLIC" ? "지역 문화행사" : "회원 제보"}
                  </span>
                ) : (
                  item.author
                )}
              </td>
              <td>
                <Status status={item.status} />
              </td>
              <td>{item.date}</td>
              <td>
                <button
                  className="adm-row-button"
                  aria-label={`${item.title} 검토`}
                  onClick={() => setParam("item", item.id)}
                >
                  검토하기
                  <ChevronRight size={14} />
                </button>
              </td>
            </tr>
          ))}
        </Table>
        {isEvent && (
          <Pagination
            page={eventsPage.number}
            totalPages={eventsPage.totalPages}
            onChange={(next) => setEventQuery({ ...eventQuery, page: next })}
          />
        )}
      </section>
      <p className="adm-footnote">
        {isEvent
          ? "진행 상태는 행사 종료일로 계산된 값입니다. 노출 관리(숨김·복구) 기능은 준비 중입니다."
          : "공개·숨김은 관리자 화면의 예시 운영 상태입니다."}
      </p>
      {selected && (
        <ContentDialog
          key={selected.id}
          item={selected}
          kind={kind}
          onClose={() => setParam("item", "")}
        />
      )}
    </>
  );
}
/**
 * 행사 상세 본문. 목록에 없는 소개·기간·장소·연락처를 보여준다.
 * 관리자가 이 행사를 조치할지 판단하려면 목록의 제목만으로는 부족하다.
 */
function EventDetailBody({
  loading,
  error,
  detail,
}: {
  loading: boolean;
  error: string;
  detail: AdminFestivalDetail | null;
}) {
  if (loading) return <p className="adm-content-body">불러오는 중…</p>;
  if (error)
    return (
      <p className="adm-content-body" role="alert">
        {error}
      </p>
    );
  if (!detail) return null;
  const place = [detail.region, detail.regionDetail]
    .filter(Boolean)
    .join(" ")
    .trim();
  const rows = [
    { label: "기간", value: period(detail.beginDe, detail.endDe) },
    { label: "장소", value: place },
    { label: "운영 시간", value: detail.eventTmInfo },
    { label: "참가비", value: detail.partcptExpnInfo },
    { label: "주최", value: detail.hostInstNm || detail.instNm },
    { label: "문의", value: detail.telnoInfo },
  ].filter((row) => row.value);
  return (
    <>
      <RichTextContent
        content={
          detail.festivalContent?.trim() || "등록된 행사 소개가 없습니다."
        }
        className="adm-content-body rich-text-content"
      />
      {rows.length > 0 && (
        <dl className="adm-details">
          {rows.map((row) => (
            <DetailLine key={row.label} label={row.label}>
              {row.value}
            </DetailLine>
          ))}
        </dl>
      )}
      {detail.referenceUrl && (
        <a
          className="adm-target-link"
          href={safeUrl(detail.referenceUrl)}
          target="_blank"
          rel="noreferrer noopener"
        >
          <ExternalLink size={18} />
          <span>
            <small>원문 보기</small>
            <strong>{detail.referenceUrl}</strong>
          </span>
        </a>
      )}
    </>
  );
}
/** "2026-10-01T10:00:00" → datetime-local 입력이 받는 "2026-10-01T10:00" */
const toLocalInput = (value: string) => value.slice(0, 16);
/** 빈 입력은 빈 문자열이 아니라 null로 보낸다. DB에 ""이 쌓이면 "없음"이 두 가지가 된다. */
const orNull = (value: string) => value.trim() || null;
/**
 * 행사 수정 폼.
 *
 * 공공 API 데이터의 오기(장소·날짜·연락처)를 관리자가 바로잡는 용도다.
 * 서버가 보낸 값을 전부 덮어쓰므로 상세를 받아 채운 뒤 통째로 보낸다.
 */
function EventEditForm({
  festivalId,
  detail,
  onDone,
  onCancel,
}: {
  festivalId: string;
  detail: AdminFestivalDetail;
  onDone: () => void;
  onCancel: () => void;
}) {
  const { api, toast } = useApp();
  const { reloadEvents } = useAdmin();
  const [form, setForm] = useState({
    title: detail.title,
    category: detail.category,
    festivalContent: detail.festivalContent ?? "",
    region: detail.region,
    regionDetail: detail.regionDetail ?? "",
    beginDe: toLocalInput(detail.beginDe),
    endDe: toLocalInput(detail.endDe),
    eventTmInfo: detail.eventTmInfo ?? "",
    partcptExpnInfo: detail.partcptExpnInfo ?? "",
    telnoInfo: detail.telnoInfo ?? "",
    instNm: detail.instNm ?? "",
    hostInstNm: detail.hostInstNm ?? "",
    referenceUrl: detail.referenceUrl ?? "",
    imgUrl: detail.imgUrl ?? "",
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const set = (key: keyof typeof form) => (value: string) =>
    setForm((current) => ({ ...current, [key]: value }));
  // 서버도 막지만 저장 버튼을 눌러보기 전에 알려주는 편이 낫다.
  const badPeriod = !!form.beginDe && !!form.endDe && form.endDe < form.beginDe;
  return (
    <form
      onSubmit={async (e) => {
        e.preventDefault();
        setSaving(true);
        setError("");
        try {
          await api.updateFestival(festivalId, {
            title: form.title.trim(),
            category: form.category.trim(),
            region: form.region,
            // datetime-local은 초를 빼고 주므로 서버 형식에 맞춰 붙인다.
            beginDe: `${form.beginDe}:00`,
            endDe: `${form.endDe}:00`,
            festivalContent: orNull(form.festivalContent),
            regionDetail: orNull(form.regionDetail),
            eventTmInfo: orNull(form.eventTmInfo),
            partcptExpnInfo: orNull(form.partcptExpnInfo),
            telnoInfo: orNull(form.telnoInfo),
            instNm: orNull(form.instNm),
            hostInstNm: orNull(form.hostInstNm),
            referenceUrl: orNull(form.referenceUrl),
            imgUrl: orNull(form.imgUrl),
          });
          // 제목·기간이 바뀌면 목록에도 반영돼야 한다.
          reloadEvents();
          toast("행사 정보를 수정했습니다.");
          onDone();
        } catch (saveError) {
          // 실패하면 닫지 않는다. 입력한 내용이 남아 있어야 다시 시도한다.
          setError(errorText(saveError));
        } finally {
          setSaving(false);
        }
      }}
    >
      <Field label="행사명" required>
        <input
          required
          maxLength={255}
          value={form.title}
          onChange={(e) => set("title")(e.target.value)}
        />
      </Field>
      <Field label="카테고리" required>
        <input
          required
          maxLength={50}
          value={form.category}
          onChange={(e) => set("category")(e.target.value)}
        />
      </Field>
      <Field label="행사 소개">
        <LazyRichTextEditor
          value={form.festivalContent}
          onChange={set("festivalContent")}
          placeholder="행사 상세 페이지에 노출되는 소개 글입니다."
          ariaLabel="행사 소개"
        />
      </Field>
      <Field label="지역" required>
        <select
          required
          value={form.region}
          onChange={(e) => set("region")(e.target.value)}
        >
          {Object.entries(regions).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </Field>
      <Field label="상세 주소">
        <input
          maxLength={255}
          value={form.regionDetail}
          onChange={(e) => set("regionDetail")(e.target.value)}
        />
      </Field>
      <Field label="시작 일시" required>
        <input
          type="datetime-local"
          required
          value={form.beginDe}
          onChange={(e) => set("beginDe")(e.target.value)}
        />
      </Field>
      <Field label="종료 일시" required>
        <input
          type="datetime-local"
          required
          value={form.endDe}
          onChange={(e) => set("endDe")(e.target.value)}
        />
      </Field>
      {badPeriod && (
        <p className="adm-dialog-note" role="alert">
          종료 일시가 시작 일시보다 앞설 수 없습니다.
        </p>
      )}
      <Field label="운영 시간">
        <input
          maxLength={255}
          value={form.eventTmInfo}
          onChange={(e) => set("eventTmInfo")(e.target.value)}
          placeholder="10:00~18:00"
        />
      </Field>
      <Field label="참가비">
        <input
          maxLength={255}
          value={form.partcptExpnInfo}
          onChange={(e) => set("partcptExpnInfo")(e.target.value)}
          placeholder="무료"
        />
      </Field>
      <Field label="기관명">
        <input
          maxLength={255}
          value={form.instNm}
          onChange={(e) => set("instNm")(e.target.value)}
        />
      </Field>
      <Field label="주최 기관">
        <input
          maxLength={255}
          value={form.hostInstNm}
          onChange={(e) => set("hostInstNm")(e.target.value)}
        />
      </Field>
      <Field label="문의 전화">
        <input
          maxLength={255}
          value={form.telnoInfo}
          onChange={(e) => set("telnoInfo")(e.target.value)}
        />
      </Field>
      <Field label="참고 링크">
        <input
          type="url"
          maxLength={2048}
          value={form.referenceUrl}
          onChange={(e) => set("referenceUrl")(e.target.value)}
        />
      </Field>
      <Field label="이미지 URL">
        <input
          type="url"
          maxLength={2048}
          value={form.imgUrl}
          onChange={(e) => set("imgUrl")(e.target.value)}
        />
      </Field>
      <p className="adm-dialog-note">
        데이터 출처와 진행 상태는 바꿀 수 없습니다. 진행 상태는 종료 일시로 다시
        계산됩니다.
      </p>
      {error && (
        <p className="adm-dialog-note" role="alert">
          {error}
        </p>
      )}
      <div className="adm-dialog-actions">
        <button type="button" className="btn secondary" onClick={onCancel}>
          취소
        </button>
        <button className="btn primary" disabled={saving || badPeriod}>
          {saving ? "저장 중…" : "수정 저장"}
        </button>
      </div>
    </form>
  );
}
/**
 * 행사 상세 모달의 조치 버튼들.
 *
 * 삭제는 되돌릴 수 있지만(소프트 삭제) 한 번 더 확인을 받는다.
 * 목록에서 사라지므로 실수로 누르면 바로 알아채기 어렵기 때문이다.
 */
function EventActions({
  item,
  canEdit,
  onEdit,
  onClose,
}: {
  item: ContentItem;
  canEdit: boolean;
  onEdit: () => void;
  onClose: () => void;
}) {
  const { deleteEvent, restoreEvent } = useAdmin();
  const { toast } = useApp();
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);
  const deleted = !!item.deletedAt;
  // 회원이 직접 지웠을 수 있어 서버가 회원 제보 복구를 막는다.
  const canRestore = deleted && item.source === "PUBLIC";
  async function run(action: () => Promise<void>) {
    setBusy(true);
    try {
      await action();
      onClose();
    } catch (error) {
      toast(errorText(error));
    } finally {
      setBusy(false);
    }
  }
  if (confirming) {
    return (
      <>
        <p className="adm-dialog-note" role="alert">
          <strong>{item.title}</strong>을(를) 삭제하면 서비스 목록과 검색에서
          사라집니다. 이 행사에 달린 후기는 지워지지 않으며, 나중에 복구할 수
          있습니다.
        </p>
        <div className="adm-dialog-actions">
          <button
            type="button"
            className="btn secondary"
            onClick={() => setConfirming(false)}
          >
            취소
          </button>
          <button
            type="button"
            className="btn danger"
            disabled={busy}
            onClick={() => run(() => deleteEvent(item.id))}
          >
            {busy ? "삭제 중…" : "삭제합니다"}
          </button>
        </div>
      </>
    );
  }
  return (
    <>
      <p className="adm-dialog-note">
        {deleted
          ? canRestore
            ? "삭제된 행사입니다. 복구하면 다시 서비스에 노출됩니다."
            : "삭제된 회원 제보입니다. 작성자가 지웠을 수 있어 복구할 수 없습니다."
          : "노출 관리(숨김) 기능은 준비 중입니다. 현재 상태는 종료일로 계산된 진행 상태입니다."}
      </p>
      <div className="adm-dialog-actions">
        <button className="btn secondary" type="button" onClick={onClose}>
          닫기
        </button>
        {deleted ? (
          <button
            type="button"
            className="btn primary"
            disabled={!canRestore || busy}
            onClick={() => run(() => restoreEvent(item.id))}
          >
            {busy ? "복구 중…" : "복구"}
          </button>
        ) : (
          <>
            <button
              type="button"
              className="btn danger"
              onClick={() => setConfirming(true)}
            >
              삭제
            </button>
            <button
              type="button"
              className="btn primary"
              // 상세를 받아야 수정 폼을 채울 수 있다.
              disabled={!canEdit}
              onClick={onEdit}
            >
              정보 수정
            </button>
          </>
        )}
      </div>
    </>
  );
}
function ContentDialog({
  item,
  kind,
  onClose,
}: {
  item: ContentItem;
  kind: "events" | "reviews";
  onClose: () => void;
}) {
  const { moderate } = useAdmin();
  const { api } = useApp();
  const isEvent = kind === "events";
  // 목록 응답에는 본문이 없다. 행사는 상세를 따로 불러온다.
  const detail = useLoad(
    () => (isEvent ? api.adminFestival(item.id) : Promise.resolve(null)),
    [isEvent, item.id],
  );
  const [status, setStatus] = useState<Visibility>(
    item.status === "PUBLISHED" ? "HIDDEN" : "PUBLISHED",
  );
  const [reason, setReason] = useState("");
  const [editing, setEditing] = useState(false);
  // 수정 중에는 아래 읽기용 본문을 숨긴다. 같은 값이 두 번 보이면 헷갈린다.
  if (editing && detail.data) {
    return (
      <Modal title="행사 수정" onClose={onClose}>
        <EventEditForm
          festivalId={item.id}
          detail={detail.data}
          onDone={onClose}
          onCancel={() => setEditing(false)}
        />
      </Modal>
    );
  }
  return (
    <Modal title={isEvent ? "행사 검토" : "후기 검토"} onClose={onClose}>
      {item.image && (
        <img className="adm-dialog-image" src={item.image} alt={item.title} />
      )}
      <div className="adm-dialog-title">
        <Status status={item.status} />
        <h3>{item.title}</h3>
        <p>
          {item.author} · {item.date} · {item.id}
        </p>
      </div>
      {isEvent ? (
        <EventDetailBody
          loading={detail.loading}
          error={detail.error}
          detail={detail.data}
        />
      ) : (
        <p className="adm-content-body">{item.content}</p>
      )}
      {item.reason && (
        <p className="adm-dialog-note">최근 처리 사유: {item.reason}</p>
      )}
      {isEvent ? (
        <EventActions
          item={item}
          canEdit={!detail.loading && !!detail.data}
          onEdit={() => setEditing(true)}
          onClose={onClose}
        />
      ) : (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            moderate(kind, item.id, status, reason);
            onClose();
          }}
        >
          <Field label="변경할 노출 상태">
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value as Visibility)}
            >
              <option value="PUBLISHED">공개</option>
              <option value="HIDDEN">숨김</option>
            </select>
          </Field>
          <Reason value={reason} onChange={setReason} />
          <div className="adm-dialog-actions">
            <button className="btn secondary" type="button" onClick={onClose}>
              취소
            </button>
            <button
              className="btn primary"
              disabled={status === item.status || !reason.trim()}
            >
              처리 내용 저장
            </button>
          </div>
        </form>
      )}
    </Modal>
  );
}

export function AdminTickets() {
  const {
    data,
    ticketsLoading,
    ticketsError,
    reloadTickets,
    ticketQuery,
    setTicketQuery,
    ticketsPage,
  } = useAdmin();
  const [query, setQuery] = useState("");
  const [params, setParams] = useSearchParams();
  const status = params.get("status") || "";
  const category = params.get("category") || "";
  const records = data.tickets;
  const items = records;
  const selected = records.find((t) => t.id === params.get("item"));
  useEffect(() => {
    const timer = window.setTimeout(() => {
      setTicketQuery({
        title: query.trim() || undefined,
        status: status ? (status as "PENDING" | "ANSWERED") : undefined,
        category: category ? (category as InquiryCategory) : undefined,
      });
    }, 250);
    return () => window.clearTimeout(timer);
  }, [query, status, category, setTicketQuery]);
  function setParam(key: string, value: string) {
    setParams((current) => {
      const next = new URLSearchParams(current);
      value ? next.set(key, value) : next.delete(key);
      return next;
    });
  }
  return (
    <>
      <Heading
        eyebrow="SUPPORT & SAFETY"
        title="문의·신고·제보"
        description="이용 문의에 답변하고 신고 및 행사 정보 제보를 검토합니다."
      />
      <section className="adm-panel">
        <Tabs
          value={category}
          onChange={(value) => setParam("category", value)}
          options={[
            { value: "", label: "전체 유형" },
            { value: "QUESTION", label: "일반 문의" },
            { value: "REPORT", label: "신고" },
            { value: "TIP", label: "제보" },
          ]}
        />
        <Tabs
          value={status}
          onChange={(value) => setParam("status", value)}
          options={[
            { value: "", label: "전체 상태" },
            { value: "PENDING", label: "답변 대기" },
            { value: "ANSWERED", label: "답변 완료" },
          ]}
        />
        <Toolbar
          query={query}
          setQuery={setQuery}
          placeholder="접수 제목 검색"
        />
        <div className="adm-result-count">
          {ticketsLoading ? (
            "불러오는 중…"
          ) : (
            <>
              검색 결과 <strong>{items.length}</strong>건
              {ticketsPage.totalElements !== items.length &&
                ` / 전체 ${ticketsPage.totalElements}건`}
            </>
          )}
        </div>
        {ticketsError && (
          <p className="adm-dialog-note" role="alert">
            {ticketsError}{" "}
            <button
              type="button"
              className="adm-row-button"
              onClick={reloadTickets}
            >
              다시 시도
            </button>
          </p>
        )}
        <Table
          label="문의·신고·제보 목록"
          headers={[
            "유형",
            "접수 내용",
            "작성자",
            "처리 상태",
            "접수일",
            "관리",
          ]}
          empty={!items.length}
        >
          {items.map((ticket) => (
            <tr key={ticket.id}>
              <td>
                <Badge tone={ticketCategoryTone(ticket.category)}>
                  {ticketCategoryNames[ticket.category]}
                </Badge>
              </td>
              <td>
                <div className="adm-content-cell">
                  <div>
                    <strong>{ticket.title}</strong>
                    <small>{ticket.id}</small>
                  </div>
                </div>
              </td>
              <td>{ticket.author}</td>
              <td>
                <Badge tone={tone(ticket.status)}>
                  {ticket.status === "PENDING" ? "답변 대기" : "답변 완료"}
                </Badge>
              </td>
              <td>{ticket.date}</td>
              <td>
                <button
                  className="adm-row-button"
                  aria-label={`${ticket.id} 접수 상세`}
                  onClick={() => setParam("item", ticket.id)}
                >
                  상세 보기
                  <ChevronRight size={14} />
                </button>
              </td>
            </tr>
          ))}
        </Table>
        <Pagination
          page={ticketsPage.number}
          totalPages={ticketsPage.totalPages}
          onChange={(page) => setTicketQuery({ ...ticketQuery, page })}
        />
      </section>
      {selected && (
        <TicketDialog
          key={selected.id}
          ticket={selected}
          onClose={() => setParam("item", "")}
        />
      )}
    </>
  );
}
function TicketDialog({
  ticket,
  onClose,
}: {
  ticket: Ticket;
  onClose: () => void;
}) {
  const { data, answer } = useAdmin();
  const { api } = useApp();
  // 목록 응답에는 본문과 답변이 없다. 상세를 열 때 따로 불러온다.
  const detail = useLoad(() => api.adminInquiry(ticket.id), [ticket.id]);
  const [response, setResponse] = useState("");
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState("");
  const [editing, setEditing] = useState(false);
  const target =
    ticket.target &&
    data[ticket.target.kind].find((item) => item.id === ticket.target!.id);
  // 상세를 받기 전에는 목록에서 알고 있는 값으로 그린다.
  const status = detail.data?.status ?? ticket.status;
  const answered = status === "ANSWERED";
  return (
    <Modal
      title={`${ticketCategoryNames[ticket.category]} 상세`}
      onClose={onClose}
    >
      <div className="adm-dialog-title">
        <Badge tone={tone(status)}>
          {status === "PENDING" ? "답변 대기" : "답변 완료"}
        </Badge>
        <h3>{ticket.title}</h3>
        <p>
          {ticket.author} · {ticket.date} · {ticket.id}
        </p>
      </div>
      {detail.loading ? (
        <p className="adm-content-body">본문을 불러오는 중…</p>
      ) : detail.error ? (
        <p className="adm-content-body" role="alert">
          {detail.error}
        </p>
      ) : (
        <>
          <p className="adm-content-body">{detail.data?.content}</p>
          {detail.data?.img && (
            <img
              className="adm-content-image"
              src={detail.data.img}
              alt="문의 첨부 이미지"
            />
          )}
          {detail.data?.deletedAt && (
            <p className="adm-dialog-note">
              작성자가 삭제한 문의입니다. 답변을 남겨도 작성자에게 보이지
              않습니다.
            </p>
          )}
        </>
      )}
      {ticket.target && (
        <Link
          className="adm-target-link"
          to={
            ticket.target.kind === "events"
              ? `/events/${ticket.target.id}`
              : `/admin/reviews?item=${ticket.target.id}`
          }
          target={ticket.target.kind === "events" ? "_blank" : undefined}
        >
          <Eye size={18} />
          <span>
            <small>
              {ticket.category === "REPORT" ? "신고된 콘텐츠" : "제보 대상"}
              {target && ` · ${statusNames[target.status]}`}
            </small>
            <strong>{ticket.target.title}</strong>
          </span>
          <ExternalLink size={16} />
        </Link>
      )}
      {answered && !editing ? (
        <div className="adm-saved-answer">
          <span>
            <Check size={16} />
            운영팀 답변
          </span>
          <p>{detail.data?.answer ?? ticket.answer}</p>
          <div className="adm-dialog-actions">
            <button
              type="button"
              className="btn secondary"
              disabled={detail.loading || !!detail.data?.deletedAt}
              onClick={() => {
                setResponse(detail.data?.answer ?? "");
                setEditing(true);
              }}
            >
              답변 수정
            </button>
          </div>
        </div>
      ) : (
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            setSaving(true);
            setSaveError("");
            try {
              await answer(ticket.id, response);
              onClose();
            } catch (error) {
              // 실패하면 닫지 않는다. 쓴 답변이 그대로 남아 있어야 다시 시도한다.
              setSaveError(errorText(error));
            } finally {
              setSaving(false);
            }
          }}
        >
          <Field label="운영팀 답변" required>
            <textarea
              rows={5}
              required
              maxLength={2000}
              value={response}
              onChange={(e) => setResponse(e.target.value)}
              placeholder="확인한 내용과 처리 결과를 안내해 주세요."
            />
          </Field>
          {ticket.category !== "QUESTION" && (
            <p className="adm-dialog-note">
              콘텐츠 조치가 필요하면 위 링크에서 먼저 검토하세요. 답변
              등록만으로 콘텐츠가 숨겨지지는 않습니다.
            </p>
          )}
          {saveError && (
            <p className="adm-dialog-note" role="alert">
              {saveError}
            </p>
          )}
          <div className="adm-dialog-actions">
            <button
              type="button"
              className="btn secondary"
              onClick={() => (editing ? setEditing(false) : onClose())}
            >
              취소
            </button>
            <button
              className="btn primary"
              disabled={!response.trim() || saving || detail.loading}
            >
              {saving ? "등록 중…" : "답변 등록"}
            </button>
          </div>
        </form>
      )}
    </Modal>
  );
}

export function AdminActivity() {
  const { data } = useAdmin();
  const [query, setQuery] = useState("");
  const [area, setArea] = useState("");
  const items = data.activity.filter(
    (log) =>
      includes(query, log.action, log.target, log.reason) &&
      (!area || log.area === area),
  );
  return (
    <>
      <Heading
        eyebrow="ACTIVITY LOG"
        title="운영 기록"
        description="회원 등급, 콘텐츠 노출, 문의 답변의 변경 이력을 확인합니다."
      />
      <section className="adm-panel">
        <Toolbar
          query={query}
          setQuery={setQuery}
          placeholder="처리 내용, 대상, 사유 검색"
        >
          <select
            aria-label="운영 기록 영역"
            value={area}
            onChange={(e) => setArea(e.target.value)}
          >
            <option value="">모든 영역</option>
            {["회원", "행사", "후기", "문의·신고·제보"].map((value) => (
              <option key={value}>{value}</option>
            ))}
          </select>
        </Toolbar>
        <div className="adm-result-count">
          운영 기록 <strong>{items.length}</strong>건
        </div>
        <Table
          label="운영 기록 목록"
          headers={["일시", "영역", "처리 내용 / 대상", "처리 사유", "처리자"]}
          empty={!items.length}
        >
          {items.map((log) => (
            <tr key={log.id}>
              <td>
                <time dateTime={log.at}>{stamp(log.at)}</time>
              </td>
              <td>
                <Badge tone="gray">{log.area}</Badge>
              </td>
              <td>
                <div className="adm-content-cell">
                  <div>
                    <strong>{log.action}</strong>
                    <small>{log.target}</small>
                  </div>
                </div>
              </td>
              <td className="adm-reason-cell">{log.reason}</td>
              <td>시스템 관리자</td>
            </tr>
          ))}
        </Table>
      </section>
      <p className="adm-footnote">
        이 기록은 현재 탭의 Mock 작업 내역이며, 실제 서버의 감사 로그가
        아닙니다.
      </p>
    </>
  );
}
