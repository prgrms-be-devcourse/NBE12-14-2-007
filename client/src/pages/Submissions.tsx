import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  CalendarDays,
  Check,
  ChevronRight,
  Lightbulb,
  MapPin,
  Plus,
  Search,
  Send,
} from "lucide-react";
import { useApp, useLoad } from "../lib/context";
import { dateText, errorText, period, regions } from "../lib/format";
import { selectedRegion } from "../lib/regions";
import { RegionSelects } from "../components/RegionSelects";
import { LazyRichTextEditor } from "../components/LazyRichTextEditor";
import type {
  EventView,
  SubmissionInput,
  SubmissionDetail,
} from "../lib/types";
import {
  Badge,
  Empty,
  ErrorState,
  Field,
  FormError,
  Loading,
  LoginRequired,
  PageTitle,
  Photo,
  SubmitButton,
  Upload,
} from "../components/ui";

export function SubmissionList({ compact = false }: { compact?: boolean }) {
  const { api } = useApp();
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
            return (
              <Link
                className="submission-row"
                key={s.festivalSubmissionId}
                to={`/submissions/${s.festivalSubmissionId}`}
              >
                <Photo src={null} alt={s.title} />
                <div className="submission-row-copy">
                  <div>
                    <Badge tone={s.status === "CLOSED" ? "gray" : "green"}>
                      {s.status === "CLOSED" ? "종료" : "종료 전"}
                    </Badge>
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
        to={submissionId ? `/submissions/${submissionId}` : "/explore"}
      >
        <ArrowLeft size={16} />
        {submissionId ? "제보 상세로" : "행사 목록으로"}
      </Link>
      <PageTitle
        eyebrow="SHARE A LITTLE JOY"
        title={
          submissionId
            ? "행사 제보 수정"
            : "당신의 제보가 새로운 탈출의 시작이에요"
        }
        description="알고 있는 행사 정보를 이웃과 공유해 주세요."
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
  const { api, toast } = useApp();
  const navigate = useNavigate();
  const current = existing?.submission;
  const [form, setForm] = useState<SubmissionInput>(() => ({
    title: "",
    category: "",
    festivalContent: "",
    region: "",
    regionDetail: "",
    beginDe: "",
    endDe: "",
    eventTmInfo: "",
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
    ...(current
      ? {
          title: current.festival.title,
          region:
            selectedRegion(current.festival.region).province?.value ||
            current.festival.region,
        }
      : {}),
  }));
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [confirmed, setConfirmed] = useState(false);
  const [titleFocused, setTitleFocused] = useState(false);
  const [suggestions, setSuggestions] = useState<EventView[]>([]);
  const [suggestionsLoading, setSuggestionsLoading] = useState(false);
  const editing = Boolean(current);
  const change = (name: keyof SubmissionInput, value: string) =>
    setForm((f) => ({ ...f, [name]: value }));

  useEffect(() => {
    const keyword = form.title.trim();

    if (editing || keyword.length < 2) {
      setSuggestions([]);
      setSuggestionsLoading(false);
      return;
    }

    let active = true;
    const timer = window.setTimeout(() => {
      setSuggestionsLoading(true);
      api
        .festivals({ keyword, page: 0 })
        .then((page) => {
          if (!active) return;

          const normalizedKeyword = keyword.toLocaleLowerCase("ko-KR");
          const titleMatches = page.content.filter((festival) =>
            festival.title
              .toLocaleLowerCase("ko-KR")
              .includes(normalizedKeyword),
          );

          setSuggestions(titleMatches.slice(0, 5));
        })
        .catch(() => {
          if (active) setSuggestions([]);
        })
        .finally(() => {
          if (active) setSuggestionsLoading(false);
        });
    }, 400);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [api, editing, form.title]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (busy) return;
    setError("");
    const requiredFields = {
      title: "행사 이름",
      category: "행사 종류",
      region: "지역",
      beginDe: "시작 일시",
      endDe: "종료 일시",
      referenceUrl: "행사 참고 링크",
    } as const;
    for (const [key, label] of Object.entries(requiredFields)) {
      if (!form[key as keyof typeof requiredFields].trim()) {
        setError(`${label} 항목을 입력해 주세요.`);
        return;
      }
    }
    if (form.endDe < form.beginDe) {
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
        festivalContent: form.festivalContent?.trim(),
        region: form.region,
        regionDetail: form.regionDetail?.trim(),
        beginDe: `${form.beginDe.slice(0, 10)}T00:00:00`,
        endDe: `${form.endDe.slice(0, 10)}T23:59:59`,
        eventTmInfo: form.eventTmInfo?.trim(),
        instNm: form.instNm?.trim(),
        referenceUrl: form.referenceUrl.trim(),
        imgUrl: form.imgUrl,
        partcptExpnInfo: form.partcptExpnInfo?.trim(),
        telnoInfo: form.telnoInfo?.trim(),
        hostInstNm: form.hostInstNm?.trim(),
      };
      let id = current?.festivalSubmissionId;
      if (id) await api.updateSubmission(id, payload);
      else id = (await api.createSubmission(payload)).festivalSubmissionId;
      toast("행사 제보를 저장했어요.");
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
        <p className="muted">* 표시된 항목은 필수입니다.</p>
        <section className="form-section">
          <div className="form-section-title">
            <span>01</span>
            <h2>어떤 행사인가요?</h2>
            <small>* 필수 입력</small>
          </div>
          <div className="form-grid">
            <Field label="행사 이름" required wide>
              <div className="festival-title-autocomplete">
                <input
                  required
                  value={form.title}
                  onChange={(e) => change("title", e.target.value)}
                  onFocus={() => setTitleFocused(true)}
                  onBlur={() => setTitleFocused(false)}
                  placeholder="행사 이름을 알려주세요"
                  autoComplete="off"
                  aria-autocomplete="list"
                  aria-expanded={
                    titleFocused &&
                    !editing &&
                    (suggestionsLoading || suggestions.length > 0)
                  }
                  aria-controls="festival-title-suggestions"
                />
                {titleFocused &&
                  !editing &&
                  form.title.trim().length >= 2 &&
                  (suggestionsLoading || suggestions.length > 0) && (
                    <div
                      className="festival-title-suggestions"
                      id="festival-title-suggestions"
                    >
                      <p>
                        {suggestionsLoading
                          ? "비슷한 행사를 찾고 있어요"
                          : "비슷한 행사가 이미 있어요"}
                      </p>
                      {!suggestionsLoading &&
                        suggestions.map((festival) => (
                          <Link
                            key={festival.festivalId}
                            to={`/events/${festival.festivalId}`}
                            onMouseDown={(e) => e.preventDefault()}
                          >
                            <span>
                              <strong>{festival.title}</strong>
                              <small>
                                {period(festival.beginDe, festival.endDe)} ·{" "}
                                {regions[festival.region] || festival.region}
                              </small>
                            </span>
                            <ChevronRight size={16} aria-hidden="true" />
                          </Link>
                        ))}
                    </div>
                  )}
              </div>
            </Field>
            <div className="field wide">
              <span>
                행사 종류<b className="required">*</b>
              </span>
              <div
                className="category-tabs"
                role="group"
                aria-label="행사 종류"
              >
                {[
                  "축제",
                  "공연",
                  "전시",
                  "체험",
                  "플리마켓",
                  "교육",
                  "기타",
                ].map((category) => (
                  <button
                    type="button"
                    key={category}
                    className={form.category === category ? "active" : ""}
                    aria-pressed={form.category === category}
                    onClick={() => change("category", category)}
                  >
                    {category}
                  </button>
                ))}
              </div>
            </div>
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
            <Field label="행사 소개" wide>
              <LazyRichTextEditor
                value={form.festivalContent || ""}
                onChange={(value) => change("festivalContent", value)}
                placeholder="행사의 내용과 즐길 거리를 자세히 소개해 주세요."
                ariaLabel="행사 소개"
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
            <Field label="시작일" required>
              <input
                required
                type="date"
                value={form.beginDe.slice(0, 10)}
                onChange={(e) => change("beginDe", e.target.value)}
                onClick={(e) => e.currentTarget.showPicker?.()}
              />
            </Field>
            <Field label="종료일" required>
              <input
                required
                type="date"
                min={form.beginDe.slice(0, 10)}
                value={form.endDe.slice(0, 10)}
                onChange={(e) => change("endDe", e.target.value)}
                onClick={(e) => e.currentTarget.showPicker?.()}
              />
            </Field>
            <Field label="운영 시간 안내" wide>
              <input
                value={form.eventTmInfo || ""}
                onChange={(e) => change("eventTmInfo", e.target.value)}
                placeholder="예: 매일 10:00 ~ 18:00, 월요일 휴무"
              />
            </Field>
            <div className="field wide">
              <span>행사 지역</span>
              <div className="submission-region-selects">
                <RegionSelects
                  value={form.region}
                  onChange={(value) => change("region", value)}
                  variant="form"
                />
              </div>
            </div>
            <Field label="상세 주소">
              <input
                maxLength={255}
                value={form.regionDetail || ""}
                onChange={(e) => change("regionDetail", e.target.value)}
                placeholder="도로명 주소, 장소 이름"
              />
            </Field>
            <Field
              label="참가 비용"
              hint="비용을 모르면 비워두세요. 무료 행사라면 '무료'라고 적어주세요."
            >
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
            <Field
              label="행사 참고 링크"
              required
              wide
              hint="내용과 일정을 확인할 수 있는 행사 홈페이지나 SNS 안내 링크를 넣어주세요."
            >
              <input
                required
                type="url"
                maxLength={2048}
                value={form.referenceUrl || ""}
                onChange={(e) => change("referenceUrl", e.target.value)}
                placeholder="https://"
              />
            </Field>
          </div>
        </section>
        <label className="checkbox-label">
          <input
            type="checkbox"
            required
            checked={confirmed}
            onChange={(e) => setConfirmed(e.target.checked)}
          />
          행사 일정과 안내 링크를 확인했으며, 정확한 정보를 제보합니다.
        </label>
        <FormError message={error} />
        <div className="form-actions">
          <Link
            className="btn secondary"
            to={
              current
                ? `/submissions/${current.festivalSubmissionId}`
                : "/explore"
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
          <h3>방구석 탈출 제보 TIP</h3>
          <p>
            <Check size={16} />
            정확한 행사 이름!
          </p>
          <p>
            <Check size={16} />
            확실한 일정과 장소!
          </p>
          <p>
            <Check size={16} />
            믿을 수 있는 안내 링크!
          </p>
          <p>
            <Check size={16} />
            현장 사진은 보너스!
          </p>
          <span>당신의 제보로 탈출 준비 완료!</span>
        </div>
      </aside>
    </div>
  );
}
