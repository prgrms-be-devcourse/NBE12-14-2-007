-- MemberRole을 신뢰 등급 체계로 재정의함에 따른 기존 role 값 이관.
-- ROLE_WARNING < ROLE_UNVERIFIED < ROLE_NORMAL < ROLE_TRUSTED < ROLE_ADMIN
--
-- ROLE_USER(기존 일반 회원) -> ROLE_UNVERIFIED(신규 주최자)
-- ROLE_MANAGER(기존 매니저)  -> ROLE_TRUSTED(여러 차례 정상 개최)
UPDATE member SET role = 'ROLE_UNVERIFIED' WHERE role = 'ROLE_USER';
UPDATE member SET role = 'ROLE_TRUSTED' WHERE role = 'ROLE_MANAGER';

-- 정의되지 않은 등급 값이 저장되지 않도록 제약을 건다.
ALTER TABLE member
    ADD CONSTRAINT ck_member_role CHECK (
        role IN ('ROLE_WARNING', 'ROLE_UNVERIFIED', 'ROLE_NORMAL', 'ROLE_TRUSTED', 'ROLE_ADMIN')
    );
