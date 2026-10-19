ALTER TABLE festival ADD COLUMN deletion_type VARCHAR(16);
ALTER TABLE festival ADD COLUMN deleted_by UUID;
ALTER TABLE festival ADD COLUMN deletion_reason VARCHAR(500);

-- 이관 때문에 새로 삭제 처리되는 행사의 작성자만 등급을 다시 계산한다.
CREATE TEMP TABLE festival_deletion_affected_authors ON COMMIT DROP AS
SELECT DISTINCT f.member_id
FROM festival f JOIN festival_submission s ON s.festival_id = f.id
WHERE f.deleted_at IS NULL AND s.deleted_at IS NOT NULL AND f.member_id IS NOT NULL;

-- 제보만 삭제된 과거 데이터도 공개되지 않도록 행사에 삭제 시각을 이관한다.
UPDATE festival f
SET deleted_at = history.deleted_at
FROM (
    SELECT festival_id, MIN(deleted_at) AS deleted_at
    FROM festival_submission
    WHERE deleted_at IS NOT NULL
    GROUP BY festival_id
) history
WHERE f.id = history.festival_id AND f.deleted_at IS NULL;

-- 기존 기록만으로 삭제자를 추정하지 않는다. UNKNOWN은 복구할 수 없다.
UPDATE festival SET deletion_type = 'UNKNOWN', deletion_reason = '기존 삭제 기록: 삭제 주체 및 사유 확인 불가'
WHERE deleted_at IS NOT NULL;

UPDATE member m
SET role = CASE
    WHEN counts.likes >= 10 AND counts.accurate >= 10 THEN 'ROLE_TRUSTED'
    WHEN counts.likes >= 10 OR counts.accurate >= 10 THEN 'ROLE_RECOGNIZED'
    ELSE 'ROLE_UNVERIFIED'
END
FROM (
    SELECT a.member_id,
        (SELECT COUNT(*) FROM "like" l JOIN festival f ON f.id = l.festival_id
         WHERE f.member_id = a.member_id AND f.provider_type = 'MEMBER'
           AND f.deleted_at IS NULL AND l.member_id <> f.member_id) AS likes,
        (SELECT COUNT(*) FROM festival_accuracy_vote v JOIN festival f ON f.id = v.festival_id
         WHERE f.member_id = a.member_id AND f.provider_type = 'MEMBER'
           AND f.deleted_at IS NULL AND v.member_id <> f.member_id
           AND v.vote_type = 'ACCURATE') AS accurate
    FROM festival_deletion_affected_authors a
) counts
WHERE m.id = counts.member_id AND m.role NOT IN ('ROLE_ADMIN', 'ROLE_WARNING');

-- 필요한 삭제 정보를 행사로 이관한 뒤, 제보의 중복 삭제 상태를 제거한다.
ALTER TABLE festival_submission DROP COLUMN deleted_at;
