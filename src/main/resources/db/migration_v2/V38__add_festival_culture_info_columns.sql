-- 전국 문화정보 API(한국문화정보원 period2/detail2) 전환을 위한 컬럼 추가.
-- external_id: API의 고유 seq를 저장해서 "건수 비교"가 아니라 "존재 여부"로 신규 행사를 판별한다.
-- place(ex 오페라 하우스) / place_address(ex 서울특별시 서초구 남북순환로): 새 API가 주는 장소명·주소를 그대로 저장한다 (기존 경기도 API엔 없던 정보).
ALTER TABLE festival
    ADD COLUMN external_id VARCHAR(64),
    ADD COLUMN place VARCHAR(255),
    ADD COLUMN place_address VARCHAR(255);

-- 같은 행사가 중복 저장되는 걸 DB 단계에서 막는다.
-- 기존 경기도 데이터는 external_id가 없어(NULL) 부분 인덱스 조건에서 제외되므로 충돌 없다.
CREATE UNIQUE INDEX uk_festival_provider_external_id
    ON festival (provider_type, external_id)
    WHERE external_id IS NOT NULL;
