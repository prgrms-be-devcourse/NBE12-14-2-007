import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  AlertTriangle,
  CalendarDays,
  Check,
  CheckCircle2,
  Clock3,
  Flag,
  Landmark,
  MapPin,
  Pencil,
  Phone,
  Share2,
  Ticket,
  Trash2,
  UsersRound,
} from "lucide-react";
import { useApp, useLoad } from "../lib/context";
import {
  errorText,
  eventState,
  imageUrl,
  period,
  regions,
  safeUrl,
} from "../lib/format";
import type { EventView, FestivalAccuracyVote } from "../lib/types";
import {
  Badge,
  Empty,
  ErrorState,
  FormError,
  Loading,
  LoginRequired,
  Modal,
} from "../components/ui";
import { FestivalPosts } from "./Reviews";

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
            {event.submitter && (
              <p className="muted small-text">
                제보자 · {event.submitter.nickname}
              </p>
            )}
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
                  <p className="prose">
                    {event.festivalContent ||
                      "아직 등록된 상세 소개가 없어요. 행사 참고 링크에서 자세한 내용을 확인해 주세요."}
                  </p>
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
          <Link
            className="report-link"
            to={`/mypage?tab=inquiries&report=${encodeURIComponent(`행사 정보 문의: ${event.title}\n행사 주소: ${window.location.href}`)}`}
          >
            <Flag size={14} />
            잘못된 정보가 있나요?
          </Link>
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
          className={data?.myVote === "ACCURATE" ? "selected accurate" : ""}
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
          className={data?.myVote === "INACCURATE" ? "selected inaccurate" : ""}
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
