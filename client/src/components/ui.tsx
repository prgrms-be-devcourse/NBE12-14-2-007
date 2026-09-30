import { useEffect, useId, useRef, useState, type ReactNode } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  ArrowRight,
  CalendarDays,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  CircleAlert,
  AlertTriangle,
  Cloud,
  CloudRain,
  CloudSnow,
  Heart,
  ImagePlus,
  LoaderCircle,
  MapPin,
  Search,
  Sprout,
  Sun,
  X,
} from "lucide-react";
import { useApp } from "../lib/context";
import { errorText, imageUrl, period, regions } from "../lib/format";
import type { EventView, WeatherCondition } from "../lib/types";
import { TrustGradeBadge } from "./TrustGradeBadge";

export function Logo() {
  return (
    <span className="logo">
      <span className="logo-mark">
        <img src="/images/logo.png" alt="방구석탈출 로고" />
      </span>
      <span>방구석탈출</span>
    </span>
  );
}
const WEATHER_ICONS: Record<WeatherCondition, typeof Sun> = {
  SUNNY: Sun,
  CLOUDY: Cloud,
  RAIN: CloudRain,
  RAIN_SNOW: CloudSnow,
  SNOW: CloudSnow,
  UNKNOWN: Cloud,
};
export function WeatherIcon({
  condition,
  size = 18,
}: {
  condition: WeatherCondition;
  size?: number;
}) {
  const Icon = WEATHER_ICONS[condition];
  return <Icon size={size} aria-hidden="true" />;
}
const WEATHER_LABELS: Record<WeatherCondition, string> = {
  SUNNY: "맑음",
  CLOUDY: "흐림",
  RAIN: "비",
  RAIN_SNOW: "비/눈",
  SNOW: "눈",
  UNKNOWN: "",
};
/** UNKNOWN(예보 범위 밖·조회 실패)이면 아무것도 안 그린다. */
export function WeatherBadge({
  condition,
  precipitationProbability,
}: {
  condition: WeatherCondition;
  precipitationProbability?: number | null;
}) {
  if (condition === "UNKNOWN") return null;
  return (
    <span
      className={`weather-badge weather-${condition.toLowerCase()}`}
      title={
        precipitationProbability != null
          ? `강수확률 ${precipitationProbability}%`
          : undefined
      }
    >
      <WeatherIcon condition={condition} size={20} />
      {WEATHER_LABELS[condition]}
    </span>
  );
}
export function SectionTitle({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="section-heading">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h2>{title}</h2>
        {description && <p className="muted">{description}</p>}
      </div>
      {action}
    </div>
  );
}
export function PageTitle({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="page-heading">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
        {description && <p className="muted">{description}</p>}
      </div>
      {action}
    </div>
  );
}
export function Badge({
  children,
  tone = "orange",
}: {
  children: ReactNode;
  tone?: string;
}) {
  return <span className={`badge ${tone}`}>{children}</span>;
}
/** 행사 이미지가 없거나 불러오지 못했을 때 보여주는 기본 이미지 */
export const FESTIVAL_DEFAULT_IMAGE = "/images/festival-default.jpg";
/** 부정확해요가 10개 이상이면 목록·상세 썸네일을 검증 안 됨 이미지로 바꾼다. */
export const INACCURATE_THUMBNAIL_THRESHOLD = 10;
export const FESTIVAL_UNVERIFIED_IMAGE = "/images/festival-unverified.jpg";

