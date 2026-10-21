-- 삭제·복구의 주체와 사유는 이 테이블에만 쌓는다. 콘텐츠 테이블에는 deleted_at 만 둔다.
CREATE TABLE deletion_history (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    target_type VARCHAR(32) NOT NULL CHECK (target_type IN ('FESTIVAL','POST','COMMENT','COMMUNITY_POST','COMMUNITY_COMMENT')),
    target_id VARCHAR(64) NOT NULL,
    action VARCHAR(16) NOT NULL CHECK (action IN ('DELETE','RESTORE')),
    actor_id UUID NOT NULL REFERENCES member(id),
    actor_type VARCHAR(16) NOT NULL CHECK (actor_type IN ('AUTHOR','ADMIN')),
    reason_code VARCHAR(32) NOT NULL CHECK (reason_code IN ('AUTHOR_REQUEST','DUPLICATE','INCORRECT_INFORMATION','SPAM','ABUSIVE_CONTENT','PRIVACY_EXPOSURE','OTHER','ADMIN_RESTORE')),
    reason_detail VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    CHECK (reason_code <> 'OTHER' OR (reason_detail IS NOT NULL AND LENGTH(TRIM(reason_detail)) > 0)),
    CHECK (
        (action = 'RESTORE' AND actor_type = 'ADMIN' AND reason_code = 'ADMIN_RESTORE') OR
        (action = 'DELETE' AND actor_type = 'AUTHOR' AND reason_code = 'AUTHOR_REQUEST' AND reason_detail IS NULL) OR
        (action = 'DELETE' AND actor_type = 'ADMIN' AND reason_code NOT IN ('AUTHOR_REQUEST','ADMIN_RESTORE'))
    )
);
CREATE INDEX idx_deletion_history_target ON deletion_history (target_type, target_id, id DESC);
CREATE INDEX idx_deletion_history_actor ON deletion_history (actor_id, id DESC);
