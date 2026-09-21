import { useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  ArrowRight,
  CalendarDays,
  Check,
  ChevronRight,
  Lightbulb,
  MapPin,
  Plus,
  Search,
  Send,
  UsersRound,
} from "lucide-react";
import { useApp, useLoad } from "../lib/context";
import { readDemo } from "../lib/demo";
import { dateText, errorText, period, regions } from "../lib/format";
import type { SubmissionInput, SubmissionDetail } from "../lib/types";
import {
  Badge,
  Empty,
  EventCard,
  ErrorState,
  Field,
  FormError,
  Loading,
  LoginRequired,
  PageTitle,
  Pagination,
  Photo,
  SubmitButton,
  Upload,
} from "../components/ui";

export function Submissions() {
  const { member, authLoading, mode } = useApp();
  return (
    <div className="container page-space">
      <PageTitle
        eyebrow="GOOD THINGS, TOGETHER"
        title="행사 제보"
        description="이웃들이 전한 행사를 둘러보고, 알고 있는 소식도 함께 나눠요."
        action={
          <Link className="btn primary" to="/submissions/new">
            <Plus size={17} />
            행사 제보하기
          </Link>
        }
      />
      <div className="submission-intro">
        <span className="route-icon sage">
          <UsersRound size={31} />
        </span>
        <div>
          <h3>작은 소식이 누군가에겐 특별한 하루가 돼요.</h3>
          <p>
            동네 플리마켓부터 이색 체험까지, 함께 나누고 싶은 행사를 알려주세요.
          </p>
        </div>
        <span className="intro-flower" aria-hidden="true">
          ✳
        </span>
      </div>
      {authLoading ? (
        <Loading />
      ) : !member ? (
        <LoginRequired />
      ) : mode === "api" ? (
        <Empty
          title="이웃의 행사 제보 조회를 준비하고 있어요"
          description="디자인 미리보기에서 이웃들이 전한 행사를 둘러볼 수 있어요."
        />
      ) : (
        <CommunitySubmissions />
      )}
    </div>
  );
}
function CommunitySubmissions() {
  const { api } = useApp();
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("ALL");
  const [page, setPage] = useState(0);
  const { data, loading, error, reload } = useLoad(
    () => api.submittedEvents(page, query, status),
    [api, page, query, status],
  );
  return (
    <>
      <div className="section-heading">
        <h2>
          이웃이 전한 행사{" "}
          <span className="count">{data?.totalElements ?? 0}</span>
        </h2>
        <select
          className="plain-select"
          aria-label="제보 행사 상태 필터"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(0);
          }}
        >
          <option value="ALL">모든 일정</option>
          <option value="OPEN">종료 전</option>
          <option value="CLOSED">종료</option>
        </select>
      </div>
      <div className="inline-search">
        <Search size={18} />
        <input
          aria-label="제보된 행사 검색"
          placeholder="이웃이 제보한 행사 검색"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setPage(0);
          }}
        />
      </div>
      {loading ? (
        <Loading cards />
      ) : error ? (
        <ErrorState message={error} retry={reload} />
      ) : data?.content.length ? (
        <>
          <div className="event-grid">
            {data.content.map((event) => (
              <EventCard event={event} key={event.festivalId} />
            ))}
          </div>
          <Pagination page={page} total={data.totalPages} onChange={setPage} />
        </>
      ) : (
        <Empty
          title="조건에 맞는 제보가 없어요"
          description="다른 검색 조건으로 찾아보거나 새로운 행사를 제보해 주세요."
        />
      )}
    </>
  );
}
export function SubmissionList({ compact = false }: { compact?: boolean }) {
  const { api, mode } = useApp();
  const { data, loading, error, reload } = useLoad(
    () => api.submissions(),
    [api],
  );
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("ALL");
  const list = (data || []).filter(
    (s) =>
      (!query || s.title.toLowerCase().includes(query.toLowerCase())) &&
      (status === "ALL" || s.status === status),
  );
  return (
    <>
      <div className="section-heading">
        <div>
          <h2>
            내가 제보한 행사 <span className="count">{data?.length || 0}</span>
          </h2>
          {!compact && (
            <p className="muted">내가 전한 즐거운 소식을 확인해 보세요.</p>
          )}
        </div>
        <select
          className="plain-select"
          aria-label="내 제보 상태 필터"
          value={status}
          onChange={(e) => setStatus(e.target.value)}
        >
          <option value="ALL">모든 일정</option>
          <option value="OPEN">종료 전</option>
          <option value="CLOSED">종료</option>
        </select>
      </div>
      <div className="inline-search">
        <Search size={18} />
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="내가 제보한 행사 검색"
          aria-label="내 제보 검색"
        />
      </div>
      {loading ? (
        <Loading cards />
      ) : error ? (
        <ErrorState message={error} retry={reload} />
      ) : list.length ? (
        <div className="submission-list">
          {list.map((s) => {
            const image =
              mode === "preview"
                ? readDemo().submissions.find(
                    (x) =>
                      x.submission.festivalSubmissionId ===
                      s.festivalSubmissionId,
                  )?.submission.festival.imgUrl
                : null;
            return (
              <Link
                className="submission-row"
                key={s.festivalSubmissionId}
                to={`/submissions/${s.festivalSubmissionId}`}
              >
                <Photo src={image} alt={s.title} />
                <div className="submission-row-copy">
                  <div>
                    <Badge tone={s.status === "CLOSED" ? "gray" : "green"}>
                      {s.status === "CLOSED" ? "종료" : "종료 전"}
                    </Badge>
                    {mode === "preview" && (
                      <span className="example-label">예시 제보</span>
                    )}
                  </div>
                  <h3>{s.title}</h3>
                  <p>
                    <MapPin size={14} />
                    {regions[s.region] || s.region} {s.regionDetail}
                  </p>
                  <p>
                    <CalendarDays size={14} />
                    {period(s.beginDe, s.endDe)}
                  </p>
                </div>
                <span className="row-date">{dateText(s.createdAt)} 제보</span>
                <ChevronRight size={19} />
              </Link>
            );
          })}
        </div>
      ) : (
        <Empty
          title={
            query || status !== "ALL"
              ? "조건에 맞는 제보가 없어요"
              : "아직 제보한 행사가 없어요"
          }
          description="알고 있는 행사를 나누며 첫 번째 이야기를 시작해 보세요."
          action={
            <Link className="btn primary" to="/submissions/new">
              <Plus size={16} />
              행사 제보하기
            </Link>
          }
        />
      )}
    </>
  );
}

