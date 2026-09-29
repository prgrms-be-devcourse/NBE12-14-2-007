-- 후기 목록/상세, 댓글, 좋아요 화면 확인용 mock 데이터입니다. 실제 방문 후기가 아닙니다.
-- V22/V25/V26의 회원과 V25의 행사 제보를 재사용합니다. local 프로필에서만 실행됩니다.
-- 기본 seed 기준: 후기 18개, 댓글 36개, 후기 좋아요 45개.
-- 고정 후기 UUID와 중복 검사로 재실행 시 같은 데이터를 추가하지 않습니다.

WITH seed_post (post_no, submission_no, author_email, title, content) AS (
    VALUES
        (1, 2, 'seed-maker@example.com', '미디어아트는 천천히 볼수록 좋았어요',
         '<p>화면이 바뀔 때마다 작품의 분위기가 달라져서 한 공간에 오래 머물렀습니다. 빠르게 지나가기보다 의자에 앉아 한 장면을 끝까지 보는 편이 좋았어요.</p><p>실내 전시를 먼저 보고 야외 작품으로 이동하니 동선도 편했습니다.</p>'),
        (2, 2, 'seed-voter@example.com', '비 오는 날 다녀온 미술관 후기',
         '<p>비가 와서 실내 위주로 관람했는데 생각보다 볼거리가 많았습니다. 영상과 소리가 함께 나오는 작품이 특히 기억에 남아요.</p><p>야외 작품은 충분히 보지 못해서 다음에 다시 방문하려고 합니다.</p>'),
        (3, 3, 'seed-master@example.com', '처음 만든 재봉 소품이 제일 뿌듯해요',
         '<p>재봉틀을 처음 사용해서 긴장했지만 기본 동작부터 설명해 주셔서 따라갈 수 있었습니다. 자투리 원단을 고르는 과정도 재미있었어요.</p><p>완성한 소품을 직접 가져갈 수 있어 여행 기념으로 남기기 좋았습니다.</p>'),
        (4, 3, 'seed-maker@example.com', '친구와 함께한 업사이클링 체험',
         '<p>친구와 서로 다른 원단을 골라 같은 모양의 소품을 만들었습니다. 같은 설명을 듣고 만들어도 결과물이 달라서 비교하는 재미가 있었어요.</p><p>작업 중에는 손을 많이 쓰니 짐을 간단하게 챙기는 것을 추천합니다.</p>'),
        (5, 4, 'seed-rookie@example.com', '해가 진 뒤 더 예뻤던 미디어아트',
         '<p>밝을 때 주변을 둘러보고 어두워진 뒤 작품을 다시 보니 느낌이 완전히 달랐습니다. 빛이 건물에 겹쳐지는 장면이 가장 인상적이었어요.</p><p>인기 있는 위치는 사람이 몰려 잠깐 기다렸다가 관람했습니다.</p>'),
        (6, 4, 'seed-master@example.com', '야간 산책 코스로 괜찮았어요',
         '<p>산책하면서 작품을 하나씩 보는 방식으로 즐겼습니다. 사진도 좋지만 잠시 휴대폰을 내려놓고 전체 연출을 보는 시간이 더 기억에 남네요.</p><p>돌아갈 때는 사람이 한꺼번에 움직여서 조금 여유 있게 이동했습니다.</p>'),
        (7, 5, 'seed-voter@example.com', '가까이서 보고 더 놀란 극사실 작품',
         '<p>멀리서는 사진처럼 보였던 작품이 가까이 가면 붓질과 재료의 질감으로 다가왔습니다. 같은 작품을 거리별로 비교하며 보는 재미가 있었어요.</p><p>설명을 읽고 다시 작품을 보면 놓쳤던 부분을 찾을 수 있었습니다.</p>'),
        (8, 5, 'seed-rookie@example.com', '혼자 집중해서 보기 좋은 전시',
         '<p>혼자 방문해서 마음에 드는 작품 앞에 충분히 머물렀습니다. 전시 초반보다 후반에 더 오래 보게 되어 시간을 넉넉히 잡길 잘했다고 생각했어요.</p><p>관람을 마친 뒤 인상 깊었던 작품을 메모해 두었습니다.</p>'),
        (9, 6, 'seed-rookie@example.com', '저지마을 골목에서 보낸 느린 오후',
         '<p>작업실과 전시 공간을 오가며 마을을 천천히 걸었습니다. 큰 전시장과는 달리 공간마다 분위기가 달라서 둘러보는 재미가 있었어요.</p><p>걷는 구간이 있어 편한 신발을 신고 가길 잘했습니다.</p>'),
        (10, 6, 'seed-master@example.com', '제주 서쪽 여행에 더한 예술 체험',
         '<p>여행 일정 사이에 체험 시간을 넣었는데 예상보다 집중해서 참여했습니다. 직접 만든 결과물을 보니 단순히 구경할 때와는 다른 만족감이 있었어요.</p><p>다음 일정까지 이동 시간을 넉넉히 두면 더 편하게 즐길 수 있겠습니다.</p>'),
        (11, 7, 'seed-maker@example.com', '걷기 챌린지 덕분에 동네를 새로 봤어요',
         '<p>평소 지나치던 길도 걸음 수를 채우려고 천천히 걸으니 새롭게 보였습니다. 하루에 몰아서 걷기보다 짧게 나누어 참여하니 부담이 적었어요.</p><p>걷기를 마친 뒤 앱 기록을 확인하는 습관도 생겼습니다.</p>'),
        (12, 7, 'seed-voter@example.com', '퇴근 후 짧은 산책이 습관이 됐어요',
         '<p>퇴근하면 바로 집으로 가곤 했는데 이번에는 한 정거장 먼저 내려 걸었습니다. 기록이 쌓이는 것을 보니 다음 날도 조금 더 움직이게 되네요.</p><p>함께 참여하는 친구와 진행 상황을 공유하는 것도 도움이 됐습니다.</p>'),
        (13, 8, 'seed-maker@example.com', '기록을 보는 시선이 달라진 전시',
         '<p>시간과 기록이라는 주제를 자료와 함께 따라가니 전시 흐름을 이해하기 쉬웠습니다. 설명을 차근차근 읽느라 예상보다 오래 머물렀어요.</p><p>조용하게 관람하고 싶은 날 다시 방문하고 싶습니다.</p>'),
        (14, 8, 'seed-rookie@example.com', '아이와 이야기하며 둘러본 박물관',
         '<p>아이와 마음에 드는 전시물을 하나씩 고르고 이유를 이야기하며 관람했습니다. 모든 설명을 읽기보다 관심 있는 부분에 집중하니 끝까지 즐겁게 볼 수 있었어요.</p><p>관람 뒤에도 기억에 남는 장면을 함께 이야기했습니다.</p>'),
        (15, 9, 'seed-rookie@example.com', '커피 향으로 기억하는 첫 바리스타 체험',
         '<p>도구 이름부터 차근차근 배우고 직접 커피를 내려 봤습니다. 같은 재료라도 손의 움직임에 따라 결과가 달라지는 점이 신기했어요.</p><p>처음에는 어렵지만 설명을 듣고 반복하니 조금씩 익숙해졌습니다.</p>'),
        (16, 9, 'seed-maker@example.com', '직접 해보니 달랐던 바리스타 수업',
         '<p>평소 쉽게 보였던 과정도 직접 해보니 신경 쓸 부분이 많았습니다. 향과 맛을 비교하며 적어 보는 시간이 특히 유익했어요.</p><p>진로 체험을 고민하는 청소년에게 경험을 나누고 싶습니다.</p>'),
        (17, 1, 'seed-master@example.com', '전시 부스를 돌며 얻은 새로운 아이디어',
         '<p>관심 있는 분야의 부스를 먼저 정하고 둘러보니 이동하기 편했습니다. 담당자에게 궁금한 점을 직접 물어볼 수 있어 소개 자료만 읽을 때보다 이해가 잘됐어요.</p><p>다시 살펴보고 싶은 내용은 관람 중에 간단히 메모했습니다.</p>'),
        (18, 1, 'seed-voter@example.com', '처음 방문한 한상대회 관람 후기',
         '<p>처음이라 어디부터 볼지 고민했지만 안내를 확인하고 동선을 정하니 수월했습니다. 다양한 분야의 이야기를 한자리에서 접할 수 있었어요.</p><p>부스마다 머무는 시간이 달라 여유 있는 일정으로 방문하는 편이 좋겠습니다.</p>')
)
INSERT INTO post (
    id, member_id, pu_fev_id, title, content, thumbnail, created_at, updated_at, deleted_at
)
SELECT
    ('00000000-0000-7000-a030-' || LPAD(seed_post.post_no::TEXT, 12, '0'))::UUID,
    author.id,
    festival.id,
    seed_post.title,
    seed_post.content,
    NULL,
    CURRENT_TIMESTAMP - (19 - seed_post.post_no) * INTERVAL '1 day',
    CURRENT_TIMESTAMP - (19 - seed_post.post_no) * INTERVAL '1 day',
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
    WHERE existing.id = ('00000000-0000-7000-a030-' || LPAD(seed_post.post_no::TEXT, 12, '0'))::UUID
);

