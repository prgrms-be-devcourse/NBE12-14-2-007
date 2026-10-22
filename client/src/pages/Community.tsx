import { useEffect, useState, type FormEvent } from "react";
import {
  Link,
  useNavigate,
  useParams,
  useSearchParams,
} from "react-router-dom";
import {
  ArrowLeft,
  Eye,
  Flag,
  MessageCircle,
  Pencil,
  Plus,
  Search,
  Trash2,
} from "lucide-react";
import { LazyRichTextEditor } from "../components/LazyRichTextEditor";
import { AdminDeletionModal } from "../components/AdminDeletionModal";
import { ReportButton } from "../components/ReportButton";
import { isRichTextEmpty, RichTextContent } from "../components/RichText";
import {
  Empty,
  ErrorState,
  Field,
  Loading,
  LoginRequired,
  PageTitle,
  Pagination,
} from "../components/ui";
import { useApp, useLoad } from "../lib/context";
import { dateTimeText, errorText, imageUrl } from "../lib/format";
import type {
  CommunityCategory,
  CommunityComment,
  CommunityPostDetail,
  CommunityPostInput,
} from "../lib/types";

const categoryLabels: Record<CommunityCategory, string> = {
  FREE: "자유",
  EVENT: "행사",
  RESTAURANT: "맛집",
};
const COMMUNITY_COMMENT_PAGE_SIZE = 20;

function Author({
  nickname,
  profileImg,
}: {
  nickname: string;
  profileImg: string | null;
}) {
  const profileUrl = imageUrl(profileImg);
  return (
    <span className="community-author">
      <span className="community-avatar">
        {profileUrl ? <img src={profileUrl} alt="" /> : nickname.slice(0, 1)}
      </span>
      {nickname}
    </span>
  );
}

