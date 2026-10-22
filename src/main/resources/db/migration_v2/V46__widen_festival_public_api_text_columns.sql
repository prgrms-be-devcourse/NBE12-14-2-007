-- 전국 문화정보 API 원본 텍스트는 길이를 우리가 통제할 수 없어서,
-- VARCHAR(255)로 제한돼 있던 컬럼들을 길이 제한 없는 TEXT로 넓힌다.
-- (동기화 중 "value too long for type character varying(255)" 에러로 저장 실패하는 걸 막기 위함)
ALTER TABLE festival
    ALTER COLUMN place TYPE TEXT,
    ALTER COLUMN place_address TYPE TEXT,
    ALTER COLUMN partcpt_expn_info TYPE TEXT,
    ALTER COLUMN telno_info TYPE TEXT,
    ALTER COLUMN region_detail TYPE TEXT;