-- 후기별 댓글 수: 0, 1, 2, 3, 4, 2개를 세 번 반복합니다.
-- 삭제된 댓글도 중복 검사에 포함해 재실행 시 삭제한 댓글을 복원하지 않습니다.
WITH seed_comment (comment_no, author_email, content) AS (
    VALUES
        (1, 'seed-voter@example.com', '방문 동선을 자세히 적어 주셔서 도움이 됐어요. 일정 짤 때 참고하겠습니다.'),
        (2, 'seed-rookie@example.com', '저도 비슷한 부분이 기억에 남았어요. 천천히 둘러보는 게 좋더라고요.'),
        (3, 'seed-maker@example.com', '사진으로만 볼 때와 직접 참여할 때의 느낌이 다르겠네요. 후기 감사합니다.'),
        (4, 'seed-master@example.com', '여유 있게 시간을 잡으라는 팁에 공감합니다. 다음 방문 때도 참고할게요.')
), seed_post AS (
    SELECT post_no,
           CASE MOD(post_no - 1, 6)
               WHEN 5 THEN 2
               ELSE MOD(post_no - 1, 6)
           END AS comment_count
    FROM GENERATE_SERIES(1, 18) AS numbers(post_no)
)
INSERT INTO "comment" (
    post_id, member_id, content, created_at, updated_at, deleted_at
)
SELECT
    post.id,
    author.id,
    seed_comment.content,
    post.created_at + seed_comment.comment_no * INTERVAL '1 hour',
    post.created_at + seed_comment.comment_no * INTERVAL '1 hour',
    NULL
