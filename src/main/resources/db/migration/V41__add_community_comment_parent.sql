-- 커뮤니티 댓글의 대댓글.
-- parent_id가 비어 있으면 일반 댓글이다. 대댓글은 일반 댓글에만 달 수 있다(한 단계).
-- 부모 댓글이 삭제돼도 행은 남기 때문에(soft delete) 대댓글이 부모를 잃지 않는다.
ALTER TABLE community_comment
    ADD COLUMN parent_id BIGINT;

ALTER TABLE community_comment
    ADD CONSTRAINT fk_community_comment_parent
        FOREIGN KEY (parent_id) REFERENCES community_comment (id);

-- 부모 댓글들의 대댓글을 한 번에 가져올 때 쓴다.
CREATE INDEX idx_community_comment_parent_id
    ON community_comment (parent_id);

-- 글의 일반 댓글을 오래된 순으로 페이지 나눌 때 쓴다.
CREATE INDEX idx_community_comment_post_root_created_at
    ON community_comment (post_id, created_at, id)
    WHERE parent_id IS NULL;
