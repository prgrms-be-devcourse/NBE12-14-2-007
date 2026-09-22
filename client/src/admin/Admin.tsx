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
  RotateCcw,
  Search,
  ShieldCheck,
  UsersRound,
} from "lucide-react";
import { Badge, Empty, Field, Logo, Modal } from "../components/ui";
import { AppProvider, useApp, useLoad } from "../lib/context";
import { errorText } from "../lib/format";
import type { Role } from "../lib/types";
import {
  AdminProvider,
  roleNames,
  useAdmin,
  visibilityNames,
  type AdminMember,
  type ContentItem,
  type Ticket,
  type Visibility,
} from "./store";
import "./admin.css";

const navigation = [
  { to: "/admin", label: "운영 대시보드", icon: LayoutDashboard },
  { to: "/admin/members", label: "회원 관리", icon: UsersRound },
  { to: "/admin/events", label: "행사 관리", icon: CalendarDays },
  { to: "/admin/reviews", label: "후기 관리", icon: MessageSquare },
  { to: "/admin/inquiries", label: "문의·신고", icon: Flag },
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
const stamp = (date: string) =>
  new Date(date).toLocaleString("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });

export function AdminLayout() {
  // AdminProvider가 실제 어드민 API를 부르려면 토큰과 로그인 정보가 필요하다.
  // AppProvider가 마운트될 때 리프레시 쿠키로 액세스 토큰을 복구한다.
  return (
    <AppProvider>
      <AdminProvider>
        <AdminShell />
      </AdminProvider>
    </AppProvider>
  );
}
function AdminShell() {
  const { data, reset } = useAdmin();
  const [resetOpen, setResetOpen] = useState(false);
  const location = useLocation();
  const pending = data.tickets.filter(
    (item) => item.status === "PENDING",
  ).length;
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
            <strong>Mock 워크스페이스</strong>
            <p>실제 회원과 서비스에 영향을 주지 않는 관리자 미리보기입니다.</p>
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
            <strong>관리자 미리보기</strong>
            <span>모든 데이터와 처리 결과는 Mock입니다.</span>
          </span>
          <button onClick={() => setResetOpen(true)}>
            <RotateCcw size={14} />
            예시 초기화
          </button>
        </div>
        <main id="admin-main" className="adm-main">
          <Outlet />
        </main>
        <footer className="adm-footer">
          <Link to="/">방구석탈출 · 서비스로 돌아가기</Link>
          <span>변경 내용은 현재 탭에만 저장됩니다.</span>
        </footer>
      </div>
      {resetOpen && (
        <Modal
          title="예시 데이터를 초기화할까요?"
          onClose={() => setResetOpen(false)}
        >
          <p className="adm-dialog-copy">
            이 탭에서 변경한 관리자 Mock 데이터와 운영 기록을 처음 상태로
            되돌립니다.
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
function Status({ status }: { status: Visibility }) {
  return <Badge tone={tone(status)}>{visibilityNames[status]}</Badge>;
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
}: {
  value: string;
  onChange: (value: string) => void;
  options: { value: string; label: string; count: number }[];
}) {
  return (
    <div className="adm-tabs" role="group" aria-label="상태 필터">
      {options.map((option) => (
        <button
          key={option.value}
          className={value === option.value ? "active" : ""}
          aria-pressed={value === option.value}
          onClick={() => onChange(option.value)}
        >
          {option.label}
          <span>{option.count}</span>
        </button>
      ))}
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
  const { data } = useAdmin();
  const pending = data.tickets.filter((t) => t.status === "PENDING");
  const reports = pending.filter((t) => t.category === "REPORT");
  const waiting = data.events.filter((e) => e.status === "PENDING").length;
  const stats = [
    {
      label: "전체 회원",
      value: data.members.length,
      unit: "명",
      detail: `신뢰 회원 ${data.members.filter((m) => m.role === "ROLE_TRUSTED").length}명`,
      icon: UsersRound,
      to: "/admin/members",
      color: "sage",
    },
    {
      label: "등록된 행사",
      value: data.events.length,
      unit: "건",
      detail: `검토 대기 ${waiting}건`,
      icon: CalendarDays,
      to: "/admin/events",
      color: "peach",
    },
    {
      label: "행사 후기",
      value: data.reviews.length,
      unit: "건",
      detail: `공개 후기 ${data.reviews.filter((p) => p.status === "PUBLISHED").length}건`,
      icon: MessageSquare,
      to: "/admin/reviews",
      color: "blue",
    },
    {
      label: "미처리 문의·신고",
      value: pending.length,
      unit: "건",
      detail: `신고 ${reports.length}건 우선 확인`,
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
          2026. 09. 21 기준 예시
        </span>
      </Heading>
      <section className="adm-welcome">
        <div>
          <span className="adm-eyebrow">BETTER EXPERIENCES, TOGETHER</span>
          <h2>좋은 경험이 이어지는 공간을 만듭니다.</h2>
          <p>
            확인이 필요한 문의·신고 <strong>{pending.length}건</strong>과 행사
            제보 <strong>{waiting}건</strong>이 있습니다.
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
      <section className="adm-stats" aria-label="서비스 현황">
        {stats.map(({ label, value, unit, detail, icon: Icon, to, color }) => (
          <Link className="adm-stat" to={to} key={label}>
            <div>
              <span>{label}</span>
              <span className={`adm-stat-icon ${color}`}>
                <Icon size={20} />
              </span>
            </div>
            <p>
              <strong>{value}</strong>
              <span>{unit}</span>
            </p>
            <div className="adm-stat-bottom">
              <span>{detail}</span>
              <ArrowRight size={15} />
            </div>
          </Link>
        ))}
      </section>
      <div className="adm-dashboard-grid">
        <section className="adm-panel">
          <PanelTitle
            title="우선 확인할 문의·신고"
            description="답변을 기다리는 이용자의 목소리입니다."
            to="/admin/inquiries?status=PENDING"
          />
          {pending.length ? (
            <div className="adm-priority-list">
              {pending.map((ticket) => (
                <Link key={ticket.id} to={`/admin/inquiries?item=${ticket.id}`}>
                  <span
                    className={`adm-ticket-icon ${ticket.category === "REPORT" ? "report" : ""}`}
                  >
                    {ticket.category === "REPORT" ? (
                      <Flag size={18} />
                    ) : (
                      <MessageSquare size={18} />
                    )}
                  </span>
                  <div>
                    <span className="adm-item-meta">
                      {ticket.category === "REPORT" ? "신고 접수" : "이용 문의"}{" "}
                      · {ticket.id}
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
            title="콘텐츠 검토 현황"
            description="목록의 현재 상태를 기준으로 집계합니다."
          />
          <div className="adm-content-summary">
            {[
              {
                name: "공개 중인 행사",
                count: data.events.filter((e) => e.status === "PUBLISHED")
                  .length,
                max: data.events.length,
                color: "green",
              },
              {
                name: "검토 대기 제보",
                count: waiting,
                max: data.events.length,
                color: "orange",
              },
              {
                name: "숨김 처리된 후기",
                count: data.reviews.filter((p) => p.status === "HIDDEN").length,
                max: data.reviews.length,
                color: "gray",
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
          <Link
            className="adm-review-callout"
            to="/admin/events?status=PENDING"
          >
            <ClipboardCheck size={23} />
            <span>
              <strong>행사 제보 검토</strong>
              <small>내용을 확인하고 공개 여부를 결정하세요.</small>
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
  const { data, membersLoading, membersError, reloadMembers } = useAdmin();
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
      <div className="adm-summary-line">
        <span>
          <UsersRound size={18} />
          전체 회원 <strong>{data.members.length}명</strong>
        </span>
        <span>
          주의 등급{" "}
          <strong>
            {data.members.filter((m) => m.role === "ROLE_WARNING").length}명
          </strong>
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

export function AdminContent({ kind }: { kind: "events" | "reviews" }) {
  const { data } = useAdmin();
  const [params, setParams] = useSearchParams();
  const [query, setQuery] = useState("");
  const [source, setSource] = useState("");
  const status = params.get("status") || "";
  const isEvent = kind === "events";
  const records = data[kind];
  const statuses: Visibility[] = isEvent
    ? ["PENDING", "PUBLISHED", "HIDDEN"]
    : ["PUBLISHED", "HIDDEN"];
  const items = records.filter(
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
            { value: "", label: "전체", count: records.length },
            ...statuses.map((value) => ({
              value,
              label: visibilityNames[value],
              count: records.filter((r) => r.status === value).length,
            })),
          ]}
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
        </Toolbar>
        <div className="adm-result-count">
          검색 결과 <strong>{items.length}</strong>건
        </div>
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
                    <strong>{item.title}</strong>
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
      </section>
      <p className="adm-footnote">
        공개·검토 대기·숨김은 관리자 화면의 예시 운영 상태입니다. 실제 서비스의
        행사 진행 상태와 별개입니다.
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
  const [status, setStatus] = useState<Visibility>(
    item.status === "PUBLISHED" ? "HIDDEN" : "PUBLISHED",
  );
  const [reason, setReason] = useState("");
  return (
    <Modal
      title={kind === "events" ? "행사 검토" : "후기 검토"}
      onClose={onClose}
    >
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
      <p className="adm-content-body">{item.content}</p>
      {item.reason && (
        <p className="adm-dialog-note">최근 처리 사유: {item.reason}</p>
      )}
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
            {kind === "events" && <option value="PENDING">검토 대기</option>}
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
    </Modal>
  );
}

export function AdminTickets() {
  const { data, ticketsLoading, ticketsError, reloadTickets } = useAdmin();
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("");
  const [params, setParams] = useSearchParams();
  const status = params.get("status") || "";
  const records = data.tickets;
  const items = records.filter(
    (t) =>
      includes(query, t.title, t.author, t.id) &&
      (!status || t.status === status) &&
      (!category || t.category === category),
  );
  const selected = records.find((t) => t.id === params.get("item"));
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
        title="문의·신고"
        description="이용 문의에 답변하고 신고 내용을 검토합니다."
      />
      <section className="adm-panel">
        <Tabs
          value={status}
          onChange={(value) => setParam("status", value)}
          options={[
            { value: "", label: "전체", count: records.length },
            {
              value: "PENDING",
              label: "답변 대기",
              count: records.filter((t) => t.status === "PENDING").length,
            },
            {
              value: "ANSWERED",
              label: "답변 완료",
              count: records.filter((t) => t.status === "ANSWERED").length,
            },
          ]}
        />
        <Toolbar
          query={query}
          setQuery={setQuery}
          placeholder="제목, 작성자, 접수 ID 검색"
        >
          <select
            aria-label="접수 유형"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
          >
            <option value="">문의·신고 전체</option>
            <option value="QUESTION">일반 문의</option>
            <option value="REPORT">신고</option>
          </select>
        </Toolbar>
        <div className="adm-result-count">
          {ticketsLoading ? (
            "불러오는 중…"
          ) : (
            <>
              검색 결과 <strong>{items.length}</strong>건
            </>
          )}
        </div>
        {ticketsError && (
          <p className="adm-dialog-note" role="alert">
            {ticketsError}{" "}
            <button type="button" className="adm-row-button" onClick={reloadTickets}>
              다시 시도
            </button>
          </p>
        )}
        <Table
          label="문의·신고 목록"
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
                <Badge tone={ticket.category === "REPORT" ? "orange" : "gray"}>
                  {ticket.category === "REPORT" ? "신고" : "문의"}
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
      title={ticket.category === "REPORT" ? "신고 상세" : "문의 상세"}
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
          to={`/admin/${ticket.target.kind}?item=${ticket.target.id}`}
        >
          <Eye size={18} />
          <span>
            <small>
              신고된 콘텐츠 {target && `· ${visibilityNames[target.status]}`}
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
          {ticket.category === "REPORT" && (
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
            {["회원", "행사", "후기", "문의·신고"].map((value) => (
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
