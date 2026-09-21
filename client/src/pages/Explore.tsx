import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  ArrowRight,
  CalendarDays,
  Grid2X2,
  Landmark,
  List,
  RotateCcw,
  Search,
  SlidersHorizontal,
} from "lucide-react";
import { useApp } from "../lib/context";
import { demoEvents } from "../lib/demo";
import { categories, regions } from "../lib/format";
import { matchesRegion } from "../lib/regions";
import { RegionSelects } from "../components/RegionSelects";
import { Empty, EventCard, PageTitle, Pagination } from "../components/ui";

export function Explore() {
  const { mode } = useApp();
  const [params, setParams] = useSearchParams();
  const [layout, setLayout] = useState("grid");
  const [text, setText] = useState(params.get("q") || "");
  const category = params.get("category") || "전체";
  const region = params.get("region") || "";
  const date = params.get("date") || "";
  const query = params.get("q") || "";
  const sort = params.get("sort") || "soon";
  const page = Math.max(0, Number(params.get("page")) || 0);
  function setFilter(key: string, value: string) {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value);
    else next.delete(key);
    if (key !== "page") next.delete("page");
    setParams(next);
  }
  const filtered = demoEvents
    .filter(
      (e) =>
        e.source === "PUBLIC" &&
        (category === "전체" || e.category === category) &&
        (!region || matchesRegion(e.region, region)) &&
        (!date ||
          (e.beginDe.slice(0, 10) <= date && e.endDe.slice(0, 10) >= date)) &&
        (!query ||
          `${e.title} ${e.regionDetail} ${e.category} ${regions[e.region]}`
            .toLowerCase()
            .includes(query.toLowerCase())),
    )
    .sort((a, b) =>
      sort === "name"
        ? a.title.localeCompare(b.title, "ko")
        : a.beginDe.localeCompare(b.beginDe),
    );
  const totalPages = Math.ceil(filtered.length / 6);
  const safePage = Math.min(page, Math.max(0, totalPages - 1));
  return (
    <div className="container page-space">
      <PageTitle
        eyebrow="CULTURE NEAR YOU"
        title="지역 문화행사"
        description="지역의 다채로운 축제와 공연, 전시를 한곳에서 발견해요."
      />
      <div className="source-note">
        <Landmark size={18} />
        <span>경기도 문화행사 정보를 바탕으로 새로운 즐거움을 소개해요.</span>
        {mode === "preview" && (
          <span className="example-label">현재 화면은 예시입니다</span>
        )}
      </div>
      {mode === "api" ? (
        <Empty
          title="지역 문화행사를 준비하고 있어요"
          description="행사 둘러보기는 아직 열리지 않았어요. 알고 있는 행사를 먼저 제보해 보세요."
          action={
            <Link className="btn primary" to="/submissions/new">
              행사 제보하기
              <ArrowRight size={16} />
            </Link>
          }
        />
      ) : (
        <>
          <form
            className="filter-panel"
            onSubmit={(e) => {
              e.preventDefault();
              setFilter("q", text.trim());
            }}
          >
            <div className="filter-search">
              <Search size={19} />
              <input
                aria-label="행사 검색"
                placeholder="찾고 싶은 행사 이름을 입력해 보세요"
                value={text}
                onChange={(e) => setText(e.target.value)}
              />
            </div>
            <RegionSelects
              value={region}
              onChange={(value) => setFilter("region", value)}
            />
            <label className="filter-select">
              <CalendarDays size={17} />
              <input
                type="date"
                aria-label="날짜 필터"
                value={date}
                onChange={(e) => setFilter("date", e.target.value)}
              />
            </label>
            <button className="btn primary">
              <Search size={17} />
              검색
            </button>
          </form>
          <div className="filter-bottom">
            <div className="category-tabs" role="group" aria-label="행사 종류">
              {categories.map((c) => (
                <button
                  key={c}
                  className={category === c ? "active" : ""}
                  onClick={() => setFilter("category", c)}
                >
                  {c}
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
              총 <strong>{filtered.length}</strong>개의 행사{" "}
              <span className="muted">· 예시 데이터</span>
            </p>
            <div>
              <SlidersHorizontal size={15} />
              <select
                aria-label="행사 정렬"
                value={sort}
                onChange={(e) => setFilter("sort", e.target.value)}
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
          {filtered.length ? (
            <div className={layout === "grid" ? "event-grid" : "event-list"}>
              {filtered.slice(safePage * 6, safePage * 6 + 6).map((event) => (
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
          <Pagination
            page={safePage}
            total={totalPages}
            onChange={(p) => setFilter("page", String(p))}
          />
          <div className="soft-callout">
            <span>
              찾던 행사가 보이지 않나요? 알고 있는 행사를 직접 알려주세요.
            </span>
            <Link to="/submissions/new">
              행사 제보하기
              <ArrowRight size={16} />
            </Link>
          </div>
        </>
      )}
    </div>
  );
}
