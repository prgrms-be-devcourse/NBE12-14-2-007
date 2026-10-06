-- 후기 상세 화면 시연용 블로그형 mock 후기 1건입니다. 실제 방문 후기가 아닙니다.
-- V31의 회원(seed-maker@example.com)과 행사 제보 #2(2026 미디어아트 프로젝트 줄리안 오피)를 재사용합니다.
-- 본문은 RichTextSanitizer가 허용하는 태그(p, h2, strong, em, ul, ol, li, blockquote, br)만 씁니다.
-- 썸네일은 Unsplash 무료 라이선스 이미지의 외부 URL입니다. (ImageUrlResolver가 http(s) 값은 그대로 내려준다)
-- 고정 UUID와 중복 검사로 재실행 시 같은 데이터를 추가하지 않습니다.

INSERT INTO post (
    id, member_id, pu_fev_id, title, content, thumbnail, created_at, updated_at, deleted_at
)
SELECT
    '00000000-0000-7000-a034-000000000001'::UUID,
    author.id,
    festival.id,
    '줄리안 오피 미디어아트, 밤에 가길 잘했어요',
    '<p>금요일 퇴근길에 울산시립미술관에 들렀습니다. 수·금·토요일은 저녁 8시까지 연다고 해서 일부러 해가 질 무렵에 맞춰 갔는데, 결론부터 말하면 <strong>이 전시는 꼭 저녁에 보세요.</strong></p>'
    || '<h2>방문 정보 한눈에 보기</h2>'
    || '<ul>'
    || '<li><strong>장소</strong>: 울산시립미술관 XR랩 · 잔디마당 · 옥외 미디어스크린</li>'
    || '<li><strong>운영 시간</strong>: 10:00~18:00 (수·금·토요일은 20:00까지)</li>'
    || '<li><strong>관람료</strong>: 성인 1,000원, 울산시민 500원, 어린이·청소년·경로 무료</li>'
    || '<li><strong>관람 소요 시간</strong>: 실내외 합쳐서 1시간 30분 정도</li>'
    || '</ul>'
    || '<h2>실내 XR랩: 생각보다 오래 머문 곳</h2>'
    || '<p>입구에서 안내를 받고 가장 먼저 XR랩으로 들어갔습니다. 가상현실 작품은 기기를 쓰고 체험하는 방식이라 대기 줄이 조금 있었어요. 제가 갔을 때는 앞에 대여섯 명 정도라 10분 남짓 기다렸습니다.</p>'
    || '<p>단순한 선과 면으로 그린 인물이 걸어가는 장면을 가상 공간 안에서 보니, 평면 작품으로 볼 때와는 거리감이 전혀 달랐습니다. 고개를 돌릴 때마다 인물들이 나를 스쳐 지나가는 느낌이 들어서 <em>"아, 이래서 VR로 만들었구나"</em> 싶더라고요.</p>'
    || '<blockquote>체험은 한 사람씩 진행되니, 일행이 있다면 먼저 체험한 사람이 다음 사람 사진을 찍어 주는 것도 좋아요.</blockquote>'
    || '<h2>해가 지면 진짜 시작되는 야외 전시</h2>'
    || '<p>실내를 다 보고 나오니 딱 어둑해지는 시간이었습니다. 잔디마당과 옥외 미디어스크린 쪽은 낮에 지나가면서 봤을 때와 분위기가 완전히 달랐어요. 화면 속 인물들이 쉬지 않고 걸어가는데, 그 앞을 실제 사람들이 지나가니까 작품과 현실이 겹쳐 보이는 순간이 몇 번 있었습니다.</p>'
    || '<p>잔디마당 가장자리에 앉아 한 장면이 처음부터 끝까지 반복되는 걸 지켜봤는데, 이게 이번 관람에서 가장 기억에 남는 시간이었어요. 사진도 많이 찍었지만 결국 휴대폰을 내려놓고 그냥 보게 되더라고요.</p>'
    || '<h2>다녀와서 정리한 팁</h2>'
    || '<ol>'
    || '<li><strong>야간 개장일(수·금·토)을 노리세요.</strong> 실내는 해 지기 전에, 야외는 해 진 뒤에 보는 동선이 가장 좋았습니다.</li>'
    || '<li><strong>XR 체험은 입장하자마자.</strong> 마감 1시간 전쯤부터 대기가 길어지는 것 같았어요.</li>'
    || '<li><strong>가벼운 겉옷 챙기기.</strong> 9월 말인데도 해가 지니 잔디마당에 오래 앉아 있기엔 쌀쌀했습니다.</li>'
    || '<li><strong>주변 산책도 함께.</strong> 미술관 바로 옆이 원도심이라 관람 후 저녁 먹고 걷기 좋았어요.</li>'
    || '</ol>'
    || '<h2>총평</h2>'
    || '<p>관람료 1,000원이 미안할 정도로 알찬 전시였습니다. 작품 수가 아주 많지는 않지만, 한 작품 앞에 오래 머물수록 보이는 게 많아지는 전시라 <strong>시간 여유를 두고 천천히</strong> 보시길 추천합니다. 전시가 10월 5일까지라 고민 중이시면 이번 주말 저녁에 다녀오세요!</p>',
    'https://images.unsplash.com/photo-1773761541805-24e73c6a7bd4?w=1200&q=80&auto=format&fit=crop',
    TIMESTAMP '2026-09-25 22:40:00',
    TIMESTAMP '2026-09-25 22:40:00',
    NULL
FROM member author
JOIN festival_submission submission
  ON submission.id = '00000000-0000-7000-9100-000000000002'::UUID
 AND submission.deleted_at IS NULL
JOIN festival
  ON festival.id = submission.festival_id
 AND festival.deleted_at IS NULL
WHERE author.email = 'seed-maker@example.com'
  AND author.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM post existing
      WHERE existing.id = '00000000-0000-7000-a034-000000000001'::UUID
  );
