-- 후기 상세 화면 시연용 블로그형 mock 후기 2건입니다. 실제 방문 후기가 아닙니다.
-- V31의 회원과 행사 제보 #3(업사이클링 초보 재봉 워크숍), #9(하반기 진로활동 바리스타)를 재사용합니다.
-- 본문은 RichTextSanitizer가 허용하는 태그(p, h2, strong, em, ul, ol, li, blockquote, br)만 씁니다.
-- 썸네일은 Unsplash 무료 라이선스 이미지의 외부 URL입니다. 원본이 5000px 이상이라 상세 화면에서도 충분하다.
-- 고정 UUID와 중복 검사로 재실행 시 같은 데이터를 추가하지 않습니다.

WITH seed_post (post_no, submission_no, author_email, title, content, thumbnail, written_at) AS (
    VALUES
        (
            2,
            3,
            'seed-master@example.com',
            '재봉틀 처음 만져본 날, 파우치 하나 완성!',
            '<p>제주 여행 마지막 날, 비행기 시간까지 애매하게 남아서 미리 예약해 둔 <strong>업사이클링 초보 재봉 워크숍</strong>에 다녀왔습니다. 재봉틀은 학교 가사 시간 이후로 처음이라 솔직히 걱정이 많았는데, 결론은 <strong>완전 초보도 괜찮아요.</strong></p>'
            || '<h2>참여 정보</h2>'
            || '<ul>'
            || '<li><strong>장소</strong>: 제주더큰내일센터 1층 (제주시 관덕로11길 34)</li>'
            || '<li><strong>시간</strong>: 매주 일·월요일 13:00~15:30</li>'
            || '<li><strong>신청</strong>: 사전 예약 필수 (현장 참여는 어려워요)</li>'
            || '<li><strong>준비물</strong>: 없음. 원단과 도구는 모두 준비되어 있어요</li>'
            || '</ul>'
            || '<h2>원단 고르는 시간이 제일 설렜어요</h2>'
            || '<p>테이블 위에 버려질 뻔한 폐원단이 색깔별로 쌓여 있었는데, 이 중에서 마음에 드는 걸 직접 골라서 쓰는 방식이었습니다. 데님, 리넨, 체크무늬 셔츠 천까지 종류가 다양해서 고르는 데만 15분은 쓴 것 같아요.</p>'
            || '<p>저는 낡은 청바지 조각에 체크 원단을 덧대기로 했는데, 같은 날 참여한 분들 결과물이 하나도 겹치지 않아서 나중에 서로 구경하는 재미도 있었습니다.</p>'
            || '<h2>재봉틀, 생각보다 무섭지 않아요</h2>'
            || '<p>처음 30분은 실 끼우는 법, 밑실 감는 법, 발판 속도 조절하는 법을 차근차근 알려 주셨어요. 버려지는 천에 직선 박기부터 연습하고 나니 손이 조금 익숙해졌습니다.</p>'
            || '<blockquote>발판은 생각보다 훨씬 살살 밟아야 해요. 처음에 꾹 밟았다가 천이 혼자 달려가서 다들 한 번씩 웃었습니다.</blockquote>'
            || '<p>모서리를 돌릴 때 바늘을 꽂은 채로 노루발을 올리고 천을 돌리는 요령을 배웠는데, 이걸 알고 나니 결과물이 훨씬 깔끔해지더라고요.</p>'
            || '<h2>이런 분께 추천해요</h2>'
            || '<ol>'
            || '<li><strong>제주 여행 중 실내 일정이 필요한 분</strong>: 원도심 근처라 이동이 편하고, 날씨 영향을 안 받아요.</li>'
            || '<li><strong>재봉틀을 한 번 배워보고 싶었던 분</strong>: 기초부터 알려 주셔서 부담이 없어요.</li>'
            || '<li><strong>여행 기념품을 직접 만들고 싶은 분</strong>: 완성품을 그대로 가져갈 수 있어요.</li>'
            || '</ol>'
            || '<h2>아쉬웠던 점</h2>'
            || '<p>2시간 30분이 생각보다 빠듯해서, 지퍼를 다는 단계에서는 조금 서둘러야 했습니다. 손이 느린 편이라면 단순한 모양을 고르는 걸 추천드려요.</p>'
            || '<p>집에 와서 완성한 파우치에 충전기를 넣어 보니 크기가 딱 맞아서 뿌듯했습니다. <em>버려질 뻔한 천이 매일 쓰는 물건이 됐다</em>는 게 이 체험의 가장 큰 매력인 것 같아요.</p>',
            'https://images.unsplash.com/photo-1641320197434-6ae0ca235048?w=2000&q=85&auto=format&fit=crop',
            TIMESTAMP '2026-09-27 20:10:00'
        ),
        (
            3,
            9,
            'seed-voter@example.com',
            '바리스타 진로수업 2회차, 라떼아트 도전기',
            '<p>남세종종합청소년센터에서 하는 <strong>하반기 진로활동 바리스타</strong> 수업을 듣고 있어요. 매주 목요일 저녁에 총 10회 진행되는데, 오늘 2회차를 마치고 기억이 생생할 때 후기를 남겨 봅니다.</p>'
            || '<h2>수업 기본 정보</h2>'
            || '<ul>'
            || '<li><strong>장소</strong>: 남세종종합청소년센터 북카페</li>'
            || '<li><strong>일정</strong>: 매주 목요일 17:30~19:30, 총 10회</li>'
            || '<li><strong>비용</strong>: 참가비 무료, 재료비 100,000원</li>'
            || '<li><strong>대상</strong>: 세종 지역 청소년</li>'
            || '</ul>'
            || '<h2>1회차: 커피가 이렇게 복잡한 거였다니</h2>'
            || '<p>첫 시간에는 원두가 어떻게 만들어지는지, 로스팅 정도에 따라 맛이 어떻게 달라지는지 배웠습니다. 같은 원두를 다르게 내린 커피 세 잔을 비교해서 마셔 봤는데, 쓴맛만 있는 줄 알았던 커피에서 신맛과 단맛이 느껴져서 정말 신기했어요.</p>'
            || '<p>에스프레소 머신 사용법도 처음 배웠는데, 원두 가루를 담고 <strong>탬핑</strong>(꾹 눌러 다지는 것)을 얼마나 고르게 하느냐에 따라 추출 시간이 달라진다는 걸 직접 확인했습니다.</p>'
            || '<h2>2회차: 드디어 라떼아트</h2>'
            || '<p>오늘은 우유 스팀부터 라떼아트 기초까지 배웠어요. 스팀 피처를 기울여서 우유가 돌도록 만드는 게 핵심인데, 처음엔 거품이 너무 많이 생기거나 우유가 끓어 버려서 몇 번이나 다시 했습니다.</p>'
            || '<blockquote>선생님이 "우유가 회오리치는 소리가 나면 잘하고 있는 거야"라고 하셨는데, 그 소리를 처음 들었을 때 진짜 짜릿했어요.</blockquote>'
            || '<p>하트를 그리는 게 목표였는데 제 첫 작품은 하트라기보다는 동그라미에 가까웠어요. 그래도 마지막 잔에서는 아래쪽이 살짝 뾰족한 하트 모양이 나와서 사진을 엄청 찍었습니다.</p>'
            || '<h2>지금까지 느낀 점</h2>'
            || '<ol>'
            || '<li><strong>생각보다 체력이 필요해요.</strong> 2시간 내내 서서 하니까 끝나면 다리가 조금 아파요.</li>'
            || '<li><strong>메모하면서 듣는 게 좋아요.</strong> 추출 시간이나 우유 온도처럼 숫자로 기억해야 할 게 많아요.</li>'
            || '<li><strong>진로 고민에 진짜 도움이 돼요.</strong> 카페 일이 단순하지 않다는 걸 알게 되니까 오히려 더 해보고 싶어졌어요.</li>'
            || '</ol>'
            || '<p>남은 8번의 수업 동안 하트를 제대로 그리는 게 목표입니다. 다음엔 <em>로제타(나뭇잎 모양)</em>까지 배운다고 해서 벌써 기대돼요. 수업이 끝나면 완주 후기도 남길게요!</p>',
            'https://images.unsplash.com/photo-1547240089-566513e12c89?w=2000&q=85&auto=format&fit=crop',
            TIMESTAMP '2026-09-24 21:30:00'
        )
)
INSERT INTO post (
    id, member_id, pu_fev_id, title, content, thumbnail, created_at, updated_at, deleted_at
)
SELECT
    ('00000000-0000-7000-a034-' || LPAD(seed_post.post_no::TEXT, 12, '0'))::UUID,
    author.id,
    festival.id,
    seed_post.title,
    seed_post.content,
    seed_post.thumbnail,
    seed_post.written_at,
    seed_post.written_at,
    NULL
FROM seed_post
JOIN member author
  ON author.email = seed_post.author_email
 AND author.deleted_at IS NULL
JOIN festival_submission submission
  ON submission.id = ('00000000-0000-7000-9100-' || LPAD(seed_post.submission_no::TEXT, 12, '0'))::UUID
 AND submission.deleted_at IS NULL
JOIN festival
  ON festival.id = submission.festival_id
 AND festival.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM post existing
    WHERE existing.id = ('00000000-0000-7000-a034-' || LPAD(seed_post.post_no::TEXT, 12, '0'))::UUID
);