FROM seed_post
JOIN post
  ON post.id = ('00000000-0000-7000-a030-' || LPAD(seed_post.post_no::TEXT, 12, '0'))::UUID
 AND post.deleted_at IS NULL
JOIN seed_comment
  ON seed_comment.comment_no <= seed_post.comment_count
JOIN member author
  ON author.email = seed_comment.author_email
 AND author.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM "comment" existing
    WHERE existing.post_id = post.id
      AND existing.member_id = author.id
      AND existing.content = seed_comment.content
);

-- 후기별 좋아요 수: 5, 0, 3, 1, 4, 2개를 반복해 인기순과 최신순 결과가 달라집니다.
-- 본인 후기는 제외하고, 같은 회원이 같은 후기에 중복으로 좋아요를 누르지 않습니다.
-- 행사 좋아요와 구분되도록 src_id만 지정하고 festival_id는 NULL로 둡니다.
WITH seed_voter (email) AS (
    VALUES
        ('roomescape@example.com'),
        ('seed-rookie@example.com'),
        ('seed-maker@example.com'),
        ('seed-master@example.com'),
        ('seed-voter@example.com'),
        ('seed-warning-voter-01@example.com')
), seed_post AS (
    SELECT post_no,
           CASE MOD(post_no - 1, 6)
               WHEN 0 THEN 5
               WHEN 1 THEN 0
               WHEN 2 THEN 3
               WHEN 3 THEN 1
               WHEN 4 THEN 4
               ELSE 2
           END AS like_count
    FROM GENERATE_SERIES(1, 18) AS numbers(post_no)
), ranked_voter AS (
    SELECT post.id AS post_id,
           post.created_at AS post_created_at,
           voter.id AS member_id,
           seed_post.like_count,
           ROW_NUMBER() OVER (PARTITION BY post.id ORDER BY voter.email) AS voter_no
    FROM seed_post
    JOIN post
      ON post.id = ('00000000-0000-7000-a030-' || LPAD(seed_post.post_no::TEXT, 12, '0'))::UUID
     AND post.deleted_at IS NULL
    CROSS JOIN seed_voter
    JOIN member voter
      ON voter.email = seed_voter.email
     AND voter.deleted_at IS NULL
     AND voter.id <> post.member_id
)
INSERT INTO "like" (
    src_id, member_id, festival_id, created_at, updated_at
)
SELECT
    ranked_voter.post_id,
    ranked_voter.member_id,
    NULL,
    ranked_voter.post_created_at + ranked_voter.voter_no * INTERVAL '30 minutes',
    ranked_voter.post_created_at + ranked_voter.voter_no * INTERVAL '30 minutes'
FROM ranked_voter
WHERE ranked_voter.voter_no <= ranked_voter.like_count
  AND NOT EXISTS (
      SELECT 1
      FROM "like" existing
      WHERE existing.src_id = ranked_voter.post_id
        AND existing.member_id = ranked_voter.member_id
  );
