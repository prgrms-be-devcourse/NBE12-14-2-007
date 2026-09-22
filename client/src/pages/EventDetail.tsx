import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  ArrowUpRight,
  CalendarDays,
  Check,
  Clock3,
  ExternalLink,
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
import { errorText, eventState, period, regions, safeUrl } from "../lib/format";
import type { EventView } from "../lib/types";
import {
  Badge,
  Empty,
  ErrorState,
  FormError,
  Loading,
  LoginRequired,
  Modal,
  Photo,
} from "../components/ui";
import { FestivalPosts } from "./Reviews";

export function EventDetailPage() {
  const { eventId, submissionId } = useParams();
  const { mode, member, authLoading } = useApp();
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
      ) : mode === "api" && !member ? (
        <LoginRequired />
      ) : mode === "api" ? (
        <Empty
          title="행사 상세 조회를 준비하고 있어요"
          description="디자인 미리보기에서 행사 정보를 확인할 수 있어요."
        />
      ) : (
        <PublicEventLoader key={eventId} id={Number(eventId)} />
      )}
    </div>
  );
}
function PublicEventLoader({ id }: { id: number }) {
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
      <div className="detail-hero">
        <Photo src={event.imgUrl} alt={event.title} />
        <div className="detail-hero-overlay" />
        <div className="detail-hero-copy">
          <span>
            {event.source === "PUBLIC"
              ? "CULTURE NEAR YOU"
              : "GOOD THINGS, TOGETHER"}
          </span>
          <p>일상 밖, 새로운 즐거움을 만나는 시간.</p>
        </div>
        {event.preview && (
          <span className="photo-example">미리보기 예시 이미지</span>
        )}
      </div>
      <div className="detail-layout">
        <div className="detail-main">
          <div className="detail-title">
            <div className="action-row">
              <Badge tone={event.source === "PUBLIC" ? "blue" : "orange"}>
                {event.source === "PUBLIC" ? "지역 문화행사" : "회원 제보"}
              </Badge>
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
                  <h2>이런 즐거움이 기다려요</h2>
                  <p className="prose">
                    {event.festivalContent ||
                      "아직 등록된 상세 소개가 없어요. 행사 참고 링크에서 자세한 내용을 확인해 주세요."}
                  </p>
                </section>
                <section className="detail-section">
                  <h2>방문 전에 확인해 주세요</h2>
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
                        {regions[event.region] || ""}{" "}
                        {event.regionDetail || "별도 안내 없음"}
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
                          ? "이웃이 제보한 행사 정보입니다. 방문 전 행사 안내를 확인해 주세요."
                          : "제공 기관의 행사 정보입니다. 방문 전 행사 안내를 확인해 주세요."}
                  </p>
                </div>
              </>
            ) : (
              <FestivalPosts festivalId={event.festivalId} />
            )}
          </div>
        </div>
        <aside className="detail-aside">
          <div className="visit-card">
            <span className="eyebrow">PLAN YOUR DAY</span>
            <h3>즐거운 하루를 준비해요</h3>
            <div>
              <CalendarDays size={18} />
              <span>
                <small>언제</small>
                {period(event.beginDe, event.endDe)}
              </span>
            </div>
            <div>
              <MapPin size={18} />
              <span>
                <small>어디서</small>
                {event.regionDetail ||
                  regions[event.region] ||
                  "장소 안내 확인"}
              </span>
            </div>
            <div>
              <Ticket size={18} />
              <span>
                <small>참가 비용</small>
                {event.partcptExpnInfo || "별도 안내 없음"}
              </span>
            </div>
            {externalUrl ? (
              <a
                className="btn primary full-width"
                href={externalUrl}
                target="_blank"
                rel="noopener noreferrer"
              >
                행사 안내 확인하기
                <ExternalLink size={16} />
              </a>
            ) : (
              <p className="aside-note">등록된 행사 참고 링크가 없어요.</p>
            )}
            <button
              className="btn secondary full-width"
              onClick={() => {
                setTab("reviews");
                document
                  .querySelector(".detail-tabs")
                  ?.scrollIntoView({ behavior: "smooth", block: "start" });
              }}
            >
              다녀온 이야기 보기
              <ArrowUpRight size={17} />
            </button>
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
        </aside>
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
