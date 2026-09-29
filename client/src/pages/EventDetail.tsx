import { useRef, useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  AlertTriangle,
  CalendarDays,
  Check,
  CheckCircle2,
  Clock3,
  Flag,
  Heart,
  Landmark,
  MapPin,
  Pencil,
  Phone,
  Share2,
  Ticket,
  Trash2,
  UserRound,
  UsersRound,
} from "lucide-react";
import { ApiError } from "../lib/api";
import { useApp, useLoad } from "../lib/context";
import {
  errorText,
  eventState,
  imageUrl,
  period,
  regions,
  roleNames,
  safeUrl,
} from "../lib/format";
import type {
  EventView,
  FestivalAccuracyVote,
  FestivalMember,
} from "../lib/types";
import {
  Badge,
  Empty,
  ErrorState,
  Field,
  FormError,
  Loading,
  LoginRequired,
  Modal,
  SubmitButton,
  Upload,
} from "../components/ui";
import { FestivalPosts } from "./Reviews";
import { RichTextContent } from "../components/RichText";

export function EventDetailPage() {
  const { eventId, submissionId } = useParams();
  const { member, authLoading } = useApp();
  if (submissionId)
    return (
      <div className="container page-space">
        {authLoading ? (
          <Loading />
        ) : member ? (
          <SubmissionDetailLoader id={submissionId} />
        ) : (
          <LoginRequired />
        )}
      </div>
    );
  return (
    <div className="container page-space">
      {authLoading ? (
        <Loading />
      ) : (
        <EventLoader
          key={`${eventId}-${member?.id ?? "guest"}`}
          id={Number(eventId)}
        />
      )}
    </div>
  );
}
function EventLoader({ id }: { id: number }) {
  const { api } = useApp();
  const { data, loading, error, reload } = useLoad(
    () => api.event(id),
    [api, id],
  );
  return loading ? (
    <Loading />
  ) : error ? (
    <ErrorState message={error} retry={reload} />
  ) : data ? (
    <EventDetail event={data} />
  ) : (
    <Empty title="행사를 찾을 수 없어요" />
  );
}
function SubmissionDetailLoader({ id }: { id: string }) {
  const { api } = useApp();
  const { data, loading, error, reload } = useLoad(
    () => api.submission(id),
    [api, id],
  );
  return loading ? (
    <Loading />
  ) : error ? (
    <ErrorState message={error} retry={reload} />
  ) : data ? (
    <EventDetail
      key={id}
      event={{
        ...data.submission.festival,
        manager: null,
        source: "MEMBER",
        submissionId: id,
      }}
    />
  ) : null;
}
function EventDetail({ event }: { event: EventView }) {
  const { api, toast } = useApp();
  const navigate = useNavigate();
  const [tab, setTab] = useState("about");
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [copied, setCopied] = useState(false);
  const externalUrl = safeUrl(event.referenceUrl);
  const heroUrl = imageUrl(event.imgUrl, 1200);
  const inaccurateVoteCount = event.accuracyVote?.inaccurateCount ?? 0;
  const [heroVisible, setHeroVisible] = useState(Boolean(heroUrl));
  // 리사이즈 프록시가 원본을 처음 받아 축소하는 동안(캐시 없을 때) 몇 초 걸릴 수 있어서,
  // 그 사이 빈 화면 대신 스켈레톤을 보여준다.
  const [heroLoaded, setHeroLoaded] = useState(false);
  const back = event.submissionId ? "/mypage?tab=submissions" : "/explore";
  async function share() {
    try {
      await navigator.clipboard.writeText(window.location.href);
      setCopied(true);
      toast("행사 링크를 복사했어요.");
      setTimeout(() => setCopied(false), 2500);
    } catch {
      toast("브라우저 주소창의 링크를 복사해 주세요.");
    }
  }
  return (
    <>
      <div className="detail-topbar">
        <Link className="back-link" to={back}>
          <ArrowLeft size={16} />
          목록으로
        </Link>
        <div className="action-row">
          <button className="text-button" onClick={share}>
            {copied ? <Check size={16} /> : <Share2 size={16} />}공유하기
          </button>
          {event.submissionId && (
            <Link
              className="text-button"
              to={`/submissions/${event.submissionId}/edit`}
            >
              <Pencil size={15} />
              제보 수정
            </Link>
          )}
        </div>
      </div>
      {inaccurateVoteCount >= 10 && (
        <div className="detail-accuracy-warning" role="alert">
          <AlertTriangle size={20} aria-hidden="true" />
          <div>
            <strong>부정확한 정보일 수 있어요</strong>
            <p>
              이 행사에 ‘부정확해요’ 평가가 {inaccurateVoteCount}개 등록됐어요.
              방문 전 공식 채널에서 일정과 장소를 다시 확인해 주세요.
            </p>
          </div>
        </div>
      )}
      {heroUrl && heroVisible && (
        <div className={`detail-hero ${heroLoaded ? "" : "loading"}`}>
          {!heroLoaded && (
            <div className="detail-hero-skeleton" aria-hidden="true" />
          )}
          <img
            src={heroUrl}
            alt={event.title}
            onLoad={() => setHeroLoaded(true)}
            onError={() => setHeroVisible(false)}
          />
          {event.preview && (
            <span className="photo-example">미리보기 예시 이미지</span>
          )}
        </div>
      )}
      <div className="detail-layout">
        <div className="detail-main">
          <div className="detail-title">
            <div className="action-row">
              <span
                className={`source-badge ${event.source === "PUBLIC" ? "public" : "member"}`}
              >
                {event.source === "PUBLIC" ? "공공데이터" : "회원 제보"}
              </span>
              <Badge tone="gray">{event.category || "행사"}</Badge>
              <span className="state-label">
                {eventState(event.beginDe, event.endDe)}
              </span>
            </div>
            <h1>{event.title}</h1>
            <p className="detail-location">
              <MapPin size={17} />
              {regions[event.region] || ""} {event.regionDetail}
            </p>
          </div>
          <div
            className="detail-tabs"
            role="tablist"
            aria-label="행사 상세 메뉴"
          >
            <button
              role="tab"
              id="about-tab"
              aria-selected={tab === "about"}
              aria-controls="event-panel"
              className={tab === "about" ? "active" : ""}
              onClick={() => setTab("about")}
            >
              행사 소개
            </button>
            <button
              role="tab"
              id="reviews-tab"
              aria-selected={tab === "reviews"}
              aria-controls="event-panel"
              className={tab === "reviews" ? "active" : ""}
              onClick={() => setTab("reviews")}
            >
              행사 후기
            </button>
          </div>
          <div
            id="event-panel"
            role="tabpanel"
            aria-labelledby={tab === "about" ? "about-tab" : "reviews-tab"}
          >
            {tab === "about" ? (
              <>
                <section className="detail-section">
                  <h2>요약 정보</h2>
                  <div className="detail-summary">
                    <div>
                      <CalendarDays size={20} />
                      <span>
                        <small>일정</small>
                        {period(event.beginDe, event.endDe)}
                      </span>
                    </div>
                    <div>
                      <MapPin size={20} />
                      <span>
                        <small>장소</small>
                        {event.regionDetail ||
                          regions[event.region] ||
                          "장소 안내 확인"}
                      </span>
                    </div>
                    <div>
                      <Ticket size={20} />
                      <span>
                        <small>참가 비용</small>
                        {event.partcptExpnInfo || "별도 안내 없음"}
                      </span>
                    </div>
                  </div>
                </section>
                <section className="detail-section detail-description">
                  <RichTextContent
                    content={
                      event.festivalContent ||
                      "아직 등록된 상세 소개가 없어요. 행사 참고 링크에서 자세한 내용을 확인해 주세요."
                    }
                  />
                  {externalUrl ? (
                    <a
                      className="btn primary"
                      href={externalUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      행사 안내 페이지 보기
                    </a>
                  ) : (
                    <p className="aside-note">
                      등록된 행사 참고 링크가 없어요.
                    </p>
                  )}
                </section>
                <section className="detail-section">
                  <h2>상세 정보</h2>
                  <dl className="event-info">
                    <div>
                      <dt>
                        <CalendarDays size={17} />
                        행사 일정
                      </dt>
                      <dd>{period(event.beginDe, event.endDe)}</dd>
                    </div>
                    <div>
                      <dt>
                        <Clock3 size={17} />
                        운영 시간
                      </dt>
                      <dd>{event.eventTmInfo || "별도 안내 없음"}</dd>
                    </div>
                    <div>
                      <dt>
                        <MapPin size={17} />
                        행사 장소
                      </dt>
                      <dd>
                        {event.regionDetail
                          ? `${regions[event.region] || ""} ${event.regionDetail}`
                          : "행사 안내 페이지에서 확인"}
                      </dd>
                    </div>
                    <div>
                      <dt>
                        <Ticket size={17} />
                        참가 비용
                      </dt>
                      <dd>{event.partcptExpnInfo || "별도 안내 없음"}</dd>
                    </div>
                    <div>
                      <dt>
                        <Landmark size={17} />
                        주최·운영
                      </dt>
                      <dd>
                        {[event.hostInstNm, event.instNm]
                          .filter(Boolean)
                          .join(" · ") || "별도 안내 없음"}
                      </dd>
                    </div>
                    {event.manager && (
                      <div>
                        <dt>
                          <UsersRound size={17} />
                          행사 담당자
                        </dt>
                        <dd>{event.manager}</dd>
                      </div>
                    )}
                    {event.telnoInfo && (
                      <div>
                        <dt>
                          <Phone size={17} />
                          문의 연락처
                        </dt>
                        <dd>{event.telnoInfo}</dd>
                      </div>
                    )}
                  </dl>
                </section>
                <div className="detail-source">
                  <Landmark size={19} />
                  <p>
                    {event.preview
                      ? "디자인 확인을 위해 구성한 예시 행사입니다. 실제 일정과 다릅니다."
                      : event.submissionId
                        ? "내가 제보한 행사 정보입니다. 일정이 변경되었다면 제보 내용을 수정해 주세요."
                        : event.source === "MEMBER"
                          ? "회원이 직접 제보한 행사 정보입니다. 방문 전 행사 안내 페이지에서 정확한 정보를 확인해 주세요."
                          : "공공데이터를 통해 제공된 행사 정보입니다. 방문 전 행사 안내 페이지에서 최신 정보를 확인해 주세요."}
                  </p>
                </div>
                {event.source === "MEMBER" && event.submitter && (
                  <SubmitterProfile member={event.submitter} />
                )}
                {!event.submissionId && <FestivalLikeButton event={event} />}
                {event.source === "MEMBER" && !event.submissionId && (
                  <AccuracyVotePanel
                    festivalId={event.festivalId}
                    initialVote={event.accuracyVote}
                  />
                )}
              </>
            ) : (
              <FestivalPosts festivalId={event.festivalId} />
            )}
          </div>
          {!event.submissionId && <FestivalReportButton event={event} />}
          {event.submissionId && (
            <button
              className="text-button muted delete-submission"
              onClick={() => setDeleteOpen(true)}
            >
              <Trash2 size={14} />내 제보 삭제
            </button>
          )}
        </div>
      </div>
      {deleteOpen && (
        <Modal
          title="행사 제보를 삭제할까요?"
          onClose={() => setDeleteOpen(false)}
        >
          <p className="muted">
            내 제보 목록에서 이 제보가 삭제돼요. 행사에 남긴 후기는 별도로
            관리됩니다.
          </p>
          <FormError message={error} />
          <div className="form-actions">
            <button
              className="btn secondary"
              onClick={() => setDeleteOpen(false)}
            >
              취소
            </button>
            <button
              className="btn danger"
              disabled={busy}
              onClick={async () => {
                setBusy(true);
                try {
                  await api.deleteSubmission(event.submissionId!);
                  toast("제보를 삭제했어요.");
                  navigate("/mypage?tab=submissions");
                } catch (e) {
                  setError(errorText(e));
                } finally {
                  setBusy(false);
                }
              }}
            >
              제보 삭제
            </button>
          </div>
        </Modal>
      )}
    </>
  );
}

function SubmitterProfile({ member }: { member: FestivalMember }) {
  const avatarUrl = imageUrl(member.profileImg, 120);
  const [avatarVisible, setAvatarVisible] = useState(Boolean(avatarUrl));

  return (
    <section className="submitter-profile" aria-label="행사 제보자 정보">
      {avatarUrl && avatarVisible ? (
        <img
          className="submitter-avatar"
          src={avatarUrl}
          alt={`${member.nickname} 프로필`}
          loading="lazy"
          onError={() => setAvatarVisible(false)}
        />
      ) : (
        <span className="submitter-avatar fallback" aria-hidden="true">
          <UserRound size={30} />
        </span>
      )}
      <div className="submitter-profile-copy">
        <small>이 행사를 알려준 이웃</small>
        <div>
          <strong>{member.nickname}</strong>
          <TrustGradeBadge member={member} />
        </div>
      </div>
    </section>
  );
}

function TrustGradeBadge({ member }: { member: FestivalMember }) {
  const grade =
    member.role === "ROLE_WARNING"
      ? "warning"
      : member.role === "ROLE_RECOGNIZED"
        ? "maker"
        : member.role === "ROLE_TRUSTED"
          ? "master"
          : "basic";
  const symbol = grade === "maker" ? "m" : grade === "master" ? "M" : null;
  const description =
    member.role === "ROLE_WARNING"
      ? "운영 정책에 따라 현재 활동이 제한된 계정입니다."
      : member.role === "ROLE_RECOGNIZED"
        ? "좋아요 또는 정확해요를 10개 이상 받은 제보자예요."
        : member.role === "ROLE_TRUSTED"
          ? "좋아요와 정확해요를 모두 10개 이상 받은 제보자예요."
          : member.role === "ROLE_ADMIN"
            ? "방구석탈출 운영·관리 계정이에요."
            : "이제 막 탈출 정보를 나누기 시작한 제보자예요.";
  const tooltipId = `trust-grade-${member.id}`;

  return (
    <span
      className="trust-grade-wrap"
      tabIndex={0}
      aria-describedby={tooltipId}
    >
      <Badge tone={`trust-grade-badge ${grade}`}>
        {grade === "warning" ? (
          <AlertTriangle
            className="trust-grade-warning-icon"
            size={12}
            aria-hidden="true"
          />
        ) : symbol ? (
          <span className="trust-grade-symbol" aria-hidden="true">
            {symbol}
          </span>
        ) : null}
        <span>{roleNames[member.role]}</span>
      </Badge>
      <span className="trust-grade-tooltip" id={tooltipId} role="tooltip">
        {description}
      </span>
    </span>
  );
}

function FestivalLikeButton({ event }: { event: EventView }) {
  const { api, member, authLoading, toast } = useApp();
  const pending = useRef(false);
  const [liked, setLiked] = useState(event.likedByMe ?? false);
  const [likeCount, setLikeCount] = useState(event.likeCount ?? 0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function toggle() {
    if (pending.current || authLoading) return;
    if (!member) {
      toast("로그인하면 행사에 좋아요를 남길 수 있어요.");
      return;
    }

    pending.current = true;
    setBusy(true);
    setError("");
    try {
      const result = await api.likeFestival(event.festivalId, liked);
      setLikeCount(result.likeCount);
      setLiked(!liked);
    } catch (e) {
      if (e instanceof ApiError && ["LIKE000", "LIKE001"].includes(e.code)) {
        try {
          const current = await api.event(event.festivalId);
          setLikeCount(current.likeCount ?? 0);
          setLiked(current.likedByMe ?? false);
        } catch (refreshError) {
          setError(errorText(refreshError));
        }
      } else {
        setError(errorText(e));
      }
    } finally {
      pending.current = false;
      setBusy(false);
    }
  }

  return (
    <section className="festival-like" aria-labelledby="festival-like-title">
      <div>
        <h2 id="festival-like-title">마음에 드는 행사인가요?</h2>
        <p>좋아요로 관심 있는 행사를 표현해 주세요.</p>
      </div>
      <button
        type="button"
        className={`btn ${liked ? "liked" : "secondary"}`}
        aria-label={`행사 좋아요${liked ? " 취소" : ""} ${likeCount}개`}
        aria-pressed={liked}
        aria-busy={busy}
        disabled={busy || authLoading}
        onClick={toggle}
      >
        <Heart
          size={18}
          fill={liked ? "currentColor" : "none"}
          aria-hidden="true"
        />
        좋아요
        <strong>{likeCount}</strong>
      </button>
      <FormError message={error} />
    </section>
  );
}

const inquiryKinds = [
  {
    value: "REPORT",
    label: "신고",
    description: "광고, 욕설 등 부적절한 콘텐츠를 알려주세요.",
  },
  {
    value: "TIP",
    label: "제보",
    description: "잘못되거나 누락된 행사 정보를 알려주세요.",
  },
] as const;
type FestivalInquiryKind = (typeof inquiryKinds)[number]["value"];

const inquiryReasons = {
  REPORT: [
    { value: "INAPPROPRIATE", label: "부적절한 내용" },
    { value: "SPAM", label: "광고·홍보성 콘텐츠" },
    { value: "OTHER", label: "기타" },
  ],
  TIP: [
    { value: "WRONG_INFO", label: "허위·잘못된 정보" },
    { value: "CLOSED", label: "취소·종료된 행사" },
    { value: "MISSING_INFO", label: "누락된 정보" },
    { value: "OTHER", label: "기타" },
  ],
} as const;

/**
 * 행사 상세에서 신고 또는 제보를 선택해 문의 API로 접수한다.
 * 대상 행사 ID를 별도 필드로 보내 관리자가 상세 화면에서 바로 이동할 수 있게 한다.
 */
function FestivalReportButton({ event }: { event: EventView }) {
  const { member, authLoading } = useApp();
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  function start() {
    if (!member) {
      navigate(
        `/login?next=${encodeURIComponent(location.pathname + location.search)}`,
      );
      return;
    }
    setOpen(true);
  }
  return (
    <>
      <button
        type="button"
        className="report-link"
        disabled={authLoading}
        onClick={start}
      >
        <Flag size={14} />이 행사 신고 및 제보
      </button>
      {open && (
        <Modal title="행사 신고 및 제보" onClose={() => setOpen(false)}>
          <FestivalReportForm event={event} onDone={() => setOpen(false)} />
        </Modal>
      )}
    </>
  );
}

function FestivalReportForm({
  event,
  onDone,
}: {
  event: EventView;
  onDone: () => void;
}) {
  const { api, toast } = useApp();
  const [kind, setKind] = useState<FestivalInquiryKind | null>(null);
  const [reason, setReason] = useState<string | null>(null);
  const [detail, setDetail] = useState("");
  const [img, setImg] = useState<string | undefined>();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const detailRequired = reason === "OTHER";

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!kind) {
      setError("신고와 제보 중 접수 유형을 선택해 주세요.");
      return;
    }
    if (!reason) {
      setError(`${kind === "REPORT" ? "신고" : "제보"} 사유를 선택해 주세요.`);
      return;
    }
    if (detailRequired && !detail.trim()) {
      setError("기타 사유는 내용을 적어 주세요.");
      return;
    }
    const kindLabel = kind === "REPORT" ? "신고" : "제보";
    const label = inquiryReasons[kind].find(
      (item) => item.value === reason,
    )!.label;
    setBusy(true);
    setError("");
    try {
      await api.createInquiry({
        category: kind,
        targetType: "FESTIVAL",
        targetId: String(event.festivalId),
        title: `[행사 ${kindLabel}] ${event.title} - ${label}`.slice(0, 255),
        content: [
          `${kindLabel} 사유: ${label}`,
          `행사: ${event.title} (ID ${event.festivalId})`,
          ...(detail.trim() ? ["", detail.trim()] : []),
        ].join("\n"),
        ...(img ? { img } : {}),
      });
      toast(
        `${kindLabel}가 접수됐어요. 마이페이지에서 처리 결과를 볼 수 있어요.`,
      );
      onDone();
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="report-form" onSubmit={submit}>
      <p className="muted">
        <strong>{event.title}</strong>에 대해 어떤 내용을 접수하시나요?
      </p>
      <fieldset className="report-reasons report-kinds">
        <legend>
          접수 유형<b className="required">*</b>
        </legend>
        {inquiryKinds.map((item) => (
          <label
            key={item.value}
            className={kind === item.value ? "selected" : ""}
          >
            <input
              type="radio"
              name="inquiry-kind"
              value={item.value}
              checked={kind === item.value}
              onChange={() => {
                setKind(item.value);
                setReason(null);
                setError("");
              }}
            />
            <span>
              <strong>{item.label}</strong>
              <small>{item.description}</small>
            </span>
          </label>
        ))}
      </fieldset>
      {kind && (
        <fieldset className="report-reasons">
          <legend>
            {kind === "REPORT" ? "신고" : "제보"} 사유
            <b className="required">*</b>
          </legend>
          {inquiryReasons[kind].map((r) => (
            <label
              key={r.value}
              className={reason === r.value ? "selected" : ""}
            >
              <input
                type="radio"
                name="report-reason"
                value={r.value}
                checked={reason === r.value}
                onChange={() => setReason(r.value)}
              />
              {r.label}
            </label>
          ))}
        </fieldset>
      )}
      <Field
        label="상세 내용"
        required={detailRequired}
        hint={detailRequired ? undefined : "선택 항목이에요."}
      >
        <textarea
          rows={4}
          maxLength={2000}
          value={detail}
          onChange={(e) => setDetail(e.target.value)}
          placeholder="확인한 내용을 적어 주시면 처리에 도움이 돼요."
        />
      </Field>
      <Upload type="INQUIRY" onChange={setImg} useKey />
      <FormError message={error} />
      <div className="form-actions">
        <button type="button" className="btn secondary" onClick={onDone}>
          취소
        </button>
        <SubmitButton busy={busy}>
          {kind
            ? `${kind === "REPORT" ? "신고" : "제보"} 접수하기`
            : "접수하기"}
        </SubmitButton>
      </div>
    </form>
  );
}

function AccuracyVotePanel({
  festivalId,
  initialVote,
}: {
  festivalId: number;
  initialVote?: FestivalAccuracyVote;
}) {
  const { api, member, authLoading, toast } = useApp();
  const [data, setData] = useState<FestivalAccuracyVote>(
    initialVote ?? {
      accurateCount: 0,
      inaccurateCount: 0,
      myVote: null,
    },
  );
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function select(voteType: "ACCURATE" | "INACCURATE") {
    if (!member) {
      toast("로그인하면 행사 정보를 평가할 수 있어요.");
      return;
    }

    setBusy(true);
    setError("");
    try {
      const result =
        data?.myVote === voteType
          ? await api.cancelAccuracyVote(festivalId)
          : await api.voteAccuracy(festivalId, voteType);
      setData(result);
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="accuracy-vote" aria-labelledby="accuracy-vote-title">
      <div>
        <h2 id="accuracy-vote-title">이 행사 정보가 정확한가요?</h2>
        <p>직접 확인한 정보를 알려주시면 다른 사용자에게 도움이 돼요.</p>
      </div>
      <div className="accuracy-vote-actions">
        <button
          type="button"
          className={`accurate ${data?.myVote === "ACCURATE" ? "selected" : ""}`}
          aria-pressed={data?.myVote === "ACCURATE"}
          disabled={busy || authLoading}
          onClick={() => select("ACCURATE")}
        >
          <CheckCircle2 size={19} />
          정확해요
          <strong>{data?.accurateCount ?? 0}</strong>
        </button>
        <button
          type="button"
          className={`inaccurate ${data?.myVote === "INACCURATE" ? "selected" : ""}`}
          aria-pressed={data?.myVote === "INACCURATE"}
          disabled={busy || authLoading}
          onClick={() => select("INACCURATE")}
        >
          <AlertTriangle size={19} />
          부정확해요
          <strong>{data?.inaccurateCount ?? 0}</strong>
        </button>
      </div>
      {!member && !authLoading && (
        <p className="accuracy-vote-login">로그인하면 평가할 수 있어요.</p>
      )}
      <FormError message={error} />
    </section>
  );
}
