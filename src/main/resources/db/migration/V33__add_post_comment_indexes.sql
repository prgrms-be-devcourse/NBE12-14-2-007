-- 후기·댓글 조회용 인덱스. 부하 테스트(후기 40만, 댓글 80만)에서 아래 조회가 매번 테이블 전체를 읽었다.

-- 후기 최신순 목록
CREATE INDEX idx_post_created_at
    ON post (created_at DESC)
    WHERE deleted_at IS NULL;

-- 행사별 후기 최신순 목록
CREATE INDEX idx_post_festival_created_at
    ON post (pu_fev_id, created_at DESC)
    WHERE deleted_at IS NULL;

-- 후기의 댓글 조회
CREATE INDEX idx_comment_post_id
    ON "comment" (post_id);

-- V1 의 uk_like_src_member 와 V18 의 uk_like_post_member 가 같은 (src_id, member_id) unique 인덱스다.
-- 좋아요를 누를 때마다 두 번 갱신되므로 엔티티에 선언된 uk_like_post_member 만 남긴다.
DROP INDEX IF EXISTS uk_like_src_member;
