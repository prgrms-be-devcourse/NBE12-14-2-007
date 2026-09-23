-- 로컬 테스트 DB 전용. 기존 후기를 수정하지 않고 삭제 확인용 후기 한 건을 추가한다.
-- 고정 ID를 사용하므로 재실행해도 중복 생성하지 않는다.
INSERT INTO post (
    id, member_id, pu_fev_id, title, content, thumbnail,
    created_at, updated_at, deleted_at
)
SELECT
    'c823b456-a35f-4fb4-9127-81145d07db01'::uuid,
    p.member_id, p.pu_fev_id,
    'soft 삭제 확인용 후기',
    '관리자 검색에서만 보여야 하는 로컬 테스트 후기입니다.',
    p.thumbnail,
    LOCALTIMESTAMP, LOCALTIMESTAMP, LOCALTIMESTAMP
FROM post p
JOIN member m ON m.id = p.member_id
JOIN festival f ON f.id = p.pu_fev_id
WHERE p.deleted_at IS NULL AND m.deleted_at IS NULL
ORDER BY p.created_at DESC, p.id DESC
LIMIT 1
ON CONFLICT (id) DO NOTHING;
