import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ArrowDown,
  ArrowRight,
  CalendarDays,
  ChevronRight,
  Compass,
  Landmark,
  MapPin,
  MessageCircle,
  Search,
  Sparkles,
  UsersRound,
} from "lucide-react";
import { useApp, useLoad } from "../lib/context";
import { demoEvents, previewPosts } from "../lib/demo";
import { categories, dateText } from "../lib/format";
import {
  Badge,
  Empty,
  EventCard,
  Loading,
  Photo,
  SectionTitle,
} from "../components/ui";
import { RegionSelects } from "../components/RegionSelects";

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

  return (
      <>
        <section className="home-intro container">
          <div className="hero">
            <img
                className="hero-image"
                src="/images/garden.jpg"
                alt="가을 꽃이 가득한 정원 풍경"
            />
            <div className="hero-shade" />
            <div className="hero-content">
            <span className="hero-kicker">
              <span />
              일상에 즐거움을 더하다
            </span>
              <h1>
                이번 주말,
                <br />
                어디로 <span>떠나볼까요?</span>
              </h1>
              <p>
                가까운 곳의 새로운 발견부터, 함께 나누는 특별한 순간까지.
                <br className="desktop-break" />
                나에게 딱 맞는 행사를 만나보세요.
              </p>
              <Link to="/explore" className="hero-link">
                새로운 즐거움 발견하기 <ArrowRight size={18} />
              </Link>
            </div>
            <div className="hero-caption">
              <MapPin size={14} />
              <span>가을을 만나는 가장 가까운 방법</span>
            </div>
            <div className="hero-index">
              <strong>AUTUMN</strong>
              <span />
              EDITION
            </div>
          </div>
          <form
              className="discovery-search"
              onSubmit={(e) => {
                e.preventDefault();
                const params = new URLSearchParams();
                if (region) params.set("region", region);
                if (date) params.set("date", date);
                if (query.trim()) params.set("q", query.trim());
                navigate(`/explore?${params}`);
              }}
          >
            <RegionSelects
                value={region}
                onChange={setRegion}
                variant="discovery"
            />
            <label>
              <CalendarDays size={20} />
              <div>
                <span>언제 떠날까요?</span>
                <input
                    aria-label="행사 날짜"
                    type="date"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                />
              </div>
            </label>
            <label className="keyword-search">
              <Search size={20} />
              <div>
                <span>어떤 즐거움을 찾나요?</span>
                <input
                    aria-label="행사 검색어"
                    placeholder="행사명, 키워드 검색"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                />
              </div>
            </label>
            <button className="btn primary search-submit">
              <Search size={19} />
              행사 찾기
            </button>
          </form>
        </section>
        <div className="container">
          <section className="discovery-routes">
            <Link to="/explore" className="route-card">
            <span className="route-icon peach">
              <Landmark size={28} />
            </span>
              <div>
                <span className="mini-label">가까운 곳의 새로운 발견</span>
                <h2>지역 문화행사</h2>
                <p>지역 곳곳의 축제, 공연, 전시를 만나보세요.</p>
              </div>
              <ArrowRight size={20} />
            </Link>
            <Link to="/submissions/new" className="route-card">
            <span className="route-icon sage">
              <UsersRound size={28} />
            </span>
              <div>
                <span className="mini-label">좋은 소식은 함께 나눠요</span>
                <h2>행사 제보</h2>
                <p>알고 있는 좋은 행사를 이웃에게 알려주세요.</p>
              </div>
              <ArrowRight size={20} />
            </Link>
          </section>

          <section className="home-events">
            <SectionTitle
                eyebrow="DISCOVER YOUR WEEKEND"
                title="가까이에서 찾는 특별한 하루"
                description="멀리 가지 않아도 괜찮아요. 새로운 즐거움은 가까이에 있으니까요."
            />

            <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  gap: "16px",
                  marginTop: "20px",
                  marginBottom: "24px",
                  flexWrap: "wrap",
                }}
            >
              <div className="category-tabs" role="group" aria-label="행사 종류" style={{ margin: 0 }}>
                {categories.map((c) => (
                    <button
                        key={c}
                        type="button"
                        className={category === c ? "active" : ""}
                        onClick={() => setCategory(c)}
                    >
                      {c === "전체" && <Sparkles size={14} />} {c}
                    </button>
                ))}
              </div>

              <div style={{ display: "flex", alignItems: "center", gap: "8px", flexShrink: 0 }}>
                {mode === "preview" && (
                    <span className="quiet-note">미리보기 예시 행사</span>
                )}
                <Link
                    to="/explore"
                    className="btn secondary"
                    style={{
                      display: "inline-flex",
                      alignItems: "center",
                      gap: "6px",
                      padding: "8px 18px",
                      borderRadius: "12px",
                      fontWeight: 600,
                      fontSize: "14px",
                      whiteSpace: "nowrap",
                      border: "1px solid #e2e8f0",
                      textDecoration: "none",
                    }}
                >
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
                        행사 둘러보기 <ArrowRight size={16} />
                      </Link>
                    }
                />
            )}
          </section>

          <section className="submission-banner">
            <div>
              <span className="eyebrow">GOOD THINGS, TOGETHER</span>
              <h2>혼자 알기 아까운 행사가 있나요?</h2>
              <p>
                작은 동네 축제도, 특별한 전시도 좋아요. 새로운 즐거움을
                알려주세요.
              </p>
            </div>
            <div className="banner-art" aria-hidden="true">
              <div className="ticket">
                <CalendarDays />
                <span>
                HELLO,
                <br />
                <strong>WEEKEND!</strong>
              </span>
                <i />
              </div>
              <Sparkles className="sparkle-one" />
              <span className="sparkle-two">✳</span>
            </div>
            <Link className="btn dark" to="/submissions/new">
              행사 제보하기
              <ArrowRight size={16} />
            </Link>
          </section>

          <section className="home-reviews">
            <SectionTitle
                eyebrow="MOMENTS WE SHARE"
                title="다녀온 사람들의 이야기"
                description="직접 경험한 순간들이 다음 나들이의 힌트가 되어줘요."
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
                          <p>{post.content.split("\n")[0]}</p>
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

          <div className="closing-line">
            <Compass size={20} />
            <span>가벼운 발걸음으로, 새로운 일상으로.</span>
            <ArrowDown size={16} />
          </div>
        </div>
      </>
  );
}
