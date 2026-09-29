import { useEffect, useState, type FormEvent } from "react";
import { useSearchParams } from "react-router-dom";
import { Search } from "lucide-react";
import type { ReviewSort } from "../lib/demo";
import type { PostSearchType } from "../lib/types";

const searchTypes: { value: PostSearchType; label: string }[] = [
  { value: "TITLE", label: "후기 제목" },
  { value: "MEMBER_NICKNAME", label: "작성자 닉네임" },
  { value: "FESTIVAL_TITLE", label: "행사명" },
];

export function useReviewQuery(preview = false) {
  const [params, setParams] = useSearchParams();
  const type =
    searchTypes.find((item) => item.value === params.get("type"))?.value ??
    "TITLE";
  const keyword = params.get("keyword")?.trim() ?? "";
  const requestedPage = Number(params.get("page") ?? 0);
  const page =
    Number.isInteger(requestedPage) &&
    requestedPage >= 0 &&
    requestedPage < 2147483647
      ? requestedPage
      : 0;
  const requestedSort = params.get("sort");
  const sort: ReviewSort =
    requestedSort === "likeCount,desc" ||
    requestedSort === "createdAt,asc" ||
    requestedSort === "createdAt,desc"
      ? requestedSort
      : preview
        ? "likeCount,desc"
        : "createdAt,desc";
  function update(values: Record<string, string | number>) {
    setParams((current) => {
      const next = new URLSearchParams(current);
      next.delete("page");
      for (const [key, value] of Object.entries(values)) {
        if (value === "" || (key === "page" && value === 0)) next.delete(key);
        else next.set(key, String(value));
      }
      return next;
    });
  }
  return { type, keyword, page, sort, update };
}

export function ReviewSearch({
  type,
  keyword,
  onSearch,
}: {
  type: PostSearchType;
  keyword: string;
  onSearch: (type: PostSearchType, keyword: string) => void;
}) {
  const [draftType, setDraftType] = useState(type);
  const [draftKeyword, setDraftKeyword] = useState(keyword);
  useEffect(() => {
    setDraftType(type);
    setDraftKeyword(keyword);
  }, [type, keyword]);
  function submit(event: FormEvent) {
    event.preventDefault();
    onSearch(draftType, draftKeyword.trim());
  }
  return (
    <form
      className="review-search"
      role="search"
      aria-label="후기 검색"
      onSubmit={submit}
    >
      <select
        aria-label="후기 검색 기준"
        value={draftType}
        onChange={(event) => setDraftType(event.target.value as PostSearchType)}
      >
        {searchTypes.map((item) => (
          <option key={item.value} value={item.value}>
            {item.label}
          </option>
        ))}
      </select>
      <input
        type="search"
        aria-label="후기 검색어"
        placeholder="찾고 싶은 후기를 검색하세요"
        value={draftKeyword}
        onChange={(event) => setDraftKeyword(event.target.value)}
      />
      <button className="btn primary small" type="submit">
        <Search size={16} />
        검색
      </button>
      <button
        className="btn secondary small"
        type="button"
        onClick={() => {
          setDraftType("TITLE");
          setDraftKeyword("");
          onSearch("TITLE", "");
        }}
      >
        초기화
      </button>
    </form>
  );
}
