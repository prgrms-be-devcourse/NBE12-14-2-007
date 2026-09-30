import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { ChevronRight, Search } from "lucide-react";
import { RichTextContent } from "../components/RichText";
import { useReviewQuery } from "../components/ReviewSearch";
import { Badge, Empty, ErrorState, Loading, Modal, Photo } from "../components/ui";
import { ApiError } from "../lib/api";
import { useApp, useLoad } from "../lib/context";
import { dateText, dateTimeText, errorText } from "../lib/format";
import type { AdminPostSummary, Comment, PostSearchType } from "../lib/types";

const searchTypes: { value: PostSearchType; label: string }[] = [
  { value: "TITLE", label: "후기 제목" },
  { value: "MEMBER_NICKNAME", label: "작성자 닉네임" },
  { value: "FESTIVAL_TITLE", label: "행사명" },
];

export function AdminReviews() {
  const { api } = useApp();
  const { page, sort, type, keyword, update } = useReviewQuery();
  const [params, setParams] = useSearchParams();
  const [query, setQuery] = useState(keyword);
  const selectedId = params.get("item");
  const { data, loading, error, reload } = useLoad(
    () =>
      api.adminPosts({
        page,
        size: 20,
        sort: sort === "createdAt,asc" ? sort : "createdAt,desc",
        type,
        keyword,
      }),
    [api, page, sort, type, keyword],
  );
  useEffect(() => {
    setQuery(keyword);
  }, [keyword]);
  useEffect(() => {
    const timer = window.setTimeout(() => {
      const next = query.trim();
      if (next !== keyword) update({ type, keyword: next, item: "" });
    }, 300);
    return () => window.clearTimeout(timer);
  }, [query, keyword, type, update]);
  function select(id: string) {
    setParams((current) => {
      const next = new URLSearchParams(current);
      if (id) next.set("item", id);
      else next.delete("item");
      return next;
    });
  }
  return (
    <>
      <div className="adm-heading">
        <div>
          <span className="adm-eyebrow">REVIEWS</span>
          <h1>후기 관리</h1>
          <p>서비스 전체 후기를 살피고 부적절한 콘텐츠를 관리합니다.</p>
        </div>
      </div>
      <section className="adm-panel">
        <div className="adm-toolbar">
          <label className="adm-search">
            <Search size={17} />
            <input
              type="search"
              aria-label="후기 검색"
              placeholder="후기 제목, 작성자, 행사명 검색"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          </label>
          <div className="adm-filters">
            <select
              aria-label="후기 검색 기준"
              value={type}
              onChange={(event) =>
                update({
                  type: event.target.value as PostSearchType,
                  keyword: query.trim(),
                  item: "",
                })
              }
            >
              {searchTypes.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
            <select
              aria-label="후기 정렬"
              value={sort === "createdAt,asc" ? sort : "createdAt,desc"}
              onChange={(event) =>
                update({ sort: event.target.value, item: "" })
              }
            >
              <option value="createdAt,desc">최신순</option>
              <option value="createdAt,asc">오래된순</option>
            </select>
          </div>
        </div>
        <div className="adm-result-count">
          {loading ? (
            "불러오는 중…"
          ) : (
            <>
              검색 결과 <strong>{data?.totalElements ?? 0}</strong>건
              {(data?.totalPages ?? 0) > 1 && (
                <span className="adm-page-hint">
                  {" "}
                  · {page + 1} / {data?.totalPages} 페이지
                </span>
              )}
            </>
          )}
        </div>
        {error && (
          <p className="adm-dialog-note" role="alert">
            {error}{" "}
            <button type="button" className="adm-row-button" onClick={reload}>
              다시 시도
            </button>
          </p>
        )}
        {loading ? (
          <Loading />
        ) : !data?.content.length ? (
          <Empty
            title="조건에 맞는 결과가 없습니다"
            description="검색어나 필터를 변경해 주세요."
          />
        ) : (
          <div
            className="adm-table-scroll"
            role="region"
            aria-label="후기 목록"
            tabIndex={0}
          >
            <table className="adm-table">
              <thead>
                <tr>
                  <th scope="col">후기 / 행사</th>
                  <th scope="col">작성자</th>
                  <th scope="col">상태</th>
                  <th scope="col">등록일</th>
                  <th scope="col">관리</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((post) => (
                  <tr key={post.id}>
                    <td>
                      <div className="adm-content-cell">
                        <div>
                          <strong>
                            {post.title}
                            {post.deletedAt && " (삭제됨)"}
                          </strong>
                          <small>{post.festivalTitle}</small>
                        </div>
                      </div>
                    </td>
                    <td>{post.member.nickname}</td>
                    <td>
                      <Badge tone={post.deletedAt ? "orange" : "green"}>
                        {post.deletedAt ? "삭제됨" : "게시 중"}
                      </Badge>
                    </td>
                    <td>{dateText(post.date)}</td>
                    <td>
                      <button
                        className="adm-row-button"
                        aria-label={`${post.title} 검토`}
                        onClick={() => select(post.id)}
                      >
                        검토하기
                        <ChevronRight size={14} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <ReviewPagination
          page={data?.number ?? page}
          totalPages={data?.totalPages ?? 0}
          onChange={(next) => update({ page: next })}
        />
      </section>
      {selectedId && (
        <AdminReviewDetail
          key={selectedId}
          id={selectedId}
          summary={data?.content.find((post) => post.id === selectedId)}
          onClose={() => select("")}
          onChanged={reload}
        />
      )}
    </>
  );
}

function ReviewPagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;
  const start = Math.max(0, Math.min(page - 2, totalPages - 5));
  const numbers = Array.from(
    { length: Math.min(5, totalPages) },
    (_, index) => start + index,
  );
  return (
    <nav className="adm-pagination" aria-label="페이지 이동">
      <button
        type="button"
        disabled={page === 0}
        onClick={() => onChange(page - 1)}
      >
        이전
      </button>
      {numbers.map((number) => (
        <button
          key={number}
          type="button"
          className={number === page ? "active" : ""}
          aria-current={number === page ? "page" : undefined}
          onClick={() => onChange(number)}
        >
          {number + 1}
        </button>
      ))}
      <button
        type="button"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        다음
      </button>
    </nav>
  );
}

function AdminReviewDetail({
  id,
  summary,
  onClose,
  onChanged,
}: {
  id: string;
  summary?: AdminPostSummary;
  onClose: () => void;
  onChanged: () => void;
}) {
  const { api, toast } = useApp();
  const knownDeleted = !!summary?.deletedAt;
  const detail = useLoad(async () => {
    if (knownDeleted) return null;
    try {
      return await api.adminPost(id);
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) return null;
      throw error;
    }
  }, [api, id, knownDeleted]);
  const [confirmPost, setConfirmPost] = useState(false);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState("");
  const removed = knownDeleted || (!detail.loading && !detail.error && !detail.data);
  const title = detail.data?.title ?? summary?.title ?? "후기";
  const author = detail.data?.member.nickname ?? summary?.member.nickname;
  const when = detail.data?.date ?? summary?.date;
  const festivalId = detail.data?.festivalId ?? summary?.festivalId;
  const festivalTitle = detail.data?.festivalTitle ?? summary?.festivalTitle;

  async function removePost() {
    setBusy(true);
    setActionError("");
    try {
      await api.deletePost(id);
      toast("후기를 삭제했습니다.");
      onChanged();
      onClose();
    } catch (error) {
      setActionError(errorText(error));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title="후기 관리" onClose={onClose}>
      {detail.loading ? (
        <Loading />
      ) : detail.error ? (
        <ErrorState message={detail.error} retry={detail.reload} />
      ) : (
        <>
          <div className="adm-dialog-title">
            <h3>{title}</h3>
            <div className="adm-review-meta">
              {author && <span>작성자 : {author}</span>}
              <span className="adm-review-date">
                {when && dateTimeText(when)}
                {removed && summary?.deletedAt &&
                  `${when ? " · " : ""}삭제 ${dateTimeText(summary.deletedAt)}`}
              </span>
            </div>
          </div>
          {festivalId != null && festivalTitle && (
            <Link className="adm-target-link" to={`/events/${festivalId}`}>
              {festivalTitle}
            </Link>
          )}
          {removed ? (
            <p className="adm-dialog-note">
              삭제된 후기입니다. 일반 사용자 목록에는 보이지 않고, 본문과
              댓글도 더 이상 조회되지 않습니다.
            </p>
          ) : (
            detail.data && (
              <>
                {detail.data.thumbnail && (
                  <Photo src={detail.data.thumbnail} alt="후기 첨부 사진" />
                )}
                <RichTextContent
                  content={detail.data.content}
                  className="adm-content-body rich-text-content"
                />
                <ReviewComments postId={id} />
              </>
            )
          )}
          {actionError && (
            <p className="adm-dialog-note" role="alert">
              {actionError}
            </p>
          )}
          {confirmPost ? (
            <div className="adm-dialog-actions">
              <button
                type="button"
                className="btn secondary"
                onClick={() => setConfirmPost(false)}
              >
                취소
              </button>
              <button
                type="button"
                className="btn danger"
                disabled={busy}
                onClick={removePost}
              >
                {busy ? "삭제 중…" : "후기를 삭제합니다"}
              </button>
            </div>
          ) : (
            <div className="adm-dialog-actions">
              <button type="button" className="btn secondary" onClick={onClose}>
                닫기
              </button>
              {!removed && (
                <button
                  type="button"
                  className="btn danger"
                  onClick={() => setConfirmPost(true)}
                >
                  후기 삭제
                </button>
              )}
            </div>
          )}
        </>
      )}
    </Modal>
  );
}

function ReviewComments({ postId }: { postId: string }) {
  const { api } = useApp();
  const [page, setPage] = useState(0);
  const { data, loading, error, reload } = useLoad(
    () => api.comments(postId, page),
    [api, postId, page],
  );
  return (
    <section className="adm-review-comments" aria-label="이 후기의 댓글">
      <h4>댓글</h4>
      {loading ? (
        <p className="adm-dialog-copy">댓글을 불러오는 중…</p>
      ) : error ? (
        <p className="adm-dialog-note" role="alert">
          {error}{" "}
          <button type="button" className="adm-row-button" onClick={reload}>
            다시 시도
          </button>
        </p>
      ) : data?.length ? (
        <>
          <ul className="adm-review-comment-list">
            {data.map((comment) => (
              <ReviewCommentRow
                key={comment.id}
                comment={comment}
                onDeleted={reload}
              />
            ))}
          </ul>
          <div className="adm-dialog-actions">
            <button
              type="button"
              className="btn secondary"
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
            >
              이전
            </button>
            <button
              type="button"
              className="btn secondary"
              disabled={data.length < 20}
              onClick={() => setPage(page + 1)}
            >
              다음
            </button>
          </div>
        </>
      ) : (
        <p className="adm-dialog-copy">
          {page > 0 ? "이 페이지에는 댓글이 없습니다." : "댓글이 없습니다."}
        </p>
      )}
    </section>
  );
}

function ReviewCommentRow({
  comment,
  onDeleted,
}: {
  comment: Comment;
  onDeleted: () => void;
}) {
  const { api, toast } = useApp();
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function remove() {
    setBusy(true);
    setError("");
    try {
      await api.deleteComment(comment.id);
      toast("댓글을 삭제했습니다.");
      setConfirming(false);
      onDeleted();
    } catch (deleteError) {
      setError(errorText(deleteError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <li>
      <div className="adm-review-comment-body">
        <div className="adm-review-comment-meta">
          <strong>{comment.nickname}</strong>
          <time dateTime={comment.date}>{dateTimeText(comment.date)}</time>
        </div>
        <p>{comment.content}</p>
        {error && (
          <p className="adm-dialog-note" role="alert">
            {error}
          </p>
        )}
      </div>
      {confirming ? (
        <span className="adm-review-comment-confirm">
          <button type="button" onClick={() => setConfirming(false)}>
            취소
          </button>
          <button type="button" disabled={busy} onClick={remove}>
            {busy ? "삭제 중" : "삭제"}
          </button>
        </span>
      ) : (
        <button
          type="button"
          className="adm-review-comment-delete"
          onClick={() => setConfirming(true)}
        >
          댓글 삭제
        </button>
      )}
    </li>
  );
}
