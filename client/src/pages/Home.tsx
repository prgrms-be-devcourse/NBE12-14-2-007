import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ArrowRight,
  CalendarDays,
  ChevronDown,
  ChevronRight,
  MapPin,
  MessageCircle,
  Search,
} from "lucide-react";
import { useApp, useLoad } from "../lib/context";
import { demoEvents, previewPosts } from "../lib/demo";
import { categories, dateText } from "../lib/format";
import { regionGroups } from "../lib/regions";
import { richTextToPlainText } from "../components/RichText";
import { PillTabs } from "../components/PillTabs";
import { HeroShowcase } from "../components/HeroShowcase";
import {
  Badge,
  Empty,
  EventCard,
  Loading,
  Photo,
  SectionTitle,
  WeatherBadge,
} from "../components/ui";

export function Home() {
  const { mode, api } = useApp();
  const navigate = useNavigate();
  const [category, setCategory] = useState("전체");
  const [region, setRegion] = useState("");
  const [date, setDate] = useState("");
  const [query, setQuery] = useState("");

  const events = demoEvents.filter(
    (e) =>
      e.source === "PUBLIC" &&
      (category === "전체" || e.category === category) &&
      e.status !== "CLOSED",
  );

  // Explore.tsx와 동일하게 excludeClosed: true로 진행 중인 행사 목록을 가져옵니다.
  const {
    data: liveEvents,
    loading: liveLoading,
    error: liveError,
  } = useLoad(
    () =>
      api.festivals({
        providerType: "PUBLIC",
        category: category === "전체" ? undefined : category,
        excludeClosed: true,
      }),
    [api, category],
  );

  const showcase = useLoad(
    () =>
      mode === "preview"
        ? Promise.resolve(null)
        : // 사진이 작은 행사는 첫 화면에서 빠지므로 후보를 두 쪽(18개) 받아 둔다
          Promise.all(
            [0, 1].map((page) =>
              api.festivals({
                providerType: "PUBLIC",
                excludeClosed: true,
                page,
              }),
            ),
          ).then((pages) => pages.flatMap((p) => p.content)),
    [api, mode],
  );
  const showcaseEvents =
    mode === "preview"
      ? demoEvents.filter((e) => e.status !== "CLOSED")
      : (showcase.data ?? []);

  const weather = useLoad(
    () => (region && date ? api.weather(region, date) : Promise.resolve(null)),
    [api, region, date],
  );

  return (
    <>
      <section className="home-intro container">
        <div className="hero-split">
          <div className="hero-main">
            <h1>
              이번 주말,
              <br />
              어디로 떠나볼까요?
            </h1>
            <p>지역 축제, 공연, 전시 일정을 한곳에서 찾아보세요.</p>
            <form
              className="hero-search"
              onSubmit={(e) => {
                e.preventDefault();
                const params = new URLSearchParams();
                if (region) params.set("region", region);
                if (date) params.set("date", date);
                if (query.trim()) params.set("q", query.trim());
                navigate(`/explore?${params}`);
              }}
            >
              <div className="hero-search-bar">
                <Search size={20} />
                <input
                  aria-label="행사 검색어"
                  placeholder="어떤 행사를 찾고 있나요?"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                />
                <button className="btn primary">검색</button>
              </div>
              <div className="hero-search-filters">
                <label className="hero-chip">
                  <MapPin size={16} />
                  <select
                    aria-label="지역 선택"
                    value={region}
                    onChange={(e) => setRegion(e.target.value)}
                  >
                    <option value="">전국</option>
                    {regionGroups.map((group) => (
                      <option key={group.value} value={group.value}>
                        {group.label}
                      </option>
                    ))}
                  </select>
                  <ChevronDown size={14} />
                </label>
                <label className="hero-chip">
                  <CalendarDays size={16} />
                  <input
                    aria-label="행사 날짜"
                    type="date"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    onClick={(e) => e.currentTarget.showPicker?.()}
                  />
                </label>
                {weather.data && weather.data.condition !== "UNKNOWN" && (
                  <WeatherBadge
                    condition={weather.data.condition}
                    precipitationProbability={
                      weather.data.precipitationProbability
                    }
                  />
                )}
                {weather.data && weather.data.condition === "UNKNOWN" && (
                  <span className="weather-hint">
                    {date < new Date().toISOString().slice(0, 10)
                      ? "지난 날짜는 날씨 제공이 불가합니다"
                      : "오늘 기준 +4일치만 날씨 제공이 가능합니다"}
                  </span>
                )}
              </div>
            </form>
          </div>
          <HeroShowcase events={showcaseEvents} />
        </div>
      </section>
      <div className="container">
        <nav className="region-jump" aria-label="지역별 행사">
          <h2>지역별로 보기</h2>
          <ul>
            {REGION_SHORTCUTS.map(([value, label]) => (
              <li key={value}>
                <Link to={`/explore?region=${value}`}>{label}</Link>
              </li>
            ))}
          </ul>
        </nav>

        <section className="home-events">
          <SectionTitle title="지금 열리는 행사" />

          <div className="home-events-toolbar">
            <PillTabs
              label="행사 종류"
              items={categories.map((c) => ({ value: c, label: c }))}
              value={category}
              onChange={setCategory}
            />
            <div className="home-events-more">
              {mode === "preview" && (
                <span className="quiet-note">미리보기 예시 행사</span>
              )}
              <Link to="/explore" className="more-link">
                행사 둘러보기
                <ChevronRight size={16} />
              </Link>
            </div>
          </div>

          {/* 카드 목록: 정확히 4개만 노출 */}
          {mode === "preview" ? (
            events.length ? (
              <div className="event-grid home-grid">
                {events.slice(0, 4).map((event) => (
                  <EventCard key={event.festivalId} event={event} />
                ))}
              </div>
            ) : (
              <Empty
                title="이 종류의 예시 행사는 아직 없어요"
                description="다른 종류의 행사를 둘러보세요."
              />
            )
          ) : liveLoading ? (
            <Loading cards />
          ) : liveEvents && liveEvents.content.length && !liveError ? (
            <div className="event-grid home-grid">
              {liveEvents.content.slice(0, 4).map((event) => (
                <EventCard key={event.festivalId} event={event} />
              ))}
            </div>
          ) : (
            <Empty
              title={
                liveError
                  ? "행사를 불러오지 못했어요"
                  : "현재 진행 중인 행사가 없어요"
              }
              description={
                liveError
                  ? liveError
                  : "다른 종류의 행사를 둘러보거나, 전체 목록에서 확인해 보세요."
              }
              action={
                <Link to="/explore" className="btn secondary">
                  행사 둘러보기
                </Link>
              }
            />
          )}
        </section>

        <section className="submission-banner">
          <div>
            <h2>알고 있는 행사가 있나요?</h2>
            <p>
              동네 축제, 공연, 전시 정보를 올리면 다른 회원들도 볼 수 있어요.
            </p>
          </div>
          <Link className="btn dark" to="/submissions/new">
            행사 제보하기
          </Link>
        </section>

        <section className="home-reviews">
          <SectionTitle
            title="최근 후기"
            action={
              <Link className="more-link" to="/reviews">
                후기 둘러보기
                <ChevronRight size={17} />
              </Link>
            }
          />
          {mode === "preview" ? (
            <div className="review-grid">
              {previewPosts()
                .slice(0, 3)
                .map((post) => (
                  <Link
                    key={post.id}
                    to={`/reviews/${post.id}`}
                    className="review-teaser"
                  >
                    <Photo src={post.thumbnail} alt={post.festivalTitle} />
                    <div>
                      <Badge tone="gray">{post.festivalTitle}</Badge>
                      <h3>{post.title}</h3>
                      <p>{richTextToPlainText(post.content)}</p>
                      <div className="review-byline">
                        <span className="avatar tiny">
                          {post.member.nickname[0]}
                        </span>
                        <span>{post.member.nickname}</span>
                        <time>{dateText(post.date)}</time>
                      </div>
                    </div>
                  </Link>
                ))}
            </div>
          ) : (
            <Link className="review-invitation" to="/reviews">
              <MessageCircle size={30} />
              <div>
                <h3>다녀온 행사의 이야기를 남겨주세요</h3>
                <p>행사를 선택하면 후기를 읽고, 나의 경험을 나눌 수 있어요.</p>
              </div>
              <ArrowRight />
            </Link>
          )}
        </section>
      </div>
    </>
  );
}

const REGION_SHORTCUTS: [string, string][] = [
  ["SEOUL", "서울"],
  ["GYEONGGI", "경기"],
  ["INCHEON", "인천"],
  ["GANGWON", "강원"],
  ["CHUNGBUK", "충북"],
  ["CHUNGNAM", "충남"],
  ["DAEJEON", "대전"],
  ["SEJONG", "세종"],
  ["JEONBUK", "전북"],
  ["JEONNAM", "전남"],
  ["GWANGJU", "광주"],
  ["GYEONGBUK", "경북"],
  ["GYEONGNAM", "경남"],
  ["DAEGU", "대구"],
  ["ULSAN", "울산"],
  ["BUSAN", "부산"],
  ["JEJU", "제주"],
];
