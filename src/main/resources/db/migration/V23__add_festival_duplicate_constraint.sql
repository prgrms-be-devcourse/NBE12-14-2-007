-- 사용자 제보 행사에서 일정·지역·링크가 모두 같은 중복 데이터를 DB 단계에서 차단한다.
-- 공공데이터와 삭제된 행사는 고유 인덱스 적용 대상에서 제외한다.
CREATE UNIQUE INDEX uk_member_festival_schedule_region_url
    ON festival (begin_de, end_de, region, url)
    WHERE provider_type = 'MEMBER'
      AND deleted_at IS NULL;
