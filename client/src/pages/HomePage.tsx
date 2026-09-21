import { Link } from 'react-router'

/**
 * 화면정의서 1번(홈).
 *
 * 행사 API가 아직 없어서 검색바와 추천 행사는 정적 화면이다.
 * 백엔드 연동은 행사 목록 API가 나온 뒤에 붙인다.
 */

/** 추천 행사 자리를 채우는 임시 데이터. 실제 이미지가 없어 그라데이션으로 대신한다. */
const RECOMMENDED = [
  {
    title: '파주 케이컬처페스티벌',
    provider: '공공',
    region: '경기도',
    date: '2026. 09. 12. (일)',
    tone: 'from-rose-200 to-orange-200',
  },
  {
    title: '화성 봄꽃 축제',
    provider: '공공',
    region: '경기도',
    date: '2026. 05. 10. (일)',
    tone: 'from-emerald-200 to-lime-200',
  },
  {
    title: '수원화성문화제',
    provider: '공공',
    region: '경기도',
    date: '2026. 10. 15. (목)',
    tone: 'from-amber-200 to-rose-200',
  },
]

const CATEGORIES = [
  {
    title: '공공행사',
    description: '공공기관에서 제공하는 다양한 행사',
    to: '/festivals/public',
    icon: '🏛️',
    tone: 'bg-brand-50',
  },
  {
    title: '민간행사',
    description: '개인이 등록한 다양한 행사',
    to: '/festivals/member',
    icon: '👥',
    tone: 'bg-amber-50',
  },
]

export default function HomePage() {
  return (
    <div className="mx-auto max-w-5xl px-4 py-6">
      {/* ── 히어로 ─────────────────────────────── */}
      <section className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-amber-800 via-orange-700 to-rose-800 p-8 text-white">
        <h1 className="text-3xl font-bold">오늘, 어디로 떠나시나요?</h1>
        <p className="mt-3 max-w-md text-sm leading-relaxed text-white/85">
          날씨와 지역에 맞는 다양한 행사 정보를 탐색하고,
          <br />
          직접 신청하고, 후기에 별점으로 신뢰를 쌓아보세요!
        </p>

        {/* 검색바 — 아직 동작하지 않는 자리표시자 */}
        <div className="mt-6 flex flex-wrap items-center gap-2 rounded-xl bg-white p-2 text-sm text-slate-700 shadow-lg">
          <SearchField label="2026. 05. 10. (일)" />
          <span className="h-6 w-px bg-sand-200" />
          <SearchField label="경기도" />
          <span className="h-6 w-px bg-sand-200" />
          <SearchField label="25° 맑음" />
          <button
            type="button"
            className="ml-auto grid size-9 place-items-center rounded-lg bg-brand-500 text-white hover:bg-brand-600"
            aria-label="행사 검색"
          >
            🔍
          </button>
        </div>
      </section>

      {/* ── 카테고리 ───────────────────────────── */}
      <section className="mt-5 grid gap-4 sm:grid-cols-2">
        {CATEGORIES.map((category) => (
          <Link
            key={category.title}
            to={category.to}
            className={`${category.tone} flex items-center gap-4 rounded-xl border border-sand-200 p-5 transition hover:border-brand-200`}
          >
            <span className="text-2xl" aria-hidden>
              {category.icon}
            </span>
            <span>
              <span className="block font-semibold text-slate-900">{category.title}</span>
              <span className="mt-0.5 block text-sm text-slate-500">{category.description}</span>
            </span>
          </Link>
        ))}
      </section>

      {/* ── 추천 행사 ──────────────────────────── */}
      <section className="mt-8">
        <div className="flex items-baseline justify-between">
          <h2 className="text-xl font-bold text-slate-900">추천 행사</h2>
          <Link to="/festivals/public" className="text-sm text-slate-500 hover:text-brand-600">
            더보기 &rsaquo;
          </Link>
        </div>

        <ul className="mt-4 grid gap-4 sm:grid-cols-3">
          {RECOMMENDED.map((festival) => (
            <li
              key={festival.title}
              className="overflow-hidden rounded-xl border border-sand-200 bg-white"
            >
              <div className={`h-28 bg-gradient-to-br ${festival.tone}`} aria-hidden />
              <div className="p-3">
                <p className="truncate font-semibold text-slate-900">{festival.title}</p>
                <p className="mt-2 flex gap-1.5 text-xs">
                  <Tag>{festival.provider}</Tag>
                  <Tag>{festival.region}</Tag>
                </p>
                <p className="mt-2 text-xs text-slate-500">{festival.date}</p>
              </div>
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}

function SearchField({ label }: { label: string }) {
  return (
    <button
      type="button"
      className="rounded-lg px-3 py-2 text-left hover:bg-sand-100"
      // 날짜·지역·날씨 선택은 행사 목록 API 연동 시 구현한다
      disabled
    >
      {label}
    </button>
  )
}

function Tag({ children }: { children: string }) {
  return <span className="rounded bg-sand-100 px-1.5 py-0.5 text-slate-600">{children}</span>
}
