/* Lucian UX/UI Prompt #213 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#213 */
import { useEffect, useId, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { imageUrl, period, regions } from "../lib/format";
import type { EventView } from "../lib/types";
import { FESTIVAL_DEFAULT_IMAGE, Photo } from "./ui";

const DUR = 4200;
const MAX_SLIDES = 5;
/** 사진을 이 배율보다 크게 늘려야 하면 흐려 보여서 첫 화면에서 뺀다 */
const MAX_UPSCALE = 1.3;
const LOAD_TIMEOUT = 5000;
const FALLBACK = "/images/garden.jpg";

/** 사진을 받아 원본 크기를 알아낸다. 실패하거나 너무 오래 걸리면 null. */
function measure(url: string) {
  return new Promise<{ w: number; h: number } | null>((resolve) => {
    const img = new Image();
    const timer = window.setTimeout(() => done(null), LOAD_TIMEOUT);
    function done(size: { w: number; h: number } | null) {
      window.clearTimeout(timer);
      img.onload = img.onerror = null;
      resolve(size);
    }
    img.onload = () => done({ w: img.naturalWidth, h: img.naturalHeight });
    img.onerror = () => done(null);
    img.src = url;
  });
}

/**
 * 첫 화면을 꽉 채워도 선명한 사진을 가진 행사만 순서대로 고른다.
 * 사진을 꽉 채우려면 가로·세로 중 더 많이 늘려야 하는 쪽 배율을 따른다.
 */
function useSharpEvents(events: EventView[], box: HTMLElement | null) {
  const [sharp, setSharp] = useState<EventView[] | null>(null);
  const key = events.map((e) => `${e.festivalId}:${e.imgUrl}`).join("|");
  useEffect(() => {
    if (!box) return;
    let alive = true;
    const { width, height } = box.getBoundingClientRect();
    Promise.all(
      events.map(async (event) => {
        const url = imageUrl(event.imgUrl);
        if (!url) return false;
        const size = await measure(url);
        if (!size || !size.w || !size.h) return false;
        return Math.max(width / size.w, height / size.h) <= MAX_UPSCALE;
      }),
    ).then((ok) => {
      if (alive) setSharp(events.filter((_, i) => ok[i]).slice(0, MAX_SLIDES));
    });
    return () => {
      alive = false;
    };
    // events는 매번 새 배열이라 내용이 바뀔 때만 다시 고른다
  }, [key, box]);
  return sharp;
}

/**
 * 첫 화면 전체를 채우는 행사 사진 슬라이드. 사진이 작아 흐려지는 행사는 뺀다.
 * 몇 초마다 다음 행사로 넘어가고, 아래 막대가 남은 시간만큼 찬다.
 * 포인터를 올리거나 키보드 초점이 들어오면 멈추고, 화면 밖에서도 멈춘다.
 */
export function HeroShowcase({ events }: { events: EventView[] }) {
  const [root, setRoot] = useState<HTMLDivElement | null>(null);
  const slides = useSharpEvents(events, root) ?? [];
  const [cur, setCur] = useState(0);
  const rootRef = useRef<HTMLDivElement | null>(null);
  const barRefs = useRef<(HTMLElement | null)[]>([]);
  const hold = useRef(false);
  const uid = useId();
  const count = slides.length;
  const active = Math.min(cur, Math.max(0, count - 1));

  useEffect(() => {
    if (count < 2) return;
    const root = rootRef.current!;
    const rm = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    barRefs.current.forEach((bar, k) => {
      if (bar) bar.style.transform = `scaleX(${k === active && rm ? 1 : 0})`;
    });
    if (rm) return;
    let acc = 0;
    let last = performance.now();
    let vis = true;
    let raf = 0;
    function tick(now: number) {
      raf = 0;
      if (!hold.current && vis) acc += Math.min(64, now - last);
      last = now;
      const p = Math.min(1, acc / DUR);
      const bar = barRefs.current[active];
      if (bar) bar.style.transform = `scaleX(${p.toFixed(3)})`;
      if (p >= 1) {
        setCur((active + 1) % count);
        return;
      }
      if (vis) raf = requestAnimationFrame(tick);
    }
    function run() {
      if (!raf) {
        last = performance.now();
        raf = requestAnimationFrame(tick);
      }
    }
    const observer = new IntersectionObserver((entries) => {
      vis = entries[0].isIntersecting;
      if (vis) run();
    });
    observer.observe(root);
    run();
    return () => {
      observer.disconnect();
      if (raf) cancelAnimationFrame(raf);
    };
  }, [active, count]);

  return (
    <div
      className="ux u213 hero-showcase"
      ref={(el) => {
        rootRef.current = el;
        setRoot(el);
      }}
      onPointerEnter={() => (hold.current = true)}
      onPointerLeave={() => (hold.current = false)}
      onFocus={() => (hold.current = true)}
      onBlur={() => (hold.current = false)}
    >
      {/* 고를 행사가 없거나 사진을 확인하는 동안 보이는 기본 풍경 */}
      <img
        className={`hero-showcase-fallback ${count ? "is-off" : ""}`}
        src={FALLBACK}
        alt={count ? "" : "가을 꽃이 가득한 정원 풍경"}
      />
      {slides.map((event, i) => {
        const path = event.submissionId
          ? `/submissions/${event.submissionId}`
          : `/events/${event.festivalId}`;
        return (
          <div
            key={event.festivalId ?? event.submissionId ?? i}
            id={`${uid}p${i}`}
            role="tabpanel"
            aria-labelledby={`${uid}t${i}`}
            className={`u213-pn ${i === active ? "is-on" : ""}`}
            inert={i !== active}
          >
            <Link to={path} className="hero-showcase-link">
              <Photo
                src={event.imgUrl}
                alt={event.title}
                fallbackSrc={FESTIVAL_DEFAULT_IMAGE}
              />
              <span className="hero-showcase-copy">
                <span className="hero-showcase-category">{event.category}</span>
                <strong>{event.title}</strong>
                <span>
                  {regions[event.region] || event.regionDetail || "지역 미정"}
                  {"  |  "}
                  {period(event.beginDe, event.endDe)}
                </span>
              </span>
            </Link>
          </div>
        );
      })}
      {count > 1 && (
        <div className="u213-tl" role="tablist" aria-label="추천 행사">
          {slides.map((event, i) => (
            <button
              key={event.festivalId ?? event.submissionId ?? i}
              id={`${uid}t${i}`}
              type="button"
              role="tab"
              className={`u213-tb ${i === active ? "is-on" : ""}`}
              aria-selected={i === active}
              aria-controls={`${uid}p${i}`}
              aria-label={event.title}
              tabIndex={i === active ? 0 : -1}
              onClick={() => setCur(i)}
              onKeyDown={(e) => {
                const d =
                  e.key === "ArrowRight" ? 1 : e.key === "ArrowLeft" ? -1 : 0;
                if (!d) return;
                e.preventDefault();
                const next = (i + d + count) % count;
                setCur(next);
                e.currentTarget.parentElement
                  ?.querySelectorAll<HTMLButtonElement>("button")
                  [next]?.focus();
              }}
            >
              <i>
                <b ref={(el) => void (barRefs.current[i] = el)} />
              </i>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
