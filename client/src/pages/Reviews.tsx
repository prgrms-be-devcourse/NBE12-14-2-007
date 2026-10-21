import { useEffect, useState, type FormEvent } from "react";
import {
  Link,
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from "react-router-dom";
import {
  ArrowLeft,
  ArrowRight,
  Flag,
  MessageCircle,
  Pencil,
  ThumbsUp,
  Trash2,
} from "lucide-react";
import { ApiError, COMMENT_PAGE_SIZE } from "../lib/api";
import { AdminDeletionModal } from "../components/AdminDeletionModal";
import { ReviewSearch, useReviewQuery } from "../components/ReviewSearch";
import { ReviewRow } from "../components/ReviewRow";
import { FestivalPicker } from "../components/FestivalPicker";
import { isRichTextEmpty, RichTextContent } from "../components/RichText";
import { LazyRichTextEditor } from "../components/LazyRichTextEditor";
import { ReportableName, ReportButton } from "../components/ReportButton";
import { useApp, useLoad } from "../lib/context";
import { readDemo } from "../lib/demo";
import { dateText, dateTimeText, errorText } from "../lib/format";
import type { Comment, EventView, PostDetail } from "../lib/types";
import {
  Badge,
  Empty,
  ErrorState,
  Field,
  FormError,
  Loading,
  LoginRequired,
  Modal,
  PageTitle,
  Pagination,
  SubmitButton,
  Upload,
} from "../components/ui";

export function Reviews() {
  return (
    <div className="container page-space">
      <PageTitle
        eyebrow="MOMENTS WE SHARE"
        title="행사 후기"
        description="직접 경험한 즐거움을 나누고, 다음 나들이의 힌트를 발견해요."
      />
      <FestivalPosts />
    </div>
  );
}
export function FestivalPosts({ festivalId }: { festivalId?: number }) {
  const { api, mode } = useApp();
  const {
    page,
    sort: activeSort,
    type,
    keyword,
    update,
  } = useReviewQuery(mode === "preview");
  const { data, loading, error, reload } = useLoad(
    () => api.posts(festivalId, page, activeSort, { type, keyword }),
    [api, festivalId, page, activeSort, type, keyword],
  );
  return (
    <div className="festival-posts">
      {festivalId === undefined && (
        <ReviewSearch
          type={type}
          keyword={keyword}
          onSearch={(type, keyword) => update({ type, keyword })}
        />
      )}
      <div className="section-heading">
        <h2>
          함께 나눈 후기{" "}
          <span className="count">
            {loading || error ? "—" : (data?.totalElements ?? 0)}
          </span>
        </h2>
        <div className="action-row">
          <select
            aria-label="후기 정렬"
            className="plain-select"
            value={activeSort}
            onChange={(e) => {
              update({ sort: e.target.value });
            }}
          >
            <option value="likeCount,desc">좋아요순</option>
            <option value="createdAt,desc">최신순</option>
            <option value="createdAt,asc">오래된순</option>
          </select>
          <Link
            to={
              festivalId
                ? `/reviews/new?festival=${festivalId}`
                : "/reviews/new"
            }
            className="btn primary small"
          >
            <Pencil size={15} />
            후기 쓰기
          </Link>
        </div>
      </div>
      {loading ? (
        <Loading />
      ) : error ? (
        <ErrorState message={error} retry={reload} />
      ) : data?.content.length ? (
        <>
          <div className="post-list">
            {data.content.map((post) => (
              <ReviewRow post={post} key={post.id} />
            ))}
          </div>
          <Pagination
            page={data.number}
            total={data.totalPages}
            onChange={(page) => update({ page })}
          />
        </>
      ) : (
        <Empty
          title={
            keyword && festivalId === undefined
              ? "검색 조건에 맞는 후기가 없어요"
              : page > 0
                ? "해당 페이지에 후기가 없어요"
                : festivalId
                  ? "이 행사의 첫 이야기를 기다려요"
                  : "첫 번째 이야기를 기다려요"
          }
          description={
            keyword
              ? "다른 검색어로 다시 찾아보세요."
              : "기억에 남은 순간을 후기로 나눠주세요."
          }
          action={
            page > 0 ? (
              <button
                className="btn secondary"
                onClick={() => update({ page: 0 })}
              >
                첫 페이지로
              </button>
            ) : undefined
          }
        />
      )}
    </div>
  );
}
export function ReviewDetailPage() {
  const { postId = "" } = useParams();
  const { authLoading } = useApp();
  // 비회원도 볼 수 있다. 로그인 확인이 끝난 뒤에 불러와야 내 좋아요 상태가 맞게 온다.
  return (
    <div className="container page-space narrow">
      <Link to="/reviews" className="back-link">
        <ArrowLeft size={16} />
        후기 목록으로
      </Link>
      {authLoading ? <Loading /> : <ReviewDetail key={postId} id={postId} />}
    </div>
  );
}
function loginPath(location: { pathname: string; search: string }) {
  return `/login?next=${encodeURIComponent(location.pathname + location.search)}`;
}
function ReviewDetail({ id }: { id: string }) {
  const { api, member, mode, toast } = useApp();
  const navigate = useNavigate();
  const location = useLocation();
  const {
    data: post,
    loading,
    error,
    reload,
  } = useLoad(() => api.post(id), [api, id]);
  // 누르기 전에는 서버가 알려준 상태를 쓰고, 누른 뒤에는 그 결과를 쓴다.
  const [likedOverride, setLiked] = useState<boolean | null>(null);
  const [likeCountOverride, setLikeCount] = useState<number | null>(null);
  const likeCount = likeCountOverride ?? post?.likeCount ?? 0;
  const liked =
    likedOverride ??
    (mode === "preview"
      ? readDemo().liked.includes(id)
      : Boolean(post?.likedByMe));
  const [busy, setBusy] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [actionError, setActionError] = useState("");
  async function like(remove: boolean) {
    if (!member) {
      navigate(loginPath(location));
      return;
    }
    setBusy(true);
    setActionError("");
    try {
      const result = await api.like(id, remove);
      setLikeCount(result.likeCount);
      setLiked(!remove);
    } catch (e) {
      if (e instanceof ApiError && e.code === "LIKE000") {
        setLiked(true);
        // 다른 곳에서 이미 누른 경우라 지금 개수를 다시 받아 온다
        api
          .post(id)
          .then((latest) => setLikeCount(latest.likeCount ?? null))
          .catch(() => undefined);
        toast("이미 좋아요를 남긴 후기예요. 다시 누르면 취소할 수 있어요.");
      } else setActionError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  if (loading) return <Loading />;
  if (error) return <ErrorState message={error} retry={reload} />;
  if (!post) return null;
  // 탈퇴한 작성자는 id가 null이라, 비회원(member 없음)과 비교하면 같다고 나올 수 있다.
  const own = Boolean(member) && post.member.id === member?.id;
  const canDelete = own || member?.role === "ROLE_ADMIN";
  return (
    <>
      <article className="review-article">
        <Badge tone="green">행사 후기</Badge>
        <Link
          className="review-event-title post-event-link"
          to={`/events/${post.festivalId}`}
        >
          {post.festivalTitle}
          <ArrowRight size={14} />
        </Link>
        <h1>{post.title}</h1>
        <div className="article-meta">
          <span className="avatar">{post.member.nickname[0]}</span>
          <div>
            <ReportableName
              memberId={post.member.id}
              nickname={post.member.nickname}
            />
            <span>{dateText(post.date)}</span>
          </div>
          <div className="article-actions">
            {own && (
              <Link to={`/reviews/${id}/edit`} className="text-button">
                <Pencil size={14} />
                수정
              </Link>
            )}
            {canDelete && (
              <button
                className="text-button"
                onClick={() => setDeleteOpen(true)}
              >
                <Trash2 size={14} />
                삭제
              </button>
            )}
          </div>
        </div>
        <RichTextContent content={post.content} />
        <div className="review-reactions">
          <button
            disabled={busy}
            className={`btn ${liked ? "liked" : "secondary"}`}
            onClick={() => like(liked)}
            aria-pressed={liked}
          >
            <ThumbsUp size={18} />
            {liked ? "도움이 됐어요" : "도움돼요"}
            <strong>{likeCount}</strong>
          </button>
          {!own && (
            <ReportButton
              target={{
                type: "POST",
                id: post.id,
                name: post.title,
                details: [
                  `후기 주소: ${window.location.origin}/reviews/${post.id}`,
                ],
              }}
            >
              <Flag size={14} />
              신고하기
            </ReportButton>
          )}
        </div>
        <FormError message={actionError} />
      </article>
      <Comments postId={id} />
      {deleteOpen && !own ? (
        <AdminDeletionModal
          targetType="POST"
          targetId={id}
          onClose={() => setDeleteOpen(false)}
          onDeleted={() => {
            toast("후기를 삭제했어요.");
            navigate(`/reviews?festival=${post.festivalId}`);
          }}
        />
      ) : (
        deleteOpen && (
          <Modal
            title="후기를 삭제할까요?"
            onClose={() => setDeleteOpen(false)}
          >
            <p className="muted">삭제한 후기는 목록에서 사라져요.</p>
            <FormError message={actionError} />
            <div className="form-actions">
              <button
                className="btn secondary"
                onClick={() => setDeleteOpen(false)}
              >
                취소
              </button>
              <button
                className="btn danger"
                disabled={busy}
                onClick={async () => {
                  setBusy(true);
                  try {
                    await api.deletePost(id);
                    toast("후기를 삭제했어요.");
                    navigate(`/reviews?festival=${post.festivalId}`);
                  } catch (e) {
                    setActionError(errorText(e));
                  } finally {
                    setBusy(false);
                  }
                }}
              >
                삭제하기
              </button>
            </div>
          </Modal>
        )
      )}
    </>
  );
}
function Comments({ postId }: { postId: string }) {
  const { api, member } = useApp();
  const location = useLocation();
  const [page, setPage] = useState(0);
  const { data, loading, error, reload } = useLoad(
    () => api.comments(postId, page),
    [api, postId, page],
  );
  const [content, setContent] = useState("");
  const [busy, setBusy] = useState(false);
  const [formError, setFormError] = useState("");
  // 답글 입력창이 열린 일반 댓글. 한 번에 하나만 연다.
  const [replyTo, setReplyTo] = useState<{
    rootId: number;
    mention: string;
  } | null>(null);
  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!content.trim()) return;
    setBusy(true);
    setFormError("");
    try {
      await api.createComment(postId, content.trim());
      setContent("");
      // 오래된 순이라 새 댓글은 마지막 페이지 끝에 붙는다.
      const lastPage = Math.floor(
        (data?.totalElements ?? 0) / COMMENT_PAGE_SIZE,
      );
      if (lastPage === page) reload();
      else setPage(lastPage);
    } catch (e) {
      setFormError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  const rowProps = (comment: Comment) => ({
    comment,
    own: Boolean(member && comment.member?.id === member.id),
    admin: member?.role === "ROLE_ADMIN",
    reload,
  });
  return (
    <section className="comments-section">
      <h2>
        함께 이야기해요 <MessageCircle size={20} />
      </h2>
      {member ? (
        <form className="comment-form" onSubmit={submit}>
          <label className="sr-only" htmlFor="new-comment">
            댓글 내용
          </label>
          <textarea
            id="new-comment"
            required
            maxLength={500}
            rows={3}
            placeholder="따뜻한 한마디를 남겨주세요."
            value={content}
            onChange={(e) => setContent(e.target.value)}
          />
          <div>
            <span className="muted small-text">{content.length} / 500</span>
            <SubmitButton busy={busy}>댓글 남기기</SubmitButton>
          </div>
          <FormError message={formError} />
        </form>
      ) : (
        <p className="muted">
          댓글을 남기려면 <Link to={loginPath(location)}>로그인</Link>해 주세요.
        </p>
      )}
      {loading ? (
        <Loading />
      ) : error ? (
        <ErrorState message={error} retry={reload} />
      ) : data?.content.length ? (
        <div className="comment-list">
          {data.content.map((root) => (
            <div key={root.id} className="comment-thread">
              <CommentRow
                {...rowProps(root)}
                onReply={
                  member
                    ? () => setReplyTo({ rootId: root.id, mention: "" })
                    : undefined
                }
              />
              {(root.replies.length > 0 || replyTo?.rootId === root.id) && (
                <div className="comment-replies">
                  {root.replies.map((reply) => (
                    <CommentRow
                      key={reply.id}
                      {...rowProps(reply)}
                      onReply={
                        member
                          ? () =>
                              setReplyTo({
                                rootId: root.id,
                                mention: reply.member
                                  ? `@${reply.member.nickname} `
                                  : "",
                              })
                          : undefined
                      }
                    />
                  ))}
                  {replyTo?.rootId === root.id && (
                    <ReplyForm
                      key={`${root.id}-${replyTo.mention}`}
                      postId={postId}
                      parentId={root.id}
                      mention={replyTo.mention}
                      onClose={() => setReplyTo(null)}
                      onDone={() => {
                        setReplyTo(null);
                        reload();
                      }}
                    />
                  )}
                </div>
              )}
            </div>
          ))}
          <div className="comment-pagination">
            <button
              className="text-button"
              disabled={data.first}
              onClick={() => setPage(page - 1)}
            >
              이전 댓글
            </button>
            <span>
              {page + 1} / {Math.max(1, data.totalPages)} 페이지
            </span>
            <button
              className="text-button"
              disabled={data.last}
              onClick={() => setPage(page + 1)}
            >
              다음 댓글
            </button>
          </div>
        </div>
      ) : (
        <p className="empty-comments">
          {page ? "더 이상 댓글이 없어요." : "첫 번째 댓글을 남겨보세요."}
          {page > 0 && (
            <button className="text-button" onClick={() => setPage(page - 1)}>
              이전 댓글로
            </button>
          )}
        </p>
      )}
    </section>
  );
}
function ReplyForm({
  postId,
  parentId,
  mention,
  onClose,
  onDone,
}: {
  postId: string;
  parentId: number;
  mention: string;
  onClose: () => void;
  onDone: () => void;
}) {
  const { api } = useApp();
  const [value, setValue] = useState(mention);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!value.trim()) return;
    setBusy(true);
    setError("");
    try {
      await api.createComment(postId, value.trim(), parentId);
      onDone();
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <form className="comment-form reply-form" onSubmit={submit}>
      <textarea
        aria-label="답글 내용"
        required
        autoFocus
        maxLength={500}
        rows={2}
        placeholder="답글을 남겨주세요."
        value={value}
        onChange={(e) => setValue(e.target.value)}
      />
      <div>
        <span className="muted small-text">{value.length} / 500</span>
        <span className="form-actions">
          <button
            type="button"
            className="btn secondary small"
            onClick={onClose}
          >
            취소
          </button>
          <SubmitButton busy={busy}>답글 남기기</SubmitButton>
        </span>
      </div>
      <FormError message={error} />
    </form>
  );
}
function CommentRow({
  comment,
  own,
  admin,
  reload,
  onReply,
}: {
  comment: Comment;
  own: boolean;
  admin: boolean;
  reload: () => void;
  onReply?: () => void;
}) {
  const { api } = useApp();
  const [editing, setEditing] = useState(false);
  const [value, setValue] = useState(comment.content);
  const [deleting, setDeleting] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function save(remove = false) {
    setBusy(true);
    setError("");
    try {
      if (remove) await api.deleteComment(comment.id);
      else await api.updateComment(comment.id, value.trim());
      setEditing(false);
      setDeleting(false);
      reload();
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  // 삭제된 댓글은 서버가 내려준 문구만 보여 준다. 작성자도 버튼도 없다.
  if (comment.deleted || !comment.member)
    return (
      <div className="comment-row deleted">
        <span className="avatar small" aria-hidden="true" />
        <div>
          <p className="prose">{comment.content}</p>
        </div>
      </div>
    );
  const author = comment.member;
  return (
    <div className="comment-row">
      <span className="avatar small">{author.nickname[0]}</span>
      <div>
        <div className="comment-meta">
          <ReportableName memberId={author.id} nickname={author.nickname} />
          <time dateTime={comment.date}>{dateTimeText(comment.date)}</time>
          <span />
          {onReply && (
            <button className="text-button" onClick={onReply}>
              답글
            </button>
          )}
          {!own && (
            <ReportButton
              ariaLabel={`${author.nickname} 님의 댓글 신고`}
              target={{
                type: "COMMENT",
                id: String(comment.id),
                name: `${author.nickname} 님의 댓글 "${comment.content.slice(0, 30)}${comment.content.length > 30 ? "…" : ""}"`,
                details: [
                  `후기 주소: ${window.location.origin}/reviews/${comment.postId}`,
                  `댓글 내용: ${comment.content}`,
                ],
              }}
            >
              신고
            </ReportButton>
          )}
          {own && (
            <button
              className="text-button"
              onClick={() => setEditing(!editing)}
            >
              수정
            </button>
          )}
          {(own || admin) && (
            <button className="text-button" onClick={() => setDeleting(true)}>
              삭제
            </button>
          )}
        </div>
        {editing ? (
          <form
            onSubmit={(e) => {
              e.preventDefault();
              save();
            }}
          >
            <textarea
              aria-label="수정할 댓글"
              required
              maxLength={500}
              value={value}
              onChange={(e) => setValue(e.target.value)}
            />
            <div className="form-actions">
              <button
                type="button"
                className="btn secondary small"
                onClick={() => setEditing(false)}
              >
                취소
              </button>
              <SubmitButton busy={busy}>저장</SubmitButton>
            </div>
          </form>
        ) : (
          <p className="prose">{comment.content}</p>
        )}
        <FormError message={error} />
      </div>
      {deleting && admin && !own ? (
        <AdminDeletionModal
          targetType="COMMENT"
          targetId={comment.id}
          onClose={() => setDeleting(false)}
          onDeleted={reload}
        />
      ) : (
        deleting && (
          <Modal title="댓글을 삭제할까요?" onClose={() => setDeleting(false)}>
            <p className="muted">
              삭제한 댓글은 "삭제된 댓글입니다"로 표시되고, 달린 답글은 그대로
              남아요.
            </p>
            <FormError message={error} />
            <div className="form-actions">
              <button
                className="btn secondary"
                onClick={() => setDeleting(false)}
              >
                취소
              </button>
              <button
                className="btn danger"
                disabled={busy}
                onClick={() => save(true)}
              >
                삭제
              </button>
            </div>
          </Modal>
        )
      )}
    </div>
  );
}
export function ReviewFormPage() {
  const { postId } = useParams();
  const [params] = useSearchParams();
  const { member, authLoading } = useApp();
  const festivalId = Number(params.get("festival"));
  return (
    <div className="container page-space narrow">
      <Link
        className="back-link"
        to={postId ? `/reviews/${postId}` : "/reviews"}
      >
        <ArrowLeft size={16} />
        돌아가기
      </Link>
      <PageTitle
        eyebrow="YOUR MOMENT MATTERS"
        title={postId ? "후기 수정하기" : "어떤 하루를 보내셨나요?"}
        description="직접 경험한 이야기가 다른 이웃에게 좋은 길잡이가 돼요."
      />
      {authLoading ? (
        <Loading />
      ) : !member ? (
        <LoginRequired />
      ) : postId ? (
        <LoadReviewEdit id={postId} />
      ) : (
        <ReviewForm
          initialFestivalId={
            Number.isInteger(festivalId) && festivalId > 0
              ? festivalId
              : undefined
          }
        />
      )}
    </div>
  );
}
function LoadReviewEdit({ id }: { id: string }) {
  const { api, member } = useApp();
  const { data, loading, error, reload } = useLoad(
    () => api.post(id),
    [api, id],
  );
  return loading ? (
    <Loading />
  ) : error ? (
    <ErrorState message={error} retry={reload} />
  ) : data?.member.id !== member?.id ? (
    <Empty title="내가 작성한 후기만 수정할 수 있어요" />
  ) : data ? (
    <ReviewForm existing={data} />
  ) : null;
}
function ReviewForm({
  initialFestivalId,
  existing,
}: {
  /** 행사 상세나 행사별 후기 목록에서 들어오면 그 행사를 미리 골라 둔다. */
  initialFestivalId?: number;
  existing?: PostDetail;
}) {
  const { api, mode, toast } = useApp();
  const navigate = useNavigate();
  const [festival, setFestival] = useState<EventView | null>(null);
  useEffect(() => {
    if (existing || !initialFestivalId) return;
    let current = true;
    api
      .event(initialFestivalId)
      .then((event) => {
        if (current) setFestival(event);
      })
      // 못 불러오면 검색 칸이 그대로 보이므로 사용자가 직접 고르면 된다.
      .catch(() => {});
    return () => {
      current = false;
    };
  }, [api, existing, initialFestivalId]);
  const festivalId = existing?.festivalId ?? festival?.festivalId;
  const [title, setTitle] = useState(existing?.title || "");
  const [content, setContent] = useState(existing?.content || "");
  const [thumbnail, setThumbnail] = useState(existing?.thumbnail || "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!festivalId) {
      setError("후기를 남길 행사를 먼저 선택해 주세요.");
      return;
    }
    if (title.trim().length < 2 || isRichTextEmpty(content)) {
      setError("제목은 2자 이상, 내용을 함께 입력해 주세요.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      let id = existing?.id;
      if (id)
        await api.updatePost(id, {
          title: title.trim(),
          content: content.trim(),
          thumbnail,
        });
      else
        id = (
          await api.createPost(festivalId, {
            title: title.trim(),
            content: content.trim(),
            thumbnail,
          })
        ).id;
      toast(
        mode === "preview"
          ? "미리보기 후기를 저장했어요."
          : "후기를 저장했어요.",
      );
      navigate(`/reviews/${id}`);
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <form className="editor-form" onSubmit={submit}>
      <div className="form-section">
        {!existing && (
          // Field는 label로 감싸서 안에 버튼이 여러 개 있으면 클릭이 엉뚱한 버튼으로 간다.
          <div className="field">
            <span>
              다녀온 행사
              <b className="required">*</b>
            </span>
            <FestivalPicker
              value={festival}
              onChange={(next) => {
                setFestival(next);
                setError("");
              }}
            />
          </div>
        )}
        <Field label="후기 제목" required hint={`${title.length} / 30자`}>
          <input
            required
            minLength={2}
            maxLength={30}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="오늘의 경험을 한 문장으로 표현해 주세요"
          />
        </Field>
        <span className="upload-label">썸네일 이미지</span>
        <Upload type="POST" value={thumbnail} onChange={setThumbnail} useKey />
        <p className="upload-hint">
          후기 목록에서 대표 사진으로 보여요. 본문에는 들어가지 않아요.
        </p>
        <Field label="후기 내용" required>
          <LazyRichTextEditor
            value={content}
            onChange={setContent}
            placeholder="어떤 점이 좋았나요? 다음에 방문할 이웃에게 전하고 싶은 팁도 좋아요."
            ariaLabel="후기 내용"
            onUploadImage={async (file) => (await api.upload(file, "POST")).url}
          />
        </Field>
      </div>
      <FormError message={error} />
      <div className="form-actions">
        <Link
          className="btn secondary"
          to={
            existing
              ? `/reviews/${existing.id}`
              : festivalId
                ? `/reviews?festival=${festivalId}`
                : "/reviews"
          }
        >
          취소
        </Link>
        <SubmitButton busy={busy}>
          <Pencil size={16} />
          {existing ? "수정 내용 저장" : "후기 등록하기"}
        </SubmitButton>
      </div>
    </form>
  );
}
