-- 현재 삭제 여부는 deleted_at, 삭제·복구 주체와 사유는 deletion_history만 사용한다.
-- 과거 주체를 추측해 새 이력을 만들지 않으며 기존 마이그레이션은 변경하지 않는다.
ALTER TABLE festival DROP COLUMN deletion_type;
ALTER TABLE festival DROP COLUMN deleted_by;
ALTER TABLE festival DROP COLUMN deletion_reason;