export function CommunityPage() {
  const { api, member } = useApp();
  const [params, setParams] = useSearchParams();
  const page = Math.max(0, Number(params.get("page") || 0));
  const category = (params.get("category") || "") as CommunityCategory | "";
  const keyword = params.get("keyword") || "";
  const [draft, setDraft] = useState(keyword);
  const result = useLoad(
    () => api.communityPosts(page, category || undefined, keyword || undefined),
    [api, page, category, keyword],
  );

  function changeFilter(nextCategory: CommunityCategory | "") {
    const next = new URLSearchParams(params);
    if (nextCategory) next.set("category", nextCategory);
    else next.delete("category");
    next.delete("page");
    setParams(next);
  }

  function submitSearch(event: FormEvent) {
    event.preventDefault();
    const next = new URLSearchParams(params);
    draft.trim() ? next.set("keyword", draft.trim()) : next.delete("keyword");
    next.delete("page");
    setParams(next);
  }

  return (
    <div className="container page-space community-page">
      <PageTitle
        eyebrow="COMMUNITY"
        title="커뮤니티"
        description="밖으로 나갈 이유와 즐거운 이야기를 이웃과 나눠보세요."
      />

      <div className="community-toolbar">
        <nav className="community-categories" aria-label="커뮤니티 카테고리">
          {[
            ["", "전체"],
            ["FREE", "자유"],
            ["EVENT", "행사"],
            ["RESTAURANT", "맛집"],
          ].map(([value, label]) => (
            <button
              key={value}
              className={category === value ? "active" : ""}
              onClick={() => changeFilter(value as CommunityCategory | "")}
            >
              {label}
            </button>
          ))}
        </nav>
        {member && (
          <Link className="btn primary" to="/community/new">
            <Plus size={17} /> 글쓰기
          </Link>
        )}
      </div>

      <form className="community-search" onSubmit={submitSearch}>
        <Search size={18} />
        <input
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder="제목이나 내용으로 검색"
          aria-label="커뮤니티 검색어"
        />
        <button className="btn secondary small">검색</button>
      </form>

      {result.loading && <Loading />}
      {result.error && (
        <ErrorState message={result.error} retry={result.reload} />
      )}
      {result.data && result.data.content.length === 0 && (
        <Empty
          title="아직 등록된 이야기가 없어요"
          description="첫 번째 탈출 이야기를 남겨보세요."
          action={
            member ? (
              <Link className="btn primary small" to="/community/new">
                글쓰기
              </Link>
            ) : undefined
          }
        />
      )}
      {result.data && result.data.content.length > 0 && (
        <div className="community-list">
          {result.data.content.map((post) => (
            <Link
              key={post.id}
              to={`/community/${post.id}`}
              className="community-row"
            >
              <span
                className={`community-category category-${post.category.toLowerCase()}`}
              >
                {categoryLabels[post.category]}
              </span>
              <div className="community-row-copy">
                <h2>
                  {post.title}
                  {post.commentCount > 0 && (
                    <span className="community-comment-count">
                      [{post.commentCount}]
                    </span>
                  )}
                </h2>
                <div className="community-row-meta">
                  <span>{post.member.nickname}</span>
                  <span>{dateTimeText(post.createdAt)}</span>
                  <span>
                    <Eye size={12} /> {post.viewCount}
                  </span>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
      {result.data && (
        <Pagination
          page={result.data.number}
          total={result.data.totalPages}
          onChange={(nextPage) => {
            const next = new URLSearchParams(params);
            next.set("page", String(nextPage));
            setParams(next);
          }}
        />
      )}
    </div>
  );
}

export function CommunityDetailPage() {
  const { postId = "" } = useParams();
  const { api, member, toast } = useApp();
  const navigate = useNavigate();
  const post = useLoad(() => api.communityPost(postId), [api, postId]);
  const [commentPage, setCommentPage] = useState(0);
  const comments = useLoad(
    () => api.communityComments(postId, commentPage),
    [api, postId, commentPage],
  );
  const [comment, setComment] = useState("");
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editingContent, setEditingContent] = useState("");
  // 답글 입력창이 열린 일반 댓글. 한 번에 하나만 연다.
  const [replyTo, setReplyTo] = useState<{ rootId: number } | null>(null);
  const [replyContent, setReplyContent] = useState("");
  const [adminTarget, setAdminTarget] = useState<{
    type: "COMMUNITY_POST" | "COMMUNITY_COMMENT";
    id: string | number;
  } | null>(null);
  const [busy, setBusy] = useState(false);

  async function removePost() {
    if (member?.role === "ROLE_ADMIN" && post.data?.member.id !== member.id) {
      setAdminTarget({ type: "COMMUNITY_POST", id: postId });
      return;
    }
    if (!window.confirm("이 글을 삭제할까요?")) return;
    try {
      await api.deleteCommunityPost(postId);
      toast("글을 삭제했습니다.");
      navigate("/community");
    } catch (error) {
      toast(errorText(error));
    }
  }

  async function submitComment(event: FormEvent) {
    event.preventDefault();
    if (!comment.trim() || busy) return;
    setBusy(true);
    try {
      await api.createCommunityComment(postId, comment.trim());
      setComment("");
      // 오래된 순이라 새 댓글은 마지막 페이지 끝에 붙는다. 페이지는 일반 댓글 수로 나뉜다.
      const lastPage = Math.floor(
        (comments.data?.totalElements ?? 0) / COMMUNITY_COMMENT_PAGE_SIZE,
      );
      if (lastPage === commentPage) comments.reload();
      else setCommentPage(lastPage);
      post.setData((current) =>
        current
          ? { ...current, commentCount: current.commentCount + 1 }
          : current,
      );
    } catch (error) {
      toast(errorText(error));
    } finally {
      setBusy(false);
    }
  }

  async function submitReply(event: FormEvent) {
    event.preventDefault();
    if (!replyTo || !replyContent.trim() || busy) return;
    setBusy(true);
    try {
      await api.createCommunityComment(
        postId,
        replyContent.trim(),
        replyTo.rootId,
      );
      setReplyTo(null);
      setReplyContent("");
      comments.reload();
      post.setData((current) =>
        current
          ? { ...current, commentCount: current.commentCount + 1 }
          : current,
      );
    } catch (error) {
      toast(errorText(error));
    } finally {
      setBusy(false);
    }
  }

  function openReply(rootId: number, mention: string) {
    setReplyTo({ rootId });
    setReplyContent(mention);
  }

  async function saveComment(item: CommunityComment) {
    if (!editingContent.trim()) return;
    try {
      await api.updateCommunityComment(item.id, editingContent.trim());
      setEditingId(null);
      comments.reload();
    } catch (error) {
      toast(errorText(error));
    }
  }

  // 삭제해도 "삭제된 댓글입니다"로 자리가 남아서 페이지가 당겨지지 않는다.
  async function removeComment(item: CommunityComment) {
    if (member?.role === "ROLE_ADMIN" && item.member?.id !== member.id) {
      setAdminTarget({ type: "COMMUNITY_COMMENT", id: item.id });
      return;
    }
    if (!window.confirm("댓글을 삭제할까요?")) return;
    try {
      await api.deleteCommunityComment(item.id);
      comments.reload();
      post.setData((current) =>
        current
          ? { ...current, commentCount: Math.max(0, current.commentCount - 1) }
          : current,
      );
    } catch (error) {
      toast(errorText(error));
    }
  }

  function renderComment(item: CommunityComment, rootId: number) {
    // 삭제된 댓글은 서버가 내려준 문구만 보여 준다. 작성자도 버튼도 없다.
    if (item.deleted || !item.member)
      return (
        <article key={item.id} className="community-comment deleted">
          <p>{item.content}</p>
        </article>
      );
    const author = item.member;
    const commentMine = Boolean(member && author.id === member.id);
    const canDeleteComment = commentMine || isAdmin;
    return (
      <article key={item.id} className="community-comment">
        <div className="community-comment-heading">
          <Author nickname={author.nickname} profileImg={author.profileImg} />
          <span>{dateTimeText(item.createdAt)}</span>
        </div>
        {editingId === item.id ? (
          <div className="community-comment-edit">
            <textarea
              value={editingContent}
              onChange={(event) => setEditingContent(event.target.value)}
              maxLength={500}
              rows={3}
            />
            <button
              className="btn primary small"
              onClick={() => saveComment(item)}
            >
              저장
            </button>
            <button
              className="btn secondary small"
              onClick={() => setEditingId(null)}
            >
              취소
            </button>
          </div>
        ) : (
          <p>{item.content}</p>
        )}
        {editingId !== item.id && (
          <div className="community-comment-actions">
            {member && (
              <button
                onClick={() =>
                  openReply(
                    rootId,
                    item.parentId === null ? "" : `@${author.nickname} `,
                  )
                }
              >
                답글
              </button>
            )}
            {!commentMine && (
              <ReportButton
                className="community-report"
                ariaLabel={`${author.nickname} 님의 댓글 신고`}
                target={{
                  type: "COMMUNITY_COMMENT",
                  id: String(item.id),
                  name: `${author.nickname} 님의 댓글 "${item.content.slice(0, 30)}${item.content.length > 30 ? "…" : ""}"`,
                  details: [
                    `글 주소: ${window.location.origin}/community/${item.postId}`,
                    `댓글 내용: ${item.content}`,
                  ],
                }}
              >
                신고
              </ReportButton>
            )}
            {commentMine && (
              <button
                onClick={() => {
                  setEditingId(item.id);
                  setEditingContent(item.content);
                }}
              >
                수정
              </button>
            )}
            {canDeleteComment && (
              <button onClick={() => removeComment(item)}>삭제</button>
            )}
          </div>
        )}
      </article>
    );
  }

  if (post.loading)
    return (
      <div className="container page-space">
        <Loading />
      </div>
    );
  if (post.error || !post.data) {
    return (
      <div className="container page-space">
        <ErrorState message={post.error} retry={post.reload} />
      </div>
    );
  }
  const data = post.data;
  const mine = Boolean(member && data.member.id === member.id);
  const isAdmin = member?.role === "ROLE_ADMIN";

  return (
    <div className="container page-space community-detail-page">
      {adminTarget && (
        <AdminDeletionModal
          targetType={adminTarget.type}
          targetId={adminTarget.id}
          onClose={() => setAdminTarget(null)}
          onDeleted={() => {
            toast("삭제했습니다.");
            if (adminTarget.type === "COMMUNITY_POST") navigate("/community");
            else {
              comments.reload();
              post.reload();
            }
          }}
        />
      )}
      <Link to="/community" className="back-link">
        <ArrowLeft size={17} /> 목록으로
      </Link>
      <article className="community-detail">
        <header>
          <span
            className={`community-category category-${data.category.toLowerCase()}`}
          >
            {categoryLabels[data.category]}
          </span>
          <h1>{data.title}</h1>
          <div className="community-detail-meta">
            <Author
              nickname={data.member.nickname}
              profileImg={data.member.profileImg}
            />
            <span>{dateTimeText(data.createdAt)}</span>
            <span>
              <Eye size={15} /> 조회 {data.viewCount}
            </span>
            <span>
              <MessageCircle size={15} /> 댓글 {data.commentCount}
            </span>
          </div>
          <div className="community-owner-actions">
            {!mine && (
              <ReportButton
                className="text-button"
                target={{
                  type: "COMMUNITY_POST",
                  id: data.id,
                  name: data.title,
                  details: [
                    `글 주소: ${window.location.origin}/community/${data.id}`,
                  ],
                }}
              >
                <Flag size={15} /> 신고
              </ReportButton>
            )}
            {mine && (
              <Link className="text-button" to={`/community/${postId}/edit`}>
                <Pencil size={15} /> 수정
              </Link>
            )}
            {(mine || isAdmin) && (
              <button className="text-button danger" onClick={removePost}>
                <Trash2 size={15} /> 삭제
              </button>
            )}
          </div>
        </header>
        <RichTextContent
          content={data.content}
          className="community-content rich-text-content"
        />
      </article>

      <section className="community-comments">
        <h2>
          댓글 <span>{data.commentCount}</span>
        </h2>
        {member ? (
          <form className="community-comment-form" onSubmit={submitComment}>
            <textarea
              value={comment}
              onChange={(event) => setComment(event.target.value)}
              placeholder="함께 나누고 싶은 이야기를 남겨주세요."
              maxLength={500}
              rows={3}
              required
            />
            <button className="btn primary small" disabled={busy}>
              댓글 등록
            </button>
          </form>
        ) : (
          <p className="community-login-note">
            댓글을 작성하려면{" "}
            <Link to={`/login?next=/community/${postId}`}>로그인</Link>해
            주세요.
          </p>
        )}

        {comments.loading && <Loading />}
        {comments.error && (
          <ErrorState message={comments.error} retry={comments.reload} />
        )}
        {comments.data?.content.length === 0 && (
          <p className="community-no-comments">첫 댓글을 남겨보세요.</p>
        )}
        <div className="community-comment-list">
          {comments.data?.content.map((root) => (
            <div key={root.id} className="community-comment-thread">
              {renderComment(root, root.id)}
              {(root.replies.length > 0 || replyTo?.rootId === root.id) && (
                <div className="community-comment-replies">
                  {root.replies.map((reply) => renderComment(reply, root.id))}
                  {replyTo?.rootId === root.id && (
                    <form
                      className="community-comment-form community-reply-form"
                      onSubmit={submitReply}
                    >
                      <textarea
                        aria-label="답글 내용"
                        value={replyContent}
                        onChange={(event) =>
                          setReplyContent(event.target.value)
                        }
                        placeholder="답글을 남겨주세요."
                        maxLength={500}
                        rows={2}
                        autoFocus
                        required
                      />
                      <div className="community-reply-actions">
                        <button
                          type="button"
                          className="btn secondary small"
                          onClick={() => setReplyTo(null)}
                        >
                          취소
                        </button>
                        <button className="btn primary small" disabled={busy}>
                          답글 등록
                        </button>
                      </div>
                    </form>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
        {comments.data && (
          <Pagination
            page={comments.data.number}
            total={comments.data.totalPages}
            onChange={setCommentPage}
          />
        )}
      </section>
    </div>
  );
}

export function CommunityFormPage() {
  const { postId } = useParams();
  const { api, member, authLoading, toast } = useApp();
  const navigate = useNavigate();
  const existing = useLoad<CommunityPostDetail | null>(
    () => (postId ? api.communityPost(postId) : Promise.resolve(null)),
    [api, postId],
  );
  const [category, setCategory] = useState<CommunityCategory>("FREE");
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [busy, setBusy] = useState(false);
  const [formError, setFormError] = useState("");

  useEffect(() => {
    if (!existing.data) return;
    setCategory(existing.data.category);
    setTitle(existing.data.title);
    setContent(existing.data.content);
  }, [existing.data]);

  if (authLoading || existing.loading)
    return (
      <div className="container page-space">
        <Loading />
      </div>
    );
  if (!member)
    return (
      <div className="container page-space">
        <LoginRequired />
      </div>
    );
  if (existing.error)
    return (
      <div className="container page-space">
        <ErrorState message={existing.error} retry={existing.reload} />
      </div>
    );
  if (existing.data && existing.data.member.id !== member.id) {
    return (
      <div className="container page-space">
        <Empty
          title="수정 권한이 없어요"
          description="작성자 본인만 글을 수정할 수 있어요."
        />
      </div>
    );
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (title.trim().length < 2 || isRichTextEmpty(content)) {
      setFormError("제목은 2자 이상, 내용도 함께 입력해 주세요.");
      return;
    }
    const input: CommunityPostInput = {
      category,
      title: title.trim(),
      content,
    };
    setBusy(true);
    setFormError("");
    try {
      const saved = postId
        ? await api.updateCommunityPost(postId, input)
        : await api.createCommunityPost(input);
      toast(postId ? "글을 수정했습니다." : "새 이야기를 등록했습니다.");
      navigate(`/community/${saved.id}`);
    } catch (error) {
      setFormError(errorText(error));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="container page-space community-form-page">
      <PageTitle
        eyebrow="COMMUNITY"
        title={postId ? "이야기 수정" : "새 이야기 쓰기"}
        description="이웃과 나누고 싶은 즐거움을 편하게 적어주세요."
      />
      <form className="community-form" onSubmit={submit}>
        <Field label="카테고리" required>
          <select
            value={category}
            onChange={(event) =>
              setCategory(event.target.value as CommunityCategory)
            }
          >
            <option value="FREE">자유</option>
            <option value="EVENT">행사</option>
            <option value="RESTAURANT">맛집</option>
          </select>
        </Field>
        <Field label="제목" required>
          <input
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            maxLength={100}
            placeholder="이야기의 제목을 적어주세요."
            required
          />
        </Field>
        <Field label="내용" required>
          <LazyRichTextEditor
            value={content}
            onChange={setContent}
            placeholder="이웃에게 들려주고 싶은 이야기를 적어주세요."
            ariaLabel="커뮤니티 글 내용"
            onUploadImage={async (file) => (await api.upload(file, "POST")).url}
          />
        </Field>
        {formError && (
          <p className="form-error" role="alert">
            {formError}
          </p>
        )}
        <div className="community-form-actions">
          <Link
            className="btn secondary"
            to={postId ? `/community/${postId}` : "/community"}
          >
            취소
          </Link>
          <button className="btn primary" disabled={busy}>
            {busy ? "저장 중..." : "등록하기"}
          </button>
        </div>
      </form>
    </div>
  );
}
