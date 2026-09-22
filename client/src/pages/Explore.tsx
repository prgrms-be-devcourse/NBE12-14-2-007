import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  ArrowRight,
  CalendarDays,
  Grid2X2,
  Landmark,
  List,
  RotateCcw,
  Search,
} from "lucide-react";
import { useApp, useLoad } from "../lib/context";
import { categories } from "../lib/format";
import { RegionSelects } from "../components/RegionSelects";
import {
  Empty,
  ErrorState,
  EventCard,
  Loading,
  PageTitle,
  Pagination,
} from "../components/ui";

export function Explore() {
  const { api, mode } = useApp();
  const [params, setParams] = useSearchParams();
  const [layout, setLayout] = useState("grid");

  const category = params.get("category") || "전체";
  const region = params.get("region") || "";
  const date = params.get("date") || "";
  const query = params.get("q") || "";
  const sort = params.get("sort") || "soon";
  const providerType = params.get("providerType") || "";
  const excludeClosed = params.get("excludeClosed") === "true";
  const page = Math.max(0, Number(params.get("page")) || 0);
  const [text, setText] = useState(query);

  useEffect(() => setText(query), [query]);

  function setFilter(key: string, value: string) {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value);
    else next.delete(key);
    if (key !== "page") next.delete("page");
    setParams(next);
  }

  const apiRegion =
    mode === "api" && region.includes(":") ? region.split(":")[0] : region;
  const { data, loading, error, reload } = useLoad(
    () =>
      api.festivals({
        keyword: query || undefined,
        region: apiRegion || undefined,
        providerType:
          providerType === "PUBLIC" || providerType === "MEMBER"
            ? providerType
            : undefined,
        category: category === "전체" ? undefined : category,
        date: date || undefined,
        excludeClosed,
        page,
        sort: sort === "name" ? "name" : "soon",
      }),
    [
      api,
      query,
      apiRegion,
      providerType,
      category,
      date,
      excludeClosed,
      page,
      sort,
    ],
  );

  const events = data?.content || [];
  const currentPage = data?.number ?? page;

  return (
    <div className="container page-space">
      <PageTitle
        eyebrow="ESCAPE THE ORDINARY"
        title="오늘, 어디로 탈출할까요?"
        description="축제부터 공연, 전시까지! 방구석을 나설 이유를 찾아보세요."
      />
      <div className="source-note">
        <Landmark size={18} />
        <span>
          공공데이터와 이웃의 제보를 모아, 놓치기 아까운 행사를 한곳에!
        </span>
        {mode === "preview" && (
          <span className="example-label">현재 화면은 예시입니다</span>
        )}
      </div>

      <form
        className="filter-panel"
        onSubmit={(event) => {
          event.preventDefault();
          setFilter("q", text.trim());
        }}
      >
        <div className="filter-search">
          <Search size={19} />
          <input
            aria-label="행사 검색"
            placeholder="찾고 싶은 행사 이름을 입력해 보세요"
            value={text}
            onChange={(event) => setText(event.target.value)}
          />
        </div>
        <RegionSelects
          value={apiRegion}
          onChange={(value) => setFilter("region", value)}
          supportedOnly={mode === "api"}
        />
        <label className="filter-select">
          <CalendarDays size={17} />
          <input
            type="date"
            aria-label="날짜 필터"
            value={date}
            onChange={(event) => setFilter("date", event.target.value)}
          />
        </label>
        <button className="btn primary">
          <Search size={17} />
          검색
        </button>
      </form>

      <div className="filter-bottom">
        <div className="category-tabs" role="group" aria-label="행사 종류">
          {categories.map((item) => (
            <button
              key={item}
              className={category === item ? "active" : ""}
              onClick={() => setFilter("category", item)}
            >
              {item}
            </button>
          ))}
        </div>
        <button
          className="text-button muted"
          onClick={() => {
            setParams({});
            setText("");
          }}
        >
          <RotateCcw size={14} />
          필터 초기화
        </button>
      </div>

      <div className="results-toolbar">
        <p>
          총 <strong>{data?.totalElements || 0}</strong>개의 행사
          {mode === "preview" && <span className="muted">· 예시 데이터</span>}
        </p>
        <div className="result-options">
          <select
            aria-label="데이터 출처"
            value={providerType}
            onChange={(event) => setFilter("providerType", event.target.value)}
          >
            <option value="">모든 출처</option>
            <option value="PUBLIC">공공데이터</option>
            <option value="MEMBER">회원 제보</option>
          </select>
          <label className="checkbox-label compact-checkbox">
            <span>종료 행사 제외</span>
            <input
              type="checkbox"
              checked={excludeClosed}
              onChange={(event) =>
                setFilter("excludeClosed", event.target.checked ? "true" : "")
              }
            />
          </label>
          <select
            aria-label="행사 정렬"
            value={sort}
            onChange={(event) => setFilter("sort", event.target.value)}
          >
            <option value="soon">시작일순</option>
            <option value="name">이름순</option>
          </select>
          <div className="layout-toggle">
            <button
              onClick={() => setLayout("grid")}
              className={layout === "grid" ? "active" : ""}
              aria-label="카드로 보기"
              aria-pressed={layout === "grid"}
            >
              <Grid2X2 size={17} />
            </button>
            <button
              onClick={() => setLayout("list")}
              className={layout === "list" ? "active" : ""}
              aria-label="목록으로 보기"
              aria-pressed={layout === "list"}
            >
              <List size={18} />
            </button>
          </div>
        </div>
      </div>

      {loading ? (
        <Loading cards />
      ) : error ? (
        <ErrorState message={error} retry={reload} />
      ) : events.length ? (
        <div className={layout === "grid" ? "event-grid" : "event-list"}>
          {events.map((event) => (
            <EventCard
              key={event.festivalId}
              event={event}
              list={layout === "list"}
            />
          ))}
        </div>
      ) : (
        <Empty
          title="조건에 맞는 행사가 없어요"
          description="날짜나 지역을 바꾸면 새로운 행사를 만날 수 있어요."
          action={
            <button
              className="btn secondary"
              onClick={() => {
                setParams({});
                setText("");
              }}
            >
              전체 행사 보기
            </button>
          }
        />
      )}

      {!loading && !error && data && (
        <Pagination
          page={currentPage}
          total={data.totalPages}
          onChange={(nextPage) => setFilter("page", String(nextPage))}
        />
      )}

      <div className="soft-callout">
        <span>
          찾던 행사가 보이지 않나요? 알고 있는 행사를 직접 알려주세요.
        </span>
        <Link to="/submissions/new">
          행사 제보하기
          <ArrowRight size={16} />
        </Link>
      </div>
    </div>
  );
}
