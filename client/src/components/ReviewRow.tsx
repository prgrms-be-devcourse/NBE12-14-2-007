import { Link } from "react-router-dom";
import { ArrowRight, ThumbsUp } from "lucide-react";
import { dateText, dateTimeText } from "../lib/format";
import type { PostSummary } from "../lib/types";
import { Badge, Photo } from "./ui";

export function ReviewRow({
  post,
  management,
}: {
  post: PostSummary;
  management?: { deletedAt: string | null; onOpen?: () => void };
}) {
  const deletedAt = management?.deletedAt;
  const canOpen = !deletedAt && (!management || !!management.onOpen);
  const photo = <Photo src={post.thumbnail} alt="" />;
  return (
    <article
      className={`post-row${deletedAt ? " post-row-deleted" : ""}`}
      aria-label={post.title}
    >
      <div className="post-row-copy">
        <span className="review-byline">
          <span className="avatar tiny">{post.member.nickname[0]}</span>
          {post.member.nickname}
          <time dateTime={post.date}>{dateText(post.date)}</time>
          {management && (
            <Badge tone={deletedAt ? "orange" : "green"}>
              {deletedAt ? "삭제됨" : "게시 중"}
            </Badge>
          )}
          {post.likeCount !== undefined && (
            <span
              className="post-like-count"
              aria-label={`좋아요 ${post.likeCount}개`}
            >
              <ThumbsUp size={13} />
              {post.likeCount}
            </span>
          )}
        </span>
        <h3>
          {!canOpen ? (
            post.title
          ) : management ? (
            <button className="post-title-button" onClick={management.onOpen}>
              {post.title}
            </button>
          ) : (
            <Link to={`/reviews/${post.id}`}>{post.title}</Link>
          )}
        </h3>
        <Link
          className="post-event-link small-text"
          to={`/events/${post.festivalId}`}
        >
          {post.festivalTitle}
          <ArrowRight size={13} />
        </Link>
        {deletedAt && (
          <p className="post-deleted-note">
            삭제 시각{" "}
            <time dateTime={deletedAt}>{dateTimeText(deletedAt)}</time> · 일반
            사용자 목록에서는 보이지 않습니다.
          </p>
        )}
      </div>
      {post.thumbnail &&
        (!canOpen ? (
          <div className="post-thumbnail">{photo}</div>
        ) : management ? (
          <button
            className="post-thumbnail"
            onClick={management.onOpen}
            aria-label={`${post.title} 후기 보기`}
          >
            {photo}
          </button>
        ) : (
          <Link
            className="post-thumbnail"
            to={`/reviews/${post.id}`}
            aria-label={`${post.title} 후기 보기`}
          >
            {photo}
          </Link>
        ))}
      {canOpen &&
        (management ? (
          <button
            className="post-detail-link"
            onClick={management.onOpen}
            aria-label={`${post.title} 상세 보기`}
          >
            <ArrowRight size={18} />
          </button>
        ) : (
          <Link
            className="post-detail-link"
            to={`/reviews/${post.id}`}
            aria-label={`${post.title} 상세 보기`}
          >
            <ArrowRight size={18} />
          </Link>
        ))}
    </article>
  );
}
