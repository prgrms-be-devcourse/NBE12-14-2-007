-- 삭제 여부는 행사(festival.deleted_at) 한 곳에서만 관리한다.
-- 제보에도 deleted_at 이 있으면 "제보만 삭제된 행사"처럼 두 값이 어긋나는 상태가 생긴다.
ALTER TABLE festival_submission DROP COLUMN deleted_at;