export function Photo({
  src,
  alt,
  className = "",
  fallbackSrc,
}: {
  src?: string | null;
  alt: string;
  className?: string;
  /** 원본이 없거나 깨졌을 때 대신 보여줄 이미지. 이것마저 실패하면 기본 안내 화면을 보여준다. */
  fallbackSrc?: string;
}) {
  const [failed, setFailed] = useState(false);
  const [fallbackFailed, setFallbackFailed] = useState(false);
  useEffect(() => {
    setFailed(false);
    setFallbackFailed(false);
  }, [src, fallbackSrc]);
  const url = imageUrl(src);
  if (url && !failed)
    return (
      <img
        className={className}
        src={url}
        alt={alt}
        loading="lazy"
        onError={() => setFailed(true)}
      />
    );
  return fallbackSrc && !fallbackFailed ? (
    <img
      className={className}
      src={fallbackSrc}
      alt={alt}
      loading="lazy"
      onError={() => setFallbackFailed(true)}
    />
  ) : (
    <div className={`photo-fallback ${className}`} role="img" aria-label={alt}>
      <Sprout size={38} />
      <span>새로운 즐거움을 만나요</span>
    </div>
  );
}
export function EventCard({
  event,
  list = false,
}: {
  event: EventView;
  list?: boolean;
}) {
  const path = event.submissionId
    ? `/submissions/${event.submissionId}`
    : `/events/${event.festivalId}`;
  const unverified =
    (event.accuracyVote?.inaccurateCount ?? 0) >= INACCURATE_THUMBNAIL_THRESHOLD;
  return (
    <Link to={path} className={`event-card ${list ? "list-card" : ""}`}>
      <div className="event-photo">
        <Photo
          src={unverified ? FESTIVAL_UNVERIFIED_IMAGE : event.imgUrl}
          alt={unverified ? "검증되지 않은 행사입니다." : event.title}
          fallbackSrc={FESTIVAL_DEFAULT_IMAGE}
        />
        <span className="image-label">{event.category}</span>
      </div>
      <div className="event-copy">
        <div className="card-eyebrow">
          <span
            className={`source-badge ${event.source === "PUBLIC" ? "public" : "member"}`}
          >
            {event.source === "PUBLIC" ? "공공데이터" : "회원 제보"}
          </span>
          {event.status === "CLOSED" && <Badge tone="gray">종료</Badge>}
          {event.preview && <span className="example-label">예시</span>}
        </div>
        <h3>{event.title}</h3>
        {event.submitter && (
          <p className="event-submitter">
            <span>{event.submitter.nickname}</span>
            <TrustGradeBadge member={event.submitter} />
          </p>
        )}
        <p>
          <MapPin size={14} />
          {regions[event.region] || event.regionDetail || "지역 미정"}
        </p>
        <p>
          <CalendarDays size={14} />
          {period(event.beginDe, event.endDe)}
        </p>
        <div className="card-bottom">
          {event.source === "MEMBER" && event.accuracyVote && (
            <span className="card-votes">
              <span
                className="vote-accurate"
                aria-label={`정확해요 ${event.accuracyVote.accurateCount}개`}
              >
                <CheckCircle2 size={13} />
                {event.accuracyVote.accurateCount}
              </span>
              <span
                className="vote-inaccurate"
                aria-label={`부정확해요 ${event.accuracyVote.inaccurateCount}개`}
              >
                <AlertTriangle size={13} />
                {event.accuracyVote.inaccurateCount}
              </span>
            </span>
          )}
          <span className="post-like-count">
            {event.likeCount !== undefined && (
              <span
                className="post-like-count"
                aria-label={`좋아요 ${event.likeCount}개`}
              >
                <Heart size={13} />
                {event.likeCount}
              </span>
            )}
            <ArrowRight size={17} />
          </span>
        </div>
      </div>
    </Link>
  );
}
export function Empty({
  title,
  description,
  action,
  icon,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
  icon?: ReactNode;
}) {
  return (
    <div className="empty-state">
      <span className="empty-icon">{icon || <Search size={30} />}</span>
      <h3>{title}</h3>
      {description && <p>{description}</p>}
      {action}
    </div>
  );
}
export function ErrorState({
  message,
  retry,
}: {
  message: string;
  retry?: () => void;
}) {
  return (
    <div className="error-state" role="alert">
      <CircleAlert size={24} />
      <div>
        <strong>정보를 불러오지 못했어요</strong>
        <p>{message}</p>
      </div>
      {retry && (
        <button className="btn secondary small" onClick={retry}>
          다시 시도
        </button>
      )}
    </div>
  );
}
export function Loading({ cards = false }: { cards?: boolean }) {
  return cards ? (
    <div className="event-grid" aria-label="불러오는 중" aria-busy="true">
      {[1, 2, 3].map((i) => (
        <div key={i} className="skeleton-card">
          <div />
          <span />
          <span />
          <span />
        </div>
      ))}
    </div>
  ) : (
    <div className="loading" role="status">
      <LoaderCircle className="spin" size={24} />
      잠시만 기다려 주세요
    </div>
  );
}
export function LoginRequired() {
  const location = useLocation();
  return (
    <Empty
      title="로그인하고 함께해요"
      description="나의 제보와 후기를 관리하고, 이웃과 이야기를 나눠보세요."
      action={
        <Link
          className="btn primary"
          to={`/login?next=${encodeURIComponent(location.pathname + location.search)}`}
        >
          로그인 <ArrowRight size={16} />
        </Link>
      }
    />
  );
}
export function Pagination({
  page,
  total,
  onChange,
}: {
  page: number;
  total: number;
  onChange: (page: number) => void;
}) {
  if (total <= 1) return null;
  const pages = Array.from({ length: total }, (_, i) => i).filter(
    (i) => i === 0 || i === total - 1 || Math.abs(i - page) <= 2,
  );
  return (
    <nav className="pagination" aria-label="페이지 이동">
      <button
        aria-label="이전 페이지"
        disabled={page === 0}
        onClick={() => onChange(page - 1)}
      >
        <ChevronLeft size={17} />
      </button>
      {pages.map((p, i) => (
        <span key={p}>
          {i > 0 && p - pages[i - 1] > 1 && <span className="ellipsis">…</span>}
          <button
            aria-current={page === p ? "page" : undefined}
            className={page === p ? "active" : ""}
            onClick={() => onChange(p)}
          >
            {p + 1}
          </button>
        </span>
      ))}
      <button
        aria-label="다음 페이지"
        disabled={page + 1 >= total}
        onClick={() => onChange(page + 1)}
      >
        <ChevronRight size={17} />
      </button>
    </nav>
  );
}
export function Modal({
  title,
  children,
  onClose,
}: {
  title: string;
  children: ReactNode;
  onClose: () => void;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const label = useId();
  useEffect(() => {
    ref.current?.showModal();
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previous;
    };
  }, []);
  return (
    <dialog
      ref={ref}
      className="modal"
      onCancel={onClose}
      aria-labelledby={label}
      onClick={(e) => {
        if (e.target === ref.current) onClose();
      }}
    >
      <div className="modal-inner">
        <div className="modal-heading">
          <h2 id={label}>{title}</h2>
          <button className="icon-btn" aria-label="닫기" onClick={onClose}>
            <X size={20} />
          </button>
        </div>
        {children}
      </div>
    </dialog>
  );
}
export function Field({
  label,
  required,
  hint,
  children,
  wide,
}: {
  label: string;
  required?: boolean;
  hint?: string;
  children: ReactNode;
  wide?: boolean;
}) {
  return (
    <label className={`field ${wide ? "wide" : ""}`}>
      <span>
        {label}
        {required && <b className="required">*</b>}
      </span>
      {children}
      {hint && <small>{hint}</small>}
    </label>
  );
}
export function FormError({ message }: { message: string }) {
  return message ? (
    <p className="form-error" role="alert">
      <CircleAlert size={16} />
      {message}
    </p>
  ) : null;
}
export function SubmitButton({
  busy,
  disabled = false,
  children,
}: {
  busy: boolean;
  disabled?: boolean;
  children: ReactNode;
}) {
  return (
    <button className="btn primary" type="submit" disabled={busy || disabled}>
      {busy ? (
        <>
          <LoaderCircle className="spin" size={17} />
          처리 중
        </>
      ) : (
        children
      )}
    </button>
  );
}
export function Upload({
  type,
  value,
  onChange,
  useKey = false,
}: {
  type: "POST" | "PROFILE" | "INQUIRY" | "FESTIVAL";
  value?: string | null;
  onChange: (value: string) => void;
  useKey?: boolean;
}) {
  const { api } = useApp();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [preview, setPreview] = useState(value);
  return (
    <div className="upload-area">
      {preview && (
        <Photo className="upload-preview" src={preview} alt="업로드한 이미지" />
      )}
      <label className="upload-button">
        <ImagePlus size={22} />
        <span>
          {busy
            ? "이미지 업로드 중…"
            : preview
              ? "이미지 바꾸기"
              : "사진을 올려주세요"}
        </span>
        <small>JPG · PNG · WEBP, 최대 5MB</small>
        <input
          type="file"
          accept="image/jpeg,image/png,image/webp"
          disabled={busy}
          onChange={async (e) => {
            const file = e.target.files?.[0];
            if (!file) return;
            setBusy(true);
            setError("");
            try {
              const result = await api.upload(file, type);
              setPreview(result.url);
              onChange(useKey ? result.key : result.url);
            } catch (err) {
              setError(errorText(err));
            } finally {
              setBusy(false);
              e.target.value = "";
            }
          }}
        />
      </label>
      {preview && (
        <button
          type="button"
          className="text-button"
          onClick={() => {
            setPreview("");
            onChange("");
          }}
        >
          이미지 제거
        </button>
      )}
      <FormError message={error} />
    </div>
  );
}