export function SubmissionFormPage() {
  const { submissionId } = useParams();
  const { member, authLoading } = useApp();
  return (
    <div className="container page-space">
      <Link
        className="back-link"
        to={submissionId ? `/submissions/${submissionId}` : "/submissions"}
      >
        <ArrowLeft size={16} />
        제보 {submissionId ? "상세" : "목록"}로
      </Link>
      <PageTitle
        eyebrow="SHARE A LITTLE JOY"
        title={submissionId ? "행사 제보 수정" : "새로운 즐거움을 알려주세요"}
        description="정확한 행사 정보는 함께 즐길 수 있는 하루의 시작이에요."
      />
      {authLoading ? (
        <Loading />
      ) : !member ? (
        <LoginRequired />
      ) : member.role === "ROLE_WARNING" ? (
        <Empty
          title="현재 행사 제보가 제한되어 있어요"
          description="활동 등급에 대한 문의는 마이페이지에서 남겨주세요."
          action={
            <Link className="btn secondary" to="/mypage?tab=inquiries">
              문의하기
            </Link>
          }
        />
      ) : submissionId ? (
        <EditSubmission id={submissionId} />
      ) : (
        <SubmissionForm />
      )}
    </div>
  );
}
function EditSubmission({ id }: { id: string }) {
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
    <SubmissionForm existing={data} />
  ) : null;
}
function SubmissionForm({ existing }: { existing?: SubmissionDetail }) {
  const { api, mode, toast } = useApp();
  const navigate = useNavigate();
  const current = existing?.submission;
  const [form, setForm] = useState<SubmissionInput>(() => ({
    title: "",
    category: "축제",
    manager: "",
    festivalContent: "",
    region: "GYEONGGI_SUWON",
    regionDetail: "",
    beginDe: "",
    endDe: "",
    eventTmInfo: "",
    submissionContent: "",
    instNm: "",
    referenceUrl: "",
    imgUrl: "",
    partcptExpnInfo: "",
    telnoInfo: "",
    hostInstNm: "",
    ...(current
      ? Object.fromEntries(
          Object.entries(current.festival).map(([k, v]) => [k, v ?? ""]),
        )
      : {}),
    ...(current ? { submissionContent: current.submissionContent } : {}),
  }));
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [confirmed, setConfirmed] = useState(false);
  const change = (name: keyof SubmissionInput, value: string) =>
    setForm((f) => ({ ...f, [name]: value }));
  async function submit(e: FormEvent) {
    e.preventDefault();
    setError("");
    if (new Date(form.endDe) < new Date(form.beginDe)) {
      setError("종료 일시는 시작 일시보다 빠를 수 없어요.");
      return;
    }
    if (!confirmed) {
      setError("행사 정보 확인에 동의해 주세요.");
      return;
    }
    setBusy(true);
    try {
      const payload: SubmissionInput = {
        title: form.title.trim(),
        category: form.category.trim(),
        manager: form.manager.trim(),
        festivalContent: form.festivalContent.trim(),
        region: form.region,
        regionDetail: form.regionDetail.trim(),
        beginDe:
          form.beginDe.length === 16 ? `${form.beginDe}:00` : form.beginDe,
        endDe: form.endDe.length === 16 ? `${form.endDe}:00` : form.endDe,
        eventTmInfo: form.eventTmInfo.trim(),
        submissionContent: form.submissionContent.trim(),
        instNm: form.instNm?.trim(),
        referenceUrl: form.referenceUrl?.trim(),
        imgUrl: form.imgUrl,
        partcptExpnInfo: form.partcptExpnInfo?.trim(),
        telnoInfo: form.telnoInfo?.trim(),
        hostInstNm: form.hostInstNm?.trim(),
      };
      let id = current?.festivalSubmissionId;
      if (id) await api.updateSubmission(id, payload);
      else id = (await api.createSubmission(payload)).festivalSubmissionId;
      toast(
        mode === "preview"
          ? "미리보기 제보가 저장되었어요. 실제 서버에는 전송하지 않았어요."
          : "행사 제보를 저장했어요.",
      );
      navigate(`/submissions/${id}`);
    } catch (err) {
      setError(errorText(err));
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="form-layout">
      <form className="editor-form" onSubmit={submit}>
        <section className="form-section">
          <div className="form-section-title">
            <span>01</span>
            <h2>어떤 행사인가요?</h2>
            <small>* 필수 입력</small>
          </div>
          <div className="form-grid">
            <Field label="행사 이름" required wide>
              <input
                required
                value={form.title}
                onChange={(e) => change("title", e.target.value)}
                placeholder="행사 이름을 알려주세요"
              />
            </Field>
            <Field label="행사 종류" required>
              <input
                list="event-categories"
                required
                maxLength={50}
                value={form.category}
                onChange={(e) => change("category", e.target.value)}
              />
              <datalist id="event-categories">
                {["축제", "공연", "전시", "체험", "플리마켓", "교육"].map(
                  (c) => (
                    <option key={c} value={c} />
                  ),
                )}
              </datalist>
            </Field>
            <Field label="행사 담당자" required>
              <input
                required
                value={form.manager}
                onChange={(e) => change("manager", e.target.value)}
                placeholder="담당자 또는 운영팀"
              />
            </Field>
            <Field label="주최 기관">
              <input
                value={form.hostInstNm || ""}
                onChange={(e) => change("hostInstNm", e.target.value)}
                placeholder="행사를 주최하는 곳"
              />
            </Field>
            <Field label="운영 기관">
              <input
                value={form.instNm || ""}
                onChange={(e) => change("instNm", e.target.value)}
                placeholder="행사를 운영하는 곳"
              />
            </Field>
            <Field label="행사 소개" required wide>
              <textarea
                required
                rows={6}
                value={form.festivalContent}
                onChange={(e) => change("festivalContent", e.target.value)}
                placeholder="행사의 내용과 즐길 거리를 자세히 소개해 주세요."
              />
            </Field>
            <div className="wide">
              <span className="upload-label">행사 사진</span>
              <Upload
                type="FESTIVAL"
                value={form.imgUrl}
                onChange={(value) => change("imgUrl", value)}
              />
            </div>
          </div>
        </section>
        <section className="form-section">
          <div className="form-section-title">
            <span>02</span>
            <h2>언제, 어디서 열리나요?</h2>
          </div>
          <div className="form-grid">
            <Field label="시작 일시" required>
              <input
                required
                type="datetime-local"
                value={form.beginDe.slice(0, 16)}
                onChange={(e) => change("beginDe", e.target.value)}
              />
            </Field>
            <Field label="종료 일시" required>
              <input
                required
                type="datetime-local"
                min={form.beginDe.slice(0, 16)}
                value={form.endDe.slice(0, 16)}
                onChange={(e) => change("endDe", e.target.value)}
              />
            </Field>
            <Field label="운영 시간 안내" required wide>
              <input
                required
                value={form.eventTmInfo}
                onChange={(e) => change("eventTmInfo", e.target.value)}
                placeholder="예: 매일 10:00 ~ 18:00, 월요일 휴무"
              />
            </Field>
            <Field label="지역" required>
              <select
                required
                value={form.region}
                onChange={(e) => change("region", e.target.value)}
              >
                {Object.entries(regions).map(([key, name]) => (
                  <option key={key} value={key}>
                    {name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="상세 주소" required>
              <input
                required
                maxLength={255}
                value={form.regionDetail}
                onChange={(e) => change("regionDetail", e.target.value)}
                placeholder="도로명 주소, 장소 이름"
              />
            </Field>
            <Field label="참가 비용">
              <input
                value={form.partcptExpnInfo || ""}
                onChange={(e) => change("partcptExpnInfo", e.target.value)}
                placeholder="예: 무료 / 1인 10,000원"
              />
            </Field>
            <Field label="문의 연락처">
              <input
                type="tel"
                value={form.telnoInfo || ""}
                onChange={(e) => change("telnoInfo", e.target.value)}
                placeholder="행사 관련 문의가 가능한 번호"
              />
            </Field>
            <Field label="행사 참고 링크" wide>
              <input
                type="url"
                maxLength={2048}
                value={form.referenceUrl || ""}
                onChange={(e) => change("referenceUrl", e.target.value)}
                placeholder="https://"
              />
            </Field>
          </div>
        </section>
        <section className="form-section">
          <div className="form-section-title">
            <span>03</span>
            <h2>제보 이야기를 남겨주세요</h2>
          </div>
          <Field
            label="제보 내용"
            required
            hint="행사 소개와 별도로, 제보하게 된 이유나 확인한 정보를 알려주세요."
          >
            <textarea
              required
              rows={4}
              value={form.submissionContent}
              onChange={(e) => change("submissionContent", e.target.value)}
              placeholder="이 행사를 어떻게 알게 되었나요?"
            />
          </Field>
        </section>
        <label className="checkbox-label">
          <input
            type="checkbox"
            required
            checked={confirmed}
            onChange={(e) => setConfirmed(e.target.checked)}
          />
          일정과 장소를 확인했으며, 정확한 정보를 제보합니다.
        </label>
        <FormError message={error} />
        <div className="form-actions">
          <Link
            className="btn secondary"
            to={
              current
                ? `/submissions/${current.festivalSubmissionId}`
                : "/submissions"
            }
          >
            취소
          </Link>
          <SubmitButton busy={busy}>
            <Send size={16} />
            {current ? "수정한 내용 저장" : "행사 제보하기"}
          </SubmitButton>
        </div>
      </form>
      <aside className="form-aside">
        <div>
          <Lightbulb size={28} />
          <h3>좋은 제보를 위한 작은 팁</h3>
          <p>
            <Check size={16} />
            행사 이름을 정확하게 적어주세요.
          </p>
          <p>
            <Check size={16} />
            일정과 장소를 한 번 더 확인해요.
          </p>
          <p>
            <Check size={16} />
            공식 안내 링크가 있으면 좋아요.
          </p>
          <p>
            <Check size={16} />
            직접 촬영한 사진도 환영해요.
          </p>
          <span>
            함께 만드는 즐거운 일상,
            <br />
            당신의 제보로 시작됩니다.
          </span>
        </div>
        {mode === "preview" && (
          <div className="preview-note">
            지금은 미리보기예요.
            <br />
            저장한 제보는 이 탭에서만 확인할 수 있어요.
          </div>
        )}
      </aside>
    </div>
  );
}
