-- 활동 제한 회원의 기존 제보가 화면에 어떻게 표시되는지 확인하기 위한 테스트 데이터입니다.
-- 실제 운영 배포 전 반드시 제거합니다.

INSERT INTO member (
    id,
    email,
    password,
    role,
    nickname,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    '00000000-0000-7000-8200-000000000001'::UUID,
    'seed-warning@example.com',
    '$2a$10$3J0CLAJcpClv0aXxx62Ir.isAsHcL4iA8CG2ahEZz9vVp9tM4pWgu',
    'ROLE_WARNING',
    '길 잃은 참새',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM member existing
    WHERE existing.email = 'seed-warning@example.com'
      AND existing.deleted_at IS NULL
);

-- 한 회원은 한 행사에 한 번만 평가할 수 있으므로 부정확도 집계 확인용 회원을 추가합니다.
WITH warning_voter (id, email, nickname) AS (
    VALUES
        ('00000000-0000-7000-8300-000000000001'::UUID, 'seed-warning-voter-01@example.com', '부정확도 확인단 01'),
        ('00000000-0000-7000-8300-000000000002'::UUID, 'seed-warning-voter-02@example.com', '부정확도 확인단 02'),
        ('00000000-0000-7000-8300-000000000003'::UUID, 'seed-warning-voter-03@example.com', '부정확도 확인단 03'),
        ('00000000-0000-7000-8300-000000000004'::UUID, 'seed-warning-voter-04@example.com', '부정확도 확인단 04'),
        ('00000000-0000-7000-8300-000000000005'::UUID, 'seed-warning-voter-05@example.com', '부정확도 확인단 05'),
        ('00000000-0000-7000-8300-000000000006'::UUID, 'seed-warning-voter-06@example.com', '부정확도 확인단 06'),
        ('00000000-0000-7000-8300-000000000007'::UUID, 'seed-warning-voter-07@example.com', '부정확도 확인단 07'),
        ('00000000-0000-7000-8300-000000000008'::UUID, 'seed-warning-voter-08@example.com', '부정확도 확인단 08'),
        ('00000000-0000-7000-8300-000000000009'::UUID, 'seed-warning-voter-09@example.com', '부정확도 확인단 09'),
        ('00000000-0000-7000-8300-000000000010'::UUID, 'seed-warning-voter-10@example.com', '부정확도 확인단 10')
)
INSERT INTO member (
    id,
    email,
    password,
    role,
    nickname,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    warning_voter.id,
    warning_voter.email,
    '$2a$10$3J0CLAJcpClv0aXxx62Ir.isAsHcL4iA8CG2ahEZz9vVp9tM4pWgu',
    'ROLE_UNVERIFIED',
    warning_voter.nickname,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
FROM warning_voter
WHERE NOT EXISTS (
    SELECT 1
    FROM member existing
    WHERE existing.email = warning_voter.email
      AND existing.deleted_at IS NULL
);

-- 활동 제한 회원이 작성한 정확한 제보 1개와 허위 행사 제보 1개를 재현합니다.
WITH warning_festival (
    inst_nm,
    title,
    category,
    content,
    url,
    img_url,
    region,
    region_detail,
    begin_de,
    end_de,
    event_tm_info,
    partcpt_expn_info,
    telno_info,
    host_inst_nm
) AS (
    VALUES
        (
            '울산시립미술관',
            '2026 미디어아트 프로젝트 줄리안 오피',
            '전시',
            '줄리안 오피의 가상현실 작품과 대형 미디어 설치 작품을 실내외 공간에서 만나는 전시입니다.',
            'https://www.ulsan.go.kr/s/uam/bbs/view.ulsan?bbsId=BBS_0000000000000175&dataId=59156&mId=001003002000000000',
            NULL,
            'ULSAN',
            '울산광역시 중구 미술관길 72 울산시립미술관 XR랩·잔디마당·옥외 미디어스크린',
            '2026-07-02 10:00:00',
            '2026-10-05 20:00:00',
            '10:00~18:00, 수·금·토요일 20:00까지',
            '성인 1,000원, 울산시민 500원, 어린이·청소년·경로 무료',
            '052-229-8443',
            '울산시립미술관'
        ),
        (
            '호그와트 울산점',
            '2026 방구석 순간이동 축제',
            '기타',
            '순간이동을 꿈꾸는 자들아. 이리 오거라',
            'https://mugglenet.com/',
            '/images/2026-09-28_warning-festival-portal.jpg',
            'ULSAN',
            '울산광역시 호그와트 울산점',
            '2026-07-02 10:00:00',
            '2026-10-05 20:00:00',
            '운영 시간 확인 불가',
            '1억 2천(부가세별도)',
            '연락처 확인 불가',
            '불사조 기사단'
        )
)
INSERT INTO festival (
    member_id,
    provider_type,
    inst_nm,
    title,
    category,
    content,
    url,
    img_url,
    begin_de,
    end_de,
    event_tm_info,
    partcpt_expn_info,
    telno_info,
    host_inst_nm,
    writng_de,
    status,
    region,
    region_detail,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    author.id,
    'MEMBER',
    warning_festival.inst_nm,
    warning_festival.title,
    warning_festival.category,
    warning_festival.content,
    warning_festival.url,
    warning_festival.img_url,
    warning_festival.begin_de::TIMESTAMP,
    warning_festival.end_de::TIMESTAMP,
    warning_festival.event_tm_info,
    warning_festival.partcpt_expn_info,
    warning_festival.telno_info,
    warning_festival.host_inst_nm,
    CURRENT_TIMESTAMP,
    'OPEN',
    warning_festival.region,
    warning_festival.region_detail,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
FROM warning_festival
JOIN member author
  ON author.email = 'seed-warning@example.com'
 AND author.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM festival existing
    WHERE existing.url = warning_festival.url
      AND existing.member_id = author.id
      AND existing.deleted_at IS NULL
);

WITH warning_submission (submission_id, reference_url) AS (
    VALUES
        (
            '00000000-0000-7000-9200-000000000001'::UUID,
            'https://www.ulsan.go.kr/s/uam/bbs/view.ulsan?bbsId=BBS_0000000000000175&dataId=59156&mId=001003002000000000'
        ),
        (
            '00000000-0000-7000-9200-000000000002'::UUID,
            'https://mugglenet.com/'
        )
)
INSERT INTO festival_submission (
    id,
    festival_id,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    warning_submission.submission_id,
    festival.id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
FROM warning_submission
JOIN festival
  ON festival.url = warning_submission.reference_url
 AND festival.member_id = '00000000-0000-7000-8200-000000000001'::UUID
 AND festival.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM festival_submission existing
    WHERE existing.id = warning_submission.submission_id
       OR (existing.festival_id = festival.id AND existing.deleted_at IS NULL)
);

-- 정확한 제보에는 '정확해요' 5표, 허위 행사 제보에는 '부정확해요' 15표를 추가합니다.
WITH seed_vote (reference_url, voter_email, vote_type) AS (
    VALUES
        ('https://www.ulsan.go.kr/s/uam/bbs/view.ulsan?bbsId=BBS_0000000000000175&dataId=59156&mId=001003002000000000', 'roomescape@example.com', 'ACCURATE'),
        ('https://www.ulsan.go.kr/s/uam/bbs/view.ulsan?bbsId=BBS_0000000000000175&dataId=59156&mId=001003002000000000', 'seed-rookie@example.com', 'ACCURATE'),
        ('https://www.ulsan.go.kr/s/uam/bbs/view.ulsan?bbsId=BBS_0000000000000175&dataId=59156&mId=001003002000000000', 'seed-maker@example.com', 'ACCURATE'),
        ('https://www.ulsan.go.kr/s/uam/bbs/view.ulsan?bbsId=BBS_0000000000000175&dataId=59156&mId=001003002000000000', 'seed-master@example.com', 'ACCURATE'),
        ('https://www.ulsan.go.kr/s/uam/bbs/view.ulsan?bbsId=BBS_0000000000000175&dataId=59156&mId=001003002000000000', 'seed-voter@example.com', 'ACCURATE'),
        ('https://mugglenet.com/', 'roomescape@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-rookie@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-maker@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-master@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-voter@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-warning-voter-01@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-warning-voter-02@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-warning-voter-03@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-warning-voter-04@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-warning-voter-05@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-warning-voter-06@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-warning-voter-07@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-warning-voter-08@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-warning-voter-09@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-warning-voter-10@example.com', 'INACCURATE')
)
INSERT INTO festival_accuracy_vote (
    festival_id,
    member_id,
    vote_type,
    created_at,
    updated_at
)
SELECT
    festival.id,
    voter.id,
    seed_vote.vote_type,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM seed_vote
JOIN member voter
  ON voter.email = seed_vote.voter_email
 AND voter.deleted_at IS NULL
JOIN festival
  ON festival.url = seed_vote.reference_url
 AND festival.member_id = '00000000-0000-7000-8200-000000000001'::UUID
 AND festival.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM festival_accuracy_vote existing
    WHERE existing.festival_id = festival.id
      AND existing.member_id = voter.id
);
