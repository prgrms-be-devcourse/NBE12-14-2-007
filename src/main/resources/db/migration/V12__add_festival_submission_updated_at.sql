-- 행사 제보의 생성일, 수정일, 삭제일을 관리한다.
ALTER TABLE festival_submission
    ADD COLUMN created_at TIMESTAMP(6) NOT NULL,
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL,
    ADD COLUMN deleted_at TIMESTAMP(6);

-- 행사 내용을 JSONB에서 TEXT로 변경한다.
ALTER TABLE festival
ALTER COLUMN content TYPE TEXT
    USING content::text;