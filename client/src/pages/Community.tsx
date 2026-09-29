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
  MessageCircle,
  Pencil,
  Plus,
  Search,
  Trash2,
} from "lucide-react";
import { LazyRichTextEditor } from "../components/LazyRichTextEditor";
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
  FREE: "자유 이야기",
  EVENT: "행사 추천",
  RESTAURANT: "맛집 추천",
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
        action={
          member ? (
            <Link className="btn primary" to="/community/new">
              <Plus size={17} /> 글쓰기
            </Link>
          ) : undefined
        }
      />

      <nav className="community-categories" aria-label="커뮤니티 카테고리">
        {[
          ["", "전체"],
          ["FREE", "자유 이야기"],
          ["EVENT", "행사 추천"],
          ["RESTAURANT", "맛집 추천"],
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
  const [busy, setBusy] = useState(false);

  async function removePost() {
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
      const lastPage = Math.floor(
        (post.data?.commentCount ?? 0) / COMMUNITY_COMMENT_PAGE_SIZE,
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

  async function removeComment(commentId: number) {
    if (!window.confirm("댓글을 삭제할까요?")) return;
    try {
      await api.deleteCommunityComment(commentId);
      const remainingCount = Math.max(0, (post.data?.commentCount ?? 1) - 1);
      const lastPage = Math.max(
        0,
        Math.ceil(remainingCount / COMMUNITY_COMMENT_PAGE_SIZE) - 1,
      );
      if (commentPage > lastPage) setCommentPage(lastPage);
      else comments.reload();
      post.setData((current) =>
        current
          ? { ...current, commentCount: Math.max(0, current.commentCount - 1) }
          : current,
      );
    } catch (error) {
      toast(errorText(error));
    }
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
          {(mine || isAdmin) && (
            <div className="community-owner-actions">
              {mine && (
                <Link className="text-button" to={`/community/${postId}/edit`}>
                  <Pencil size={15} /> 수정
                </Link>
              )}
              <button className="text-button danger" onClick={removePost}>
                <Trash2 size={15} /> 삭제
              </button>
            </div>
          )}
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
          {comments.data?.content.map((item) => {
            const commentMine = Boolean(member && item.member.id === member.id);
            const canDeleteComment = commentMine || isAdmin;
            return (
              <article key={item.id} className="community-comment">
                <div className="community-comment-heading">
                  <Author
                    nickname={item.member.nickname}
                    profileImg={item.member.profileImg}
                  />
                  <span>{dateTimeText(item.createdAt)}</span>
                </div>
                {editingId === item.id ? (
                  <div className="community-comment-edit">
                    <textarea
                      value={editingContent}
                      onChange={(event) =>
                        setEditingContent(event.target.value)
                      }
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
                {canDeleteComment && editingId !== item.id && (
                  <div className="community-comment-actions">
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
                    <button onClick={() => removeComment(item.id)}>삭제</button>
                  </div>
                )}
              </article>
            );
          })}
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
            <option value="FREE">자유 이야기</option>
            <option value="EVENT">행사 추천</option>
            <option value="RESTAURANT">맛집 추천</option>
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
