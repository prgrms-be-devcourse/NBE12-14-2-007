-- 검색어는 제목·기관·상세지역 한가운데를 찾는다. 일반 인덱스는 이 형태를 타지 못한다.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_festival_title_trgm
    ON festival USING gin (lower(COALESCE(title, '')) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_festival_inst_nm_trgm
    ON festival USING gin (lower(COALESCE(inst_nm, '')) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_festival_region_detail_trgm
    ON festival USING gin (lower(COALESCE(region_detail, '')) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_festival_category_lower
    ON festival (lower(COALESCE(category, '')));
