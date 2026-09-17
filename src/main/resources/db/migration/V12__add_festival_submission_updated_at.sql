-- 사용자 행사 제보의 마지막 수정 시각을 기록
ALTER TABLE festival_submission
    ADD COLUMN updated_at TIMESTAMP(6);

-- 행사 내용을 JSONB에서 TEXT로 변경
ALTER TABLE festival
ALTER COLUMN content TYPE TEXT
    USING content::text;