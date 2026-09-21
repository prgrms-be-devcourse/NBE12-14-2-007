-- 문의를 종류별로 나눈다. 일반 문의(QUESTION)와 신고(REPORT) 두 가지다.
-- 이미 데이터가 있는 테이블에 NOT NULL을 바로 붙이면 실패하므로
-- nullable로 추가 → 백필 → NOT NULL 승격 순서로 처리한다.

ALTER TABLE inquiry
    ADD COLUMN category VARCHAR(32);

-- 구분이 생기기 전에 등록된 글은 일반 문의로 본다.
UPDATE inquiry
SET category = 'QUESTION'
WHERE category IS NULL;

ALTER TABLE inquiry
    ALTER COLUMN category SET NOT NULL;
