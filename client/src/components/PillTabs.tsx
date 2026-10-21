/* Lucian UX/UI Prompt #039 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#039 */
import { type ReactNode, useLayoutEffect, useRef } from "react";

export type PillTabItem = { value: string; label: string; icon?: ReactNode };

/**
 * 하나만 고르는 필터 버튼 줄. 고른 버튼 뒤로 알약이 미끄러져 옮겨 간다.
 * 아래 목록을 바꾸는 필터라 tab 대신 aria-pressed 버튼으로 둔다.
 */
export function PillTabs({
  label,
  items,
  value,
  onChange,
}: {
  label: string;
  items: PillTabItem[];
  value: string;
  onChange: (value: string) => void;
}) {
  const tabsRef = useRef<HTMLDivElement>(null);
  const pillRef = useRef<HTMLSpanElement>(null);
  const firstMove = useRef(true);
  const index = Math.max(
    0,
    items.findIndex((item) => item.value === value),
  );

  useLayoutEffect(() => {
    const tabs = tabsRef.current!;
    const pill = pillRef.current!;
    function movePill(animate: boolean) {
      const tab = tabs.querySelectorAll<HTMLButtonElement>(".u039-tab")[index];
      if (!tab) return;
      if (!animate) pill.style.transition = "none";
      pill.style.setProperty("--x", `${tab.offsetLeft}px`);
      pill.style.setProperty("--w", `${tab.offsetWidth}px`);
      if (!animate) {
        void pill.offsetWidth;
        pill.style.transition = "";
      }
      // 가로로 넘치는 줄에서는 고른 버튼이 보이도록 스크롤한다
      const left = tab.offsetLeft - 5;
      const right = tab.offsetLeft + tab.offsetWidth + 5;
      if (left < tabs.scrollLeft) tabs.scrollLeft = left;
      else if (right > tabs.scrollLeft + tabs.clientWidth)
        tabs.scrollLeft = right - tabs.clientWidth;
    }
    movePill(!firstMove.current);
    firstMove.current = false;
    const observer = new ResizeObserver(() => movePill(false));
    observer.observe(tabs);
    return () => observer.disconnect();
  }, [index, items.length]);

  return (
    <div className="ux u039">
      <div className="u039-tabs" role="group" aria-label={label} ref={tabsRef}>
        <span className="u039-pill" aria-hidden="true" ref={pillRef} />
        {items.map((item) => (
          <button
            key={item.value}
            className="u039-tab"
            type="button"
            aria-pressed={item.value === value}
            onClick={() => onChange(item.value)}
          >
            {item.icon}
            {item.label}
          </button>
        ))}
      </div>
    </div>
  );
}
