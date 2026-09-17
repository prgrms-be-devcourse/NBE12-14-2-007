-- 엔티티가 BaseTimeEntity / SoftDeletableEntity 를 상속하도록 변경되면서
-- 감사(auditing) 컬럼이 필요해졌다. ddl-auto=validate 라서 컬럼이 없으면 앱이 기동하지 않는다.
--
-- 이미 데이터가 있는 테이블에 NOT NULL 컬럼을 바로 추가하면 실패하므로
--   (1) nullable 로 추가 → (2) 기존 행 백필 → (3) NOT NULL 승격
-- 순서로 처리한다.
--
-- comment / post / member 테이블은 V1에서 이미 세 컬럼을 모두 갖고 있어 대상이 아니다.
-- festival_submission 은 V12에서 처리했다.

-- ── festival : SoftDeletableEntity ──────────────────────────────
ALTER TABLE festival
    ADD COLUMN created_at TIMESTAMP(6),
    ADD COLUMN updated_at TIMESTAMP(6),
    ADD COLUMN deleted_at TIMESTAMP(6);

-- writng_de 는 공공 API가 내려주는 원본 작성일이라 우리 시스템의 생성 시각과는 별개다.
-- 기존 행이 언제 동기화됐는지는 알 수 없으므로 마이그레이션 시각으로 채운다.
UPDATE festival
SET created_at = now(),
    updated_at = now()
WHERE created_at IS NULL;

ALTER TABLE festival
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

-- ── like : BaseTimeEntity (소프트 삭제 없음) ─────────────────────
ALTER TABLE "like"
    ADD COLUMN created_at TIMESTAMP(6),
    ADD COLUMN updated_at TIMESTAMP(6);

UPDATE "like"
SET created_at = now(),
    updated_at = now()
WHERE created_at IS NULL;

ALTER TABLE "like"
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

-- ── inquiry : SoftDeletableEntity ───────────────────────────────
ALTER TABLE inquiry
    ADD COLUMN created_at TIMESTAMP(6),
    ADD COLUMN updated_at TIMESTAMP(6),
    ADD COLUMN deleted_at TIMESTAMP(6);

UPDATE inquiry
SET created_at = now(),
    updated_at = now()
WHERE created_at IS NULL;

ALTER TABLE inquiry
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;
