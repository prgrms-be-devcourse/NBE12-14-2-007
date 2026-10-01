-- 후기 좋아요 수를 post 에 따로 둔다. (좋아요순 정렬용)
-- 누가 눌렀는지는 지금처럼 "like" 테이블에 남고, like_count 는 좋아요 등록·취소 때 같은 트랜잭션에서 1씩 바꾼다.
-- 그전에는 좋아요순 목록을 볼 때마다 "like" 전체를 후기별로 집계해서, 후기 40만·좋아요 200만 건 기준 1건에 2~5초 걸렸다.

ALTER TABLE post
    ADD COLUMN like_count BIGINT NOT NULL DEFAULT 0;

-- 이미 있는 좋아요 수를 채운다.
UPDATE post p
SET like_count = l.cnt
FROM (
    SELECT src_id, COUNT(*) AS cnt
    FROM "like"
    WHERE src_id IS NOT NULL
    GROUP BY src_id
) l
WHERE l.src_id = p.id;

-- 좋아요순 목록: ORDER BY like_count DESC, created_at DESC, id DESC
CREATE INDEX idx_post_like_count
    ON post (like_count DESC, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

-- 행사별 좋아요순 목록
CREATE INDEX idx_post_festival_like_count
    ON post (pu_fev_id, like_count DESC, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;
