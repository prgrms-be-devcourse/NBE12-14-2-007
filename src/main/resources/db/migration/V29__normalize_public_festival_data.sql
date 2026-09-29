-- 새 정규화 로직(FestivalService.normalizeHomepageUrl / parseEndDate)이 반영되기 전에
-- 이미 동기화된 공공 행사 데이터를 같은 규칙으로 보정한다.

-- 1) hmpg_url: 의미 없는 placeholder 값("-", "undefined", 빈 문자열)은 NULL로 정리
UPDATE festival
SET hmpg_url = NULL
WHERE provider_type = 'PUBLIC'
  AND hmpg_url IS NOT NULL
  AND (trim(hmpg_url) = '' OR trim(hmpg_url) = '-' OR lower(trim(hmpg_url)) = 'undefined');

-- 2) hmpg_url: 문자열 중간에 http(s)://가 섞여 있으면 그 지점부터 잘라낸다 (예: "홈페이지 https://...")
UPDATE festival
SET hmpg_url = substring(hmpg_url from position('http://' in lower(hmpg_url)))
WHERE provider_type = 'PUBLIC'
  AND hmpg_url IS NOT NULL
  AND hmpg_url !~* '^https?://'
  AND position('http://' in lower(hmpg_url)) > 0;

UPDATE festival
SET hmpg_url = substring(hmpg_url from position('https://' in lower(hmpg_url)))
WHERE provider_type = 'PUBLIC'
  AND hmpg_url IS NOT NULL
  AND hmpg_url !~* '^https?://'
  AND position('https://' in lower(hmpg_url)) > 0;

-- 3) hmpg_url: 나머지(스킴이 아예 없는 순수 도메인 등)는 https://를 앞에 붙인다
UPDATE festival
SET hmpg_url = 'https://' || trim(hmpg_url)
WHERE provider_type = 'PUBLIC'
  AND hmpg_url IS NOT NULL
  AND hmpg_url !~* '^https?://';

-- 4) end_de: 자정(00:00:00)으로 저장된 종료일을 그 날 끝(23:59:59.999999)까지로 보정한다.
--    마지막 날 낮에 진행 중인 행사가 자정이 지나자마자 이미 종료된 것으로 잘못 판정되는 걸 막기 위함.
UPDATE festival
SET end_de = end_de + INTERVAL '1 day' - INTERVAL '1 microsecond'
WHERE provider_type = 'PUBLIC'
  AND end_de IS NOT NULL
  AND end_de = date_trunc('day', end_de);
