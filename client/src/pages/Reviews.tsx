import { useState, type FormEvent } from "react";
import {
  Link,
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
import { ApiError } from "../lib/api";
import { useApp, useLoad } from "../lib/context";
import { readDemo } from "../lib/demo";
import { dateText, dateTimeText, errorText } from "../lib/format";
import type { Comment, PostDetail, PostSummary } from "../lib/types";
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
  Photo,
  SubmitButton,
  Upload,
} from "../components/ui";

export function Reviews() {
  const { member, authLoading } = useApp();
  return (
    <div className="container page-space">
      <PageTitle
        eyebrow="MOMENTS WE SHARE"
        title="행사 후기"
        description="직접 경험한 즐거움을 나누고, 다음 나들이의 힌트를 발견해요."
      />
      {authLoading ? (
        <Loading />
      ) : member ? (
        <FestivalPosts />
      ) : (
        <LoginRequired />
      )}
    </div>
  );
}
export function FestivalPosts({ festivalId }: { festivalId?: number }) {
  const { api, member } = useApp();
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState("createdAt,desc");
  const { data, loading, error, reload } = useLoad(
    () => api.posts(festivalId, page, sort),
    [api, festivalId, page, sort],
  );
  if (!member) return <LoginRequired />;
  return (
    <div className="festival-posts">
      <div className="section-heading">
        <h2>
          함께 나눈 후기{" "}
          <span className="count">{data?.totalElements ?? 0}</span>
        </h2>
        <div className="action-row">
          <select
            aria-label="후기 정렬"
            className="plain-select"
            value={sort}
            onChange={(e) => {
              setSort(e.target.value);
              setPage(0);
            }}
          >
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
              <PostRow post={post} key={post.id} />
            ))}
          </div>
          <Pagination page={page} total={data.totalPages} onChange={setPage} />
        </>
      ) : (
        <Empty
          title={
            festivalId
              ? "이 행사의 첫 이야기를 기다려요"
              : "첫 번째 이야기를 기다려요"
          }
          description="기억에 남은 순간을 후기로 나눠주세요."
        />
      )}
    </div>
  );
}
function PostRow({ post }: { post: PostSummary }) {
  return (
    <article className="post-row">
      <div className="post-row-copy">
        <span className="review-byline">
          <span className="avatar tiny">{post.member.nickname[0]}</span>
          {post.member.nickname}
          <time dateTime={post.date}>{dateText(post.date)}</time>
        </span>
        <h3>
          <Link to={`/reviews/${post.id}`}>{post.title}</Link>
        </h3>
        <Link
          className="post-event-link small-text"
          to={`/events/${post.festivalId}`}
        >
          {post.festivalTitle}
          <ArrowRight size={13} />
        </Link>
      </div>
      {post.thumbnail && (
        <Link
          className="post-thumbnail"
          to={`/reviews/${post.id}`}
          aria-label={`${post.title} 후기 보기`}
        >
          <Photo src={post.thumbnail} alt="" />
        </Link>
      )}
      <Link
        className="post-detail-link"
        to={`/reviews/${post.id}`}
        aria-label={`${post.title} 상세 보기`}
      >
        <ArrowRight size={18} />
      </Link>
    </article>
  );
}
export function ReviewDetailPage() {
  const { postId = "" } = useParams();
  const { member, authLoading } = useApp();
  return (
    <div className="container page-space narrow">
      <Link to="/reviews" className="back-link">
        <ArrowLeft size={16} />
        후기 목록으로
      </Link>
      {authLoading ? (
        <Loading />
      ) : member ? (
        <ReviewDetail key={postId} id={postId} />
      ) : (
        <LoginRequired />
      )}
    </div>
  );
}
function ReviewDetail({ id }: { id: string }) {
  const { api, member, mode, toast } = useApp();
  const navigate = useNavigate();
  const {
    data: post,
    loading,
    error,
    reload,
  } = useLoad(() => api.post(id), [api, id]);
  const likes = useLoad(() => api.likeCount(id), [api, id]);
  const [liked, setLiked] = useState(
    mode === "preview" && readDemo().liked.includes(id),
  );
  const [busy, setBusy] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [actionError, setActionError] = useState("");
  async function like(remove: boolean) {
    setBusy(true);
    setActionError("");
    try {
      const result = await api.like(id, remove);
      likes.setData(result.likeCount);
      setLiked(!remove);
    } catch (e) {
      if (e instanceof ApiError && e.code === "LIKE000") {
        setLiked(true);
        likes.reload();
        toast("이미 좋아요를 남긴 후기예요. 다시 누르면 취소할 수 있어요.");
      } else setActionError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  if (loading) return <Loading />;
  if (error) return <ErrorState message={error} retry={reload} />;
  if (!post) return null;
  const own = post.member.id === member?.id;
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
            <strong>{post.member.nickname}</strong>
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
        {post.thumbnail && (
          <Photo
            className="article-image"
            src={post.thumbnail}
            alt={post.title}
          />
        )}
        <div className="prose">{post.content}</div>
        <div className="review-reactions">
          <button
            disabled={busy || likes.loading}
            className={`btn ${liked ? "liked" : "secondary"}`}
            onClick={() => like(liked)}
            aria-pressed={liked}
          >
            <ThumbsUp size={18} />
            {liked ? "도움이 됐어요" : "도움돼요"}
            {likes.data !== null && <strong>{likes.data}</strong>}
          </button>
          <Link
            className="text-button muted"
            to={`/mypage?tab=inquiries&report=${encodeURIComponent(`후기 신고: ${post.title}\n후기 주소: ${window.location.origin}/reviews/${post.id}`)}`}
          >
            <Flag size={14} />
            신고하기
          </Link>
        </div>
        {likes.error && (
          <ErrorState message={likes.error} retry={likes.reload} />
        )}
        <FormError message={actionError} />
      </article>
      <Comments postId={id} />
      {deleteOpen && (
        <Modal title="후기를 삭제할까요?" onClose={() => setDeleteOpen(false)}>
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
      )}
    </>
  );
}
function Comments({ postId }: { postId: string }) {
  const { api, member } = useApp();
  const [page, setPage] = useState(0);
  const { data, loading, error, reload } = useLoad(
    () => api.comments(postId, page),
    [api, postId, page],
  );
  const [content, setContent] = useState("");
  const [busy, setBusy] = useState(false);
  const [formError, setFormError] = useState("");
  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!content.trim()) return;
    setBusy(true);
    setFormError("");
    try {
      await api.createComment(postId, content.trim());
      setContent("");
      setPage(0);
      reload();
    } catch (e) {
      setFormError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="comments-section">
      <h2>
        함께 이야기해요 <MessageCircle size={20} />
      </h2>
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
      {loading ? (
        <Loading />
      ) : error ? (
        <ErrorState message={error} retry={reload} />
      ) : data?.length ? (
        <div className="comment-list">
          {data.map((comment) => (
            <CommentRow
              key={comment.id}
              comment={comment}
              own={comment.memberId === member?.id}
              admin={member?.role === "ROLE_ADMIN"}
              reload={reload}
            />
          ))}
          <div className="comment-pagination">
            <button
              className="text-button"
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
            >
              이전 댓글
            </button>
            <span>{page + 1} 페이지</span>
            <button
              className="text-button"
              disabled={data.length < 20}
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
function CommentRow({
  comment,
  own,
  admin,
  reload,
}: {
  comment: Comment;
  own: boolean;
  admin: boolean;
  reload: () => void;
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
  return (
    <div className="comment-row">
      <span className="avatar small">{comment.nickname[0]}</span>
      <div>
        <div className="comment-meta">
          <strong>{comment.nickname}</strong>
          <time dateTime={comment.date}>{dateTimeText(comment.date)}</time>
          <span />
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
      {deleting && (
        <Modal title="댓글을 삭제할까요?" onClose={() => setDeleting(false)}>
          <p className="muted">작성한 댓글이 삭제됩니다.</p>
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
      ) : Number.isInteger(festivalId) && festivalId > 0 ? (
        <ReviewForm festivalId={festivalId} />
      ) : (
        <Empty
          title="후기를 남길 행사를 찾아주세요"
          description="행사 상세의 후기 탭에서 다녀온 이야기를 남길 수 있어요."
          action={
            <div className="action-row">
              <Link className="btn primary" to="/explore">
                지역 문화행사 보기
              </Link>
              <Link className="btn secondary" to="/submissions">
                제보된 행사 보기
              </Link>
            </div>
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
    <ReviewForm festivalId={data.festivalId} existing={data} />
  ) : null;
}
function ReviewForm({
  festivalId,
  existing,
}: {
  festivalId: number;
  existing?: PostDetail;
}) {
  const { api, mode, toast } = useApp();
  const navigate = useNavigate();
  const [title, setTitle] = useState(existing?.title || "");
  const [content, setContent] = useState(existing?.content || "");
  const [thumbnail, setThumbnail] = useState(existing?.thumbnail || "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function submit(e: FormEvent) {
    e.preventDefault();
    if (title.trim().length < 2 || !content.trim()) {
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
        <Field label="후기 내용" required>
          <textarea
            required
            rows={10}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="어떤 점이 좋았나요? 다음에 방문할 이웃에게 전하고 싶은 팁도 좋아요."
          />
        </Field>
        <span className="upload-label">기억에 남은 사진</span>
        <Upload type="POST" value={thumbnail} onChange={setThumbnail} />
      </div>
      <FormError message={error} />
      <div className="form-actions">
        <Link
          className="btn secondary"
          to={
            existing
              ? `/reviews/${existing.id}`
              : `/reviews?festival=${festivalId}`
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
