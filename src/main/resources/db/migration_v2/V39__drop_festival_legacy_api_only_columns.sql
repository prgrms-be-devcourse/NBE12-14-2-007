-- 경기도 API 전용이 아니라 관리자 등록·회원 제보 기능에서도 쓰던 컬럼들이지만,
-- 전국 문화정보 API로 전환하면서 더 이상 이 정보를 쓰지 않기로 해 완전히 제거한다.
-- (애플리케이션 코드의 instNm/hostInstNm/eventTmInfo/writngDe 관련 참조도 함께 제거했다.)
-- inst_nm 컬럼에 걸려 있던 trigram 인덱스(idx_festival_inst_nm_trgm, V34)는
-- 컬럼이 삭제되면 PostgreSQL이 함께 제거해준다.
--
-- hmpg_url은 경기도 API(HMPG_URL)에서만 채워지던 컬럼이다. 관리자 수정·회원 제보는
-- 둘 다 referenceUrl 하나만 받아서 url 컬럼에만 저장하므로 hmpg_url은 건드리지 않는다.
-- 새 전국 API도 url 필드 하나뿐이라 "url/hmpgUrl 중 하나를 골라 referenceUrl을 계산"하던
-- 로직 자체가 필요 없어져 hmpg_url도 함께 삭제한다.
ALTER TABLE festival
    DROP COLUMN inst_nm,
    DROP COLUMN host_inst_nm,
    DROP COLUMN event_tm_info,
    DROP COLUMN writng_de,
    DROP COLUMN hmpg_url;
