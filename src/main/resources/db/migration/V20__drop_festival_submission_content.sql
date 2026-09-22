-- 행사 정보와 중복되던 별도 제보 내용을 제거한다.
ALTER TABLE festival_submission
    DROP COLUMN content;
