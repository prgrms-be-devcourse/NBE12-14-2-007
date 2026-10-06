import { useEffect, useState } from "react";
import { Search, X } from "lucide-react";
import { useApp } from "../lib/context";
import { errorText, eventState, period } from "../lib/format";
import type { EventView } from "../lib/types";
import { Badge, FESTIVAL_DEFAULT_IMAGE, Photo } from "./ui";

/** 입력이 멈춘 뒤에 검색한다. 글자마다 요청하면 서버에 불필요한 부하가 간다. */
const SEARCH_DELAY_MS = 300;

/** 목록 응답에는 상세 주소가 없어서 기관명으로 대신한다. */
function placeText(festival: EventView) {
  const place = festival.regionDetail || festival.instNm;
  return place ? ` · ${place}` : "";
}

/**
 * 후기를 남길 행사를 고르는 검색 칸.
 * 행사 상세를 거치지 않고 작성 화면에서 바로 행사를 찾을 수 있게 한다.
 * 이미 고른 행사가 있으면 요약 카드를 보여주고, "변경"을 누르면 다시 검색한다.
 */
export function FestivalPicker({
  value,
  onChange,
}: {
  value: EventView | null;
  onChange: (festival: EventView | null) => void;
}) {
  const { api } = useApp();
  const [keyword, setKeyword] = useState("");
  const [results, setResults] = useState<EventView[]>([]);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const trimmed = keyword.trim();
    if (value || !trimmed) {
      setResults([]);
      setSearching(false);
      setError("");
      return;
    }
    let current = true;
    setSearching(true);
    const timer = window.setTimeout(() => {
      api
        .festivals({ keyword: trimmed })
        .then((page) => {
          if (!current) return;
          setResults(page.content);
          setError("");
        })
        .catch((e) => {
          if (current) setError(errorText(e));
        })
        .finally(() => {
          if (current) setSearching(false);
        });
    }, SEARCH_DELAY_MS);
    return () => {
      current = false;
      window.clearTimeout(timer);
    };
  }, [api, keyword, value]);

  if (value)
    return (
      <div className="festival-picker-selected">
        <Photo
          src={value.imgUrl}
          alt=""
          className="festival-picker-thumb"
          fallbackSrc={FESTIVAL_DEFAULT_IMAGE}
        />
        <div className="festival-picker-info">
          <strong>{value.title}</strong>
          <small>
            {period(value.beginDe, value.endDe)}
            {placeText(value)}
          </small>
        </div>
        <button
          type="button"
          className="btn secondary small"
          onClick={() => {
            setKeyword("");
            onChange(null);
          }}
        >
          변경
        </button>
      </div>
    );

  const trimmed = keyword.trim();
  return (
    <div className="festival-picker">
      <div className="festival-picker-input">
        <Search size={16} aria-hidden />
        <input
          // type="search"는 브라우저 기본 지우기 버튼이 따로 생겨 직접 만든 버튼과 겹친다.
          type="text"
          enterKeyHint="search"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="다녀온 행사 이름이나 기관명을 입력해 주세요"
          aria-label="후기를 남길 행사 검색"
          autoComplete="off"
        />
        {keyword && (
          <button
            type="button"
            className="festival-picker-clear"
            aria-label="검색어 지우기"
            onClick={() => setKeyword("")}
          >
            <X size={14} />
          </button>
        )}
      </div>
      {trimmed &&
        (searching ? (
          <p className="festival-picker-message">행사를 찾고 있어요…</p>
        ) : error ? (
          <p className="festival-picker-message" role="alert">
            {error}
          </p>
        ) : results.length === 0 ? (
          <p className="festival-picker-message">
            '{trimmed}'에 맞는 행사가 없어요. 다른 이름으로 검색해 보세요.
          </p>
        ) : (
          <ul className="festival-picker-results" aria-label="행사 검색 결과">
            {results.map((festival) => (
              <li key={festival.festivalId}>
                <button type="button" onClick={() => onChange(festival)}>
                  <Photo
                    src={festival.imgUrl}
                    alt=""
                    className="festival-picker-thumb"
                    fallbackSrc={FESTIVAL_DEFAULT_IMAGE}
                  />
                  <span className="festival-picker-info">
                    <strong>{festival.title}</strong>
                    <small>
                      {period(festival.beginDe, festival.endDe)}
                      {placeText(festival)}
                    </small>
                  </span>
                  {festival.beginDe && festival.endDe && (
                    <StateBadge festival={festival} />
                  )}
                </button>
              </li>
            ))}
          </ul>
        ))}
    </div>
  );
}

/** 개최 예정인 행사는 아직 다녀올 수 없어서 흐린 색으로 구분한다. */
function StateBadge({ festival }: { festival: EventView }) {
  const state = eventState(festival.beginDe, festival.endDe);
  return <Badge tone={state === "개최 예정" ? "gray" : "green"}>{state}</Badge>;
}
