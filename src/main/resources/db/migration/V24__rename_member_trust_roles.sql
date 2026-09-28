-- 회원 역할 이름을 실제 신뢰 등급 의미에 맞게 변경
ALTER TABLE member DROP CONSTRAINT ck_member_role;

-- WARNING과 ADMIN은 자동 등급 계산 대상이 아님
-- 나머지 회원은 현재 누적 반응을 기준으로 등급을 다시 계산한다.
UPDATE member
SET role = 'ROLE_UNVERIFIED'
WHERE role NOT IN ('ROLE_WARNING', 'ROLE_ADMIN');

-- 좋아요 또는 '정확해요' 중 하나가 10개 이상이면 Expert 등급이다.
UPDATE member target
SET role = 'ROLE_RECOGNIZED'
WHERE target.role = 'ROLE_UNVERIFIED'
  AND (
    (SELECT COUNT(festival_like.id)
     FROM "like" festival_like
     JOIN festival ON festival.id = festival_like.festival_id
     WHERE festival.member_id = target.id
       AND festival.provider_type = 'MEMBER'
       AND festival.deleted_at IS NULL
       AND festival_like.member_id <> festival.member_id) >= 10
    OR
    (SELECT COUNT(accuracy_vote.id)
     FROM festival_accuracy_vote accuracy_vote
     JOIN festival ON festival.id = accuracy_vote.festival_id
     WHERE festival.member_id = target.id
       AND festival.provider_type = 'MEMBER'
       AND festival.deleted_at IS NULL
       AND accuracy_vote.vote_type = 'ACCURATE'
       AND accuracy_vote.member_id <> festival.member_id) >= 10
  );

-- 두 조건이 모두 10개 이상이면 Master 등급이다.
UPDATE member target
SET role = 'ROLE_TRUSTED'
WHERE target.role = 'ROLE_RECOGNIZED'
  AND (SELECT COUNT(festival_like.id)
       FROM "like" festival_like
       JOIN festival ON festival.id = festival_like.festival_id
       WHERE festival.member_id = target.id
         AND festival.provider_type = 'MEMBER'
         AND festival.deleted_at IS NULL
         AND festival_like.member_id <> festival.member_id) >= 10
  AND (SELECT COUNT(accuracy_vote.id)
       FROM festival_accuracy_vote accuracy_vote
       JOIN festival ON festival.id = accuracy_vote.festival_id
       WHERE festival.member_id = target.id
         AND festival.provider_type = 'MEMBER'
         AND festival.deleted_at IS NULL
         AND accuracy_vote.vote_type = 'ACCURATE'
         AND accuracy_vote.member_id <> festival.member_id) >= 10;

ALTER TABLE member
    ADD CONSTRAINT ck_member_role CHECK (
        role IN ('ROLE_WARNING', 'ROLE_UNVERIFIED', 'ROLE_RECOGNIZED', 'ROLE_TRUSTED', 'ROLE_ADMIN')
    );
