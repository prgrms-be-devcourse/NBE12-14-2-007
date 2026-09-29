-- 배포 화면과 발표 시연에 사용할 행사 제보 데이터입니다.
-- 관리자 계정은 만들지 않으며, 신뢰 등급과 활동 제한 표시를 확인할 수 있도록 구성합니다.
-- seed-voter@example.com / 123456a! 계정으로 허위 행사에 10번째 '부정확해요' 평가를 시연할 수 있습니다.

-- 발표자가 사용할 평가 계정만 123456a!로 로그인할 수 있고, 나머지 계정은 로그인할 수 없게 둡니다.
WITH seed_member (id, email, role, nickname) AS (
    VALUES
        ('00000000-0000-7000-8100-000000000001'::UUID, 'seed-rookie@example.com', 'ROLE_UNVERIFIED', '주말 탈출 꿈나무'),
        ('00000000-0000-7000-8100-000000000002'::UUID, 'seed-maker@example.com', 'ROLE_RECOGNIZED', '동네 참새'),
        ('00000000-0000-7000-8100-000000000003'::UUID, 'seed-master@example.com', 'ROLE_TRUSTED', '팔도 철새'),
        ('00000000-0000-7000-8100-000000000004'::UUID, 'seed-voter@example.com', 'ROLE_UNVERIFIED', '행사 탐험가')
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
    seed_member.id,
    seed_member.email,
    CASE
        WHEN seed_member.email = 'seed-voter@example.com'
            THEN '$2a$10$3J0CLAJcpClv0aXxx62Ir.isAsHcL4iA8CG2ahEZz9vVp9tM4pWgu'
        ELSE '$2a$10$3J0CLAJcpClv0aXxx62Ir.isAsHcL4iA8CG2ahEZz9vVp9tM4pWge'
    END,
    seed_member.role,
    seed_member.nickname,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
FROM seed_member
WHERE NOT EXISTS (
    SELECT 1
    FROM member existing
    WHERE existing.email = seed_member.email
      AND existing.deleted_at IS NULL
);

-- 탈출 꿈나무, 탈출 메이커, 탈출 마스터 회원에게 행사 제보를 각각 3개씩 등록합니다.
WITH seed_festival (
    author_email,
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
            'seed-rookie@example.com',
            '재외동포청',
            '제24차 세계한상대회',
            '기타',
            '전 세계 한인 경제인과 국내 기업이 전시, 비즈니스 상담과 교류 프로그램에 참여하는 행사입니다.',
            'https://www.incheon.go.kr/eco/ECO050101/view?curPage=&nttNo=2046171&srchKey=&srchWord=',
            NULL,
            'INCHEON',
            '인천광역시 연수구 송도컨벤시아',
            '2026-09-28 00:00:00',
            '2026-09-30 23:59:59',
            '프로그램별 상이',
            '현장 등록 가능',
            '032-440-1536',
            '세계한상위원회·재외동포청'
        ),
        (
            'seed-rookie@example.com',
            '울산시립미술관',
            '2026 미디어아트 프로젝트 줄리안 오피',
            '전시',
            '줄리안 오피의 가상현실 작품과 대형 미디어 설치 작품을 실내외 공간에서 만나는 전시입니다.',
            'https://www.ulsan.go.kr/s/uam/bbs/view.ulsan?bbsId=BBS_0000000000000174&dataId=58565&mId=001003001000000000',
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
            'seed-rookie@example.com',
            '제주더큰내일센터',
            '탐나는 마켓 X 섬섬옥수 업사이클링 초보 재봉 워크숍',
            '체험',
            '폐원단과 재봉틀을 활용해 생활 소품을 만들어 보는 소규모 업사이클링 체험입니다.',
            'https://m.visitjeju.net/kr/festival/view?contentsid=CNTS_300000000014628&menuId=DOM_000001718007000000',
            'https://api.cdn.visitjeju.net/photomng/thumbnailpath/202607/20/b19d3841-875a-4795-8ad8-12d5a3e49217.webp',
            'JEJU',
            '제주특별자치도 제주시 관덕로11길 34 제주더큰내일센터 1층',
            '2026-07-19 13:00:00',
            '2026-09-28 15:30:00',
            '매주 일·월요일 13:00~15:30',
            '사전 예약 필요',
            '010-8830-2739',
            '제주더큰내일센터'
        ),
        (
            'seed-maker@example.com',
            '인천광역시',
            '국가유산 미디어아트',
            '축제',
            '인천의 국가유산을 빛과 영상으로 새롭게 만나는 야간 미디어아트 행사입니다.',
            'https://itour.incheon.go.kr/ssst/ssst/list.do?fstv_month=09&pageNm=fstv',
            NULL,
            'INCHEON',
            '인천광역시 일원',
            '2026-08-14 00:00:00',
            '2026-11-08 23:59:59',
            NULL,
            NULL,
            '02-3011-7781',
            '인천광역시'
        ),
        (
            'seed-maker@example.com',
            '울산시립미술관',
            '국제 하이퍼-리얼리즘 가장 완벽한 환영',
            '전시',
            '국내외 극사실주의 작가들의 회화와 조각 작품을 한자리에서 소개하는 국제 전시입니다.',
            'https://ulsan.go.kr/s/uam/bbs/view.do?bbsId=BBS_0000000000000188&dataId=58173&mId=001007002001000000',
            NULL,
            'ULSAN',
            '울산광역시 중구 미술관길 72 울산시립미술관 1전시실',
            '2026-07-02 10:00:00',
            '2026-10-05 20:00:00',
            '평일 10:00~18:00, 수·금·토요일 20:00까지',
            '성인 15,000원',
            '052-211-3800',
            '울산시립미술관'
        ),
        (
            'seed-maker@example.com',
            '제주문화예술재단',
            '저지문화예술인마을 제주 서쪽, 저지에서 놀자!',
            '체험',
            '저지문화예술인마을의 작업실과 전시 공간을 둘러보고 예술 창작을 체험하는 프로그램입니다.',
            'https://m.visitjeju.net/kr/festival/view?contentsid=CNTS_300000000014322&menuId=DOM_000001718007000000',
            'https://api.cdn.visitjeju.net/photomng/thumbnailpath/202605/26/21258019-ae8a-4bde-b554-8b3ca7e72778.webp',
            'JEJU',
            '제주특별자치도 제주시 한경면 저지14길 13',
            '2026-05-13 10:00:00',
            '2026-09-30 16:00:00',
            '프로그램별 상이',
            '무료, 프로그램별 선착순 15명',
            '010-6701-6835',
            '제주문화예술재단'
        ),
        (
            'seed-master@example.com',
            '인천광역시자살예방센터',
            '생명을 잇는 걸음 워크온 챌린지',
            '체험',
            '워크온 앱을 이용해 한 달 동안 10만 보 걷기에 도전하는 인천 시민 참여 행사입니다.',
            'https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=',
            NULL,
            'INCHEON',
            '인천광역시 전역(워크온 앱 참여)',
            '2026-09-01 00:00:00',
            '2026-09-30 23:59:59',
            '기간 중 자유 참여',
            '무료',
            '032-468-9917',
            '인천광역시자살예방센터'
        ),
        (
            'seed-master@example.com',
            '울산암각화박물관',
            '시간저장소, 그날의 데이터',
            '전시',
            '울산암각화박물관에서 시간과 기록을 주제로 선보이는 2026년 특별기획전입니다.',
            'https://www.ulsan.go.kr/s/bangudae/contents.ulsan?mId=001001004000000000',
            NULL,
            'ULSAN',
            '울산광역시 울주군 두동면 반구대안길 254 울산암각화박물관',
            '2026-07-07 09:00:00',
            '2027-04-25 18:00:00',
            '박물관 운영시간 내 관람',
            NULL,
            '052-229-4797',
            '울산암각화박물관'
        ),
        (
            'seed-master@example.com',
            '남세종종합청소년센터',
            '2026년 하반기 진로활동 바리스타',
            '교육',
            '세종 지역 청소년이 바리스타 직무를 체험하며 진로 역량을 기르는 하반기 교육 프로그램입니다.',
            'https://www2.sejong.go.kr/youth/prog/progrm/kor/sub03_02/view.do?progrmNo=2051&tmeNo=1',
            NULL,
            'SEJONG',
            '세종특별자치시 남세종종합청소년센터 북카페',
            '2026-09-17 17:30:00',
            '2026-12-03 19:30:00',
            '매주 목요일 17:30~19:30, 총 10회',
            '참가비 무료, 재료비 100,000원',
            '044-300-4982',
            '남세종종합청소년센터'
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
    seed_festival.inst_nm,
    seed_festival.title,
    seed_festival.category,
    seed_festival.content,
    seed_festival.url,
    seed_festival.img_url,
    seed_festival.begin_de::TIMESTAMP,
    seed_festival.end_de::TIMESTAMP,
    seed_festival.event_tm_info,
    seed_festival.partcpt_expn_info,
    seed_festival.telno_info,
    seed_festival.host_inst_nm,
    CURRENT_TIMESTAMP,
    CASE
        WHEN seed_festival.end_de::TIMESTAMP < CURRENT_TIMESTAMP THEN 'CLOSED'
        ELSE 'OPEN'
    END,
    seed_festival.region,
    seed_festival.region_detail,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
FROM seed_festival
JOIN member author
  ON author.email = seed_festival.author_email
 AND author.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM festival existing
    WHERE existing.url = seed_festival.url
      AND existing.member_id = author.id
      AND existing.deleted_at IS NULL
);

WITH seed_submission (submission_id, reference_url) AS (
    VALUES
        ('00000000-0000-7000-9100-000000000001'::UUID, 'https://www.incheon.go.kr/eco/ECO050101/view?curPage=&nttNo=2046171&srchKey=&srchWord='),
        ('00000000-0000-7000-9100-000000000002'::UUID, 'https://www.ulsan.go.kr/s/uam/bbs/view.ulsan?bbsId=BBS_0000000000000174&dataId=58565&mId=001003001000000000'),
        ('00000000-0000-7000-9100-000000000003'::UUID, 'https://m.visitjeju.net/kr/festival/view?contentsid=CNTS_300000000014628&menuId=DOM_000001718007000000'),
        ('00000000-0000-7000-9100-000000000004'::UUID, 'https://itour.incheon.go.kr/ssst/ssst/list.do?fstv_month=09&pageNm=fstv'),
        ('00000000-0000-7000-9100-000000000005'::UUID, 'https://ulsan.go.kr/s/uam/bbs/view.do?bbsId=BBS_0000000000000188&dataId=58173&mId=001007002001000000'),
        ('00000000-0000-7000-9100-000000000006'::UUID, 'https://m.visitjeju.net/kr/festival/view?contentsid=CNTS_300000000014322&menuId=DOM_000001718007000000'),
        ('00000000-0000-7000-9100-000000000007'::UUID, 'https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord='),
        ('00000000-0000-7000-9100-000000000008'::UUID, 'https://www.ulsan.go.kr/s/bangudae/contents.ulsan?mId=001001004000000000'),
        ('00000000-0000-7000-9100-000000000009'::UUID, 'https://www2.sejong.go.kr/youth/prog/progrm/kor/sub03_02/view.do?progrmNo=2051&tmeNo=1')
)
INSERT INTO festival_submission (
    id,
    festival_id,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    seed_submission.submission_id,
    festival.id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
FROM seed_submission
JOIN festival
  ON festival.url = seed_submission.reference_url
 AND festival.provider_type = 'MEMBER'
 AND festival.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM festival_submission existing
    WHERE existing.id = seed_submission.submission_id
       OR (existing.festival_id = festival.id AND existing.deleted_at IS NULL)
);


-- 탈출 메이커는 자신이 작성한 행사에서 받은 좋아요 합계가 10개가 되도록 구성합니다.
WITH seed_like (reference_url, voter_email) AS (
    VALUES
        ('https://itour.incheon.go.kr/ssst/ssst/list.do?fstv_month=09&pageNm=fstv', 'seed-rookie@example.com'),
        ('https://itour.incheon.go.kr/ssst/ssst/list.do?fstv_month=09&pageNm=fstv', 'seed-master@example.com'),
        ('https://itour.incheon.go.kr/ssst/ssst/list.do?fstv_month=09&pageNm=fstv', 'seed-voter@example.com'),
        ('https://ulsan.go.kr/s/uam/bbs/view.do?bbsId=BBS_0000000000000188&dataId=58173&mId=001007002001000000', 'seed-rookie@example.com'),
        ('https://ulsan.go.kr/s/uam/bbs/view.do?bbsId=BBS_0000000000000188&dataId=58173&mId=001007002001000000', 'seed-master@example.com'),
        ('https://m.visitjeju.net/kr/festival/view?contentsid=CNTS_300000000014322&menuId=DOM_000001718007000000', 'seed-rookie@example.com'),
        ('https://m.visitjeju.net/kr/festival/view?contentsid=CNTS_300000000014322&menuId=DOM_000001718007000000', 'seed-master@example.com'),
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'seed-rookie@example.com'),
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'seed-maker@example.com'),
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'seed-voter@example.com'),
        ('https://www.ulsan.go.kr/s/bangudae/contents.ulsan?mId=001001004000000000', 'seed-rookie@example.com'),
        ('https://www.ulsan.go.kr/s/bangudae/contents.ulsan?mId=001001004000000000', 'seed-maker@example.com'),
        ('https://www2.sejong.go.kr/youth/prog/progrm/kor/sub03_02/view.do?progrmNo=2051&tmeNo=1', 'seed-rookie@example.com'),
        ('https://www2.sejong.go.kr/youth/prog/progrm/kor/sub03_02/view.do?progrmNo=2051&tmeNo=1', 'seed-maker@example.com')
)
INSERT INTO "like" (
    src_id,
    member_id,
    festival_id,
    created_at,
    updated_at
)
SELECT
    NULL,
    voter.id,
    festival.id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM seed_like
JOIN member voter
  ON voter.email = seed_like.voter_email
 AND voter.deleted_at IS NULL
JOIN festival
  ON festival.url = seed_like.reference_url
 AND festival.provider_type = 'MEMBER'
 AND festival.deleted_at IS NULL
WHERE voter.id <> festival.member_id
  AND NOT EXISTS (
      SELECT 1
      FROM "like" existing
      WHERE existing.festival_id = festival.id
        AND existing.member_id = voter.id
  );

-- 탈출 마스터는 좋아요 10개와 '정확해요' 10개를 모두 충족하도록 구성합니다.
WITH seed_accuracy_vote (reference_url, voter_email) AS (
    VALUES
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'seed-rookie@example.com'),
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'seed-maker@example.com'),
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'seed-voter@example.com'),
        ('https://www.ulsan.go.kr/s/bangudae/contents.ulsan?mId=001001004000000000', 'seed-rookie@example.com'),
        ('https://www.ulsan.go.kr/s/bangudae/contents.ulsan?mId=001001004000000000', 'seed-maker@example.com'),
        ('https://www2.sejong.go.kr/youth/prog/progrm/kor/sub03_02/view.do?progrmNo=2051&tmeNo=1', 'seed-rookie@example.com'),
        ('https://www2.sejong.go.kr/youth/prog/progrm/kor/sub03_02/view.do?progrmNo=2051&tmeNo=1', 'seed-maker@example.com')
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
    'ACCURATE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM seed_accuracy_vote
JOIN member voter
  ON voter.email = seed_accuracy_vote.voter_email
 AND voter.deleted_at IS NULL
JOIN festival
  ON festival.url = seed_accuracy_vote.reference_url
 AND festival.provider_type = 'MEMBER'
 AND festival.deleted_at IS NULL
WHERE voter.id <> festival.member_id
  AND NOT EXISTS (
      SELECT 1
      FROM festival_accuracy_vote existing
      WHERE existing.festival_id = festival.id
        AND existing.member_id = voter.id
  );

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
    '$2a$10$3J0CLAJcpClv0aXxx62Ir.isAsHcL4iA8CG2ahEZz9vVp9tM4pWge',
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
    '$2a$10$3J0CLAJcpClv0aXxx62Ir.isAsHcL4iA8CG2ahEZz9vVp9tM4pWge',
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

-- 정확한 제보에는 '정확해요' 4표를, 허위 행사 제보에는 시연 직전 상태인 '부정확해요' 9표를 추가합니다.
-- 로컬 seed가 먼저 실행된 경우에도 발표 시연 기준인 9표로 맞춥니다.
DELETE FROM festival_accuracy_vote
WHERE vote_type = 'INACCURATE'
  AND festival_id IN (
      SELECT id
      FROM festival
      WHERE url = 'https://mugglenet.com/'
        AND member_id = '00000000-0000-7000-8200-000000000001'::UUID
        AND deleted_at IS NULL
  );

WITH seed_vote (reference_url, voter_email, vote_type) AS (
    VALUES
        ('https://www.ulsan.go.kr/s/uam/bbs/view.ulsan?bbsId=BBS_0000000000000175&dataId=59156&mId=001003002000000000', 'seed-rookie@example.com', 'ACCURATE'),
        ('https://www.ulsan.go.kr/s/uam/bbs/view.ulsan?bbsId=BBS_0000000000000175&dataId=59156&mId=001003002000000000', 'seed-maker@example.com', 'ACCURATE'),
        ('https://www.ulsan.go.kr/s/uam/bbs/view.ulsan?bbsId=BBS_0000000000000175&dataId=59156&mId=001003002000000000', 'seed-master@example.com', 'ACCURATE'),
        ('https://www.ulsan.go.kr/s/uam/bbs/view.ulsan?bbsId=BBS_0000000000000175&dataId=59156&mId=001003002000000000', 'seed-voter@example.com', 'ACCURATE'),
        ('https://mugglenet.com/', 'seed-rookie@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-maker@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-master@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-warning-voter-01@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-warning-voter-02@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-warning-voter-03@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-warning-voter-04@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-warning-voter-05@example.com', 'INACCURATE'),
        ('https://mugglenet.com/', 'seed-warning-voter-06@example.com', 'INACCURATE')
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

-- 운영팀 관리자 계정 없이도 메이커·마스터 조건이 각각 10개를 충족하도록 보완합니다.
WITH supplemental_like (reference_url, voter_email) AS (
    VALUES
        ('https://itour.incheon.go.kr/ssst/ssst/list.do?fstv_month=09&pageNm=fstv', 'seed-warning-voter-01@example.com'),
        ('https://itour.incheon.go.kr/ssst/ssst/list.do?fstv_month=09&pageNm=fstv', 'seed-warning-voter-02@example.com'),
        ('https://itour.incheon.go.kr/ssst/ssst/list.do?fstv_month=09&pageNm=fstv', 'seed-warning-voter-03@example.com'),
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'seed-warning-voter-01@example.com'),
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'seed-warning-voter-02@example.com'),
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'seed-warning-voter-03@example.com')
)
INSERT INTO "like" (
    src_id,
    member_id,
    festival_id,
    created_at,
    updated_at
)
SELECT
    NULL,
    voter.id,
    festival.id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM supplemental_like
JOIN member voter
  ON voter.email = supplemental_like.voter_email
 AND voter.deleted_at IS NULL
JOIN festival
  ON festival.url = supplemental_like.reference_url
 AND festival.provider_type = 'MEMBER'
 AND festival.deleted_at IS NULL
WHERE voter.id <> festival.member_id
  AND (
      SELECT COUNT(*)
      FROM "like" existing_count
      JOIN festival authored_festival
        ON authored_festival.id = existing_count.festival_id
       AND authored_festival.deleted_at IS NULL
      WHERE authored_festival.member_id = festival.member_id
  ) < 10
  AND NOT EXISTS (
      SELECT 1
      FROM "like" existing
      WHERE existing.festival_id = festival.id
        AND existing.member_id = voter.id
  );

WITH supplemental_accuracy_vote (reference_url, voter_email) AS (
    VALUES
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'seed-warning-voter-01@example.com'),
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'seed-warning-voter-02@example.com'),
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'seed-warning-voter-03@example.com')
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
    'ACCURATE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM supplemental_accuracy_vote
JOIN member voter
  ON voter.email = supplemental_accuracy_vote.voter_email
 AND voter.deleted_at IS NULL
JOIN festival
  ON festival.url = supplemental_accuracy_vote.reference_url
 AND festival.provider_type = 'MEMBER'
 AND festival.deleted_at IS NULL
WHERE voter.id <> festival.member_id
  AND (
      SELECT COUNT(*)
      FROM festival_accuracy_vote existing_count
      JOIN festival authored_festival
        ON authored_festival.id = existing_count.festival_id
       AND authored_festival.deleted_at IS NULL
      WHERE authored_festival.member_id = festival.member_id
        AND existing_count.vote_type = 'ACCURATE'
  ) < 10
  AND NOT EXISTS (
      SELECT 1
      FROM festival_accuracy_vote existing
      WHERE existing.festival_id = festival.id
        AND existing.member_id = voter.id
  );

-- 행사 제보 초기 데이터입니다.
-- 20개 행사를 꿈나무 2명, 메이커, 마스터 작성자에게 나누어 배정합니다.

WITH seed_member (id, email, role, nickname) AS (
    VALUES
        ('00000000-0000-7000-8100-000000000001'::UUID, 'seed-rookie@example.com', 'ROLE_UNVERIFIED', '주말 탈출 꿈나무'),
        ('00000000-0000-7000-8400-000000000001'::UUID, 'seed-rookie-2@example.com', 'ROLE_UNVERIFIED', '동네 탐험 꿈나무'),
        ('00000000-0000-7000-8400-000000000002'::UUID, 'seed-rookie-3@example.com', 'ROLE_UNVERIFIED', '나들이 준비 꿈나무'),
        ('00000000-0000-7000-8100-000000000002'::UUID, 'seed-maker@example.com', 'ROLE_UNVERIFIED', '동네 참새'),
        ('00000000-0000-7000-8100-000000000003'::UUID, 'seed-master@example.com', 'ROLE_UNVERIFIED', '팔도 철새')
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
    seed_member.id,
    seed_member.email,
    '$2a$10$3J0CLAJcpClv0aXxx62Ir.isAsHcL4iA8CG2ahEZz9vVp9tM4pWge',
    seed_member.role,
    seed_member.nickname,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
FROM seed_member
WHERE NOT EXISTS (
    SELECT 1
    FROM member existing
    WHERE existing.email = seed_member.email
      AND existing.deleted_at IS NULL
);

WITH seed (
    inst_nm,
    title,
    category,
    content,
    url,
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
            '하동북천코스모스메밀꽃영농조합법인',
            '제20회 하동 북천 코스모스·메밀꽃 축제',
            '축제',
            '코스모스와 메밀꽃이 펼쳐진 꽃단지에서 가을 풍경과 체험 프로그램을 즐기는 축제입니다.',
            'https://korean.visitkorea.or.kr/kfes/detail/fstvlDetail.do?fstvlCntntsId=3d76c452-c3c6-4959-94e9-07200623c75d',
            'GYEONGNAM',
            '경상남도 하동군 북천면 직전·이명마을 꽃단지 일원',
            '2026-09-22 00:00:00',
            '2026-10-05 23:59:59',
            NULL,
            NULL,
            NULL,
            '북천면 꽃단지 행사추진위원회'
        ),
        (
            '한국정신문화재단',
            '2026 안동국제탈춤페스티벌',
            '축제',
            '국내외 탈춤 공연과 전시, 체험을 만날 수 있는 안동의 대표 문화축제입니다.',
            'https://korean.visitkorea.or.kr/kfes/detail/fstvlDetail.do?Flag=Y&cmsCntntsId=506670',
            'GYEONGBUK',
            '경상북도 안동시 (구)안동역사부지 및 탈춤공원 일원',
            '2026-09-24 00:00:00',
            '2026-10-04 23:59:59',
            NULL,
            NULL,
            NULL,
            '안동시'
        ),
        (
            '예천문화관광재단',
            '2026 예천 삼강나루 주막축제',
            '축제',
            '삼강나루의 역사와 주막 문화를 바탕으로 뱃사공·보부상 체험과 공연을 즐기는 축제입니다.',
            'https://www.ycg.kr/open.content/samgang/notification/event/?i=185296',
            'GYEONGBUK',
            '경상북도 예천군 풍양면 삼강문화단지 일원',
            '2026-09-25 11:00:00',
            '2026-09-27 18:00:00',
            '11:00~18:00',
            NULL,
            '054-650-6802',
            '예천군'
        ),
        (
            '부산관광공사',
            '2026 별바다부산 나이트 마켓 - 골든 어텀 시즌',
            '플리마켓',
            '지역 먹거리와 전통주, 소상공인 마켓, 공연과 체험을 함께 즐기는 야간 행사입니다.',
            'https://www.busan.go.kr/nbtnewsBU/1755302',
            'BUSAN',
            '부산광역시 북구 화명생태공원 연꽃단지 일원',
            '2026-09-26 00:00:00',
            '2026-10-18 23:59:59',
            '매주 목·금·토·일요일(첫째 주는 토·일요일)',
            NULL,
            '051-888-5212',
            '부산광역시'
        ),
        (
            '영광불갑산상사화축제 추진위원회',
            '제26회 영광불갑산상사화축제',
            '축제',
            '불갑산과 불갑사 관광지구의 상사화 군락을 중심으로 공연과 체험을 즐기는 가을 축제입니다.',
            'https://www.mcst.go.kr/site/s_culture/festival/festivalView.jsp?pRo=9&pSeq=11596',
            'JEONNAM',
            '전라남도 영광군 불갑면 불갑사 관광지구 일원',
            '2026-09-18 10:00:00',
            '2026-09-27 21:00:00',
            '10:00~21:00',
            '무료',
            '061-350-5269',
            '영광군'
        ),
        (
            '김제시지평선축제제전위원회',
            '제28회 김제지평선축제',
            '축제',
            '김제의 농경문화를 공연과 전통 민속행사, 가족 체험으로 만나는 문화관광축제입니다.',
            'https://festival.gimje.go.kr/content/content200.do',
            'JEONBUK',
            '전북특별자치도 김제시 일원(벽골제 중심)',
            '2026-10-01 00:00:00',
            '2026-10-05 23:59:59',
            NULL,
            NULL,
            '063-540-3032',
            '김제시지평선축제제전위원회'
        ),
        (
            '광주동구문화관광재단',
            '제23회 광주 추억의 충장축제',
            '축제',
            '추억을 주제로 세대가 함께 즐기는 공연과 거리 퍼레이드가 펼쳐지는 도심 축제입니다.',
            'https://www.recollection.kr/',
            'GWANGJU',
            '광주광역시 동구 금남로·충장로·5·18민주광장 일원',
            '2026-10-07 00:00:00',
            '2026-10-11 23:59:59',
            '프로그램별 상이',
            '무료',
            '062-608-4672',
            '광주광역시 동구'
        ),
        (
            '곡성군',
            '제26회 곡성심청어린이대축제',
            '축제',
            '가을 장미가 피는 섬진강기차마을에서 어린이와 가족이 공연과 놀이를 즐기는 축제입니다.',
            'https://www.gokseong.go.kr/tour/festivity/simcheong/',
            'JEONNAM',
            '전라남도 곡성군 오곡면 기차마을로 232 섬진강기차마을',
            '2026-10-08 00:00:00',
            '2026-10-11 23:59:59',
            NULL,
            NULL,
            NULL,
            '곡성군'
        ),
        (
            '계룡시문화관광재단',
            '2026 계룡軍문화축제',
            '축제',
            '군악·의장 공연과 퍼레이드, 군 장비와 병영 체험을 만나는 국방문화축제입니다.',
            'https://www.mcst.go.kr/site/s_culture/festival/festivalView.jsp?pRo=79&pSeq=11693',
            'CHUNGNAM',
            '충청남도 계룡시 계룡대 활주로 일원',
            '2026-10-01 09:00:00',
            '2026-10-05 17:00:00',
            '09:00~17:00(프로그램별 상이)',
            '무료(일부 체험 유료)',
            '042-840-2621~4',
            '계룡시'
        ),
        (
            '천안문화재단',
            '천안흥타령춤축제 2026',
            '축제',
            '국내외 춤 경연과 거리 댄스 퍼레이드를 중심으로 열리는 도심형 춤 축제입니다.',
            'https://www.mcst.go.kr/site/s_culture/festival/festivalView.jsp?pRo=4&pSeq=11672',
            'CHUNGNAM',
            '충청남도 천안시 천안종합운동장 및 천안삼거리공원',
            '2026-10-01 10:00:00',
            '2026-10-05 22:00:00',
            '10:00~22:00',
            '무료',
            '041-900-7021',
            '천안문화재단'
        ),
        (
            '금산문화관광재단',
            '제44회 금산세계인삼축제',
            '축제',
            '금산 인삼과 약초를 주제로 먹거리, 전시와 체험을 즐기는 지역 대표 축제입니다.',
            'https://www.insamfestival.co.kr/',
            'CHUNGNAM',
            '충청남도 금산군 금산읍 인삼광장로 30 금산세계인삼엑스포광장 및 인삼약초거리',
            '2026-10-02 00:00:00',
            '2026-10-11 23:59:59',
            NULL,
            NULL,
            '041-750-2319',
            '금산군'
        ),
        (
            '백제문화재단',
            '제72회 백제문화제',
            '축제',
            '사비백제의 역사 공간에서 제례, 행렬, 공연, 전시와 야간경관을 체험하는 역사문화축제입니다.',
            'https://www.baekjecf.or.kr/event/01.php',
            'CHUNGNAM',
            '충청남도 부여군 정림사지·석탑로·관북리유적 등 시가지 일원',
            '2026-10-03 00:00:00',
            '2026-10-11 23:59:59',
            NULL,
            NULL,
            '041-835-2721',
            '부여군'
        ),
        (
            '인제군문화재단',
            '2026 인제가을꽃축제',
            '축제',
            '인제 용대관광지에 조성된 가을꽃 정원과 다양한 체험을 즐기는 계절 축제입니다.',
            'https://korean.visitkorea.or.kr/kfes/detail/fstvlDetail.do?fstvlCntntsId=813eddb1-c961-4572-95b5-490dd8f54804',
            'GANGWON',
            '강원특별자치도 인제군 북면 용대리 용대관광지 일원',
            '2026-09-24 00:00:00',
            '2026-10-11 23:59:59',
            NULL,
            '무료(일부 프로그램 유료)',
            '033-460-8900',
            '인제군'
        ),
        (
            '정선아리랑문화재단',
            '제51회 정선아리랑제',
            '축제',
            '정선아리랑의 문화적 가치를 공연, 퍼레이드, 경연과 체험으로 이어가는 전통문화축제입니다.',
            'https://jacf.or.kr/jacf/pageview.php?keyvalue=sub02&url=sub02a',
            'GANGWON',
            '강원특별자치도 정선군 정선공설운동장 및 정선아리랑시장 일원',
            '2026-10-01 10:00:00',
            '2026-10-04 21:00:00',
            '10:00~21:00',
            '무료',
            '033-560-3013~5',
            '정선군'
        ),
        (
            '고성문화재단',
            '제26회 고성명태축제',
            '축제',
            '고성 명태와 가을 바다를 주제로 먹거리, 체험과 공연을 즐기는 해변 축제입니다.',
            'https://korean.visitkorea.or.kr/kfes/detail/fstvlDetail.do?fstvlCntntsId=0d4b1a68-4a2b-4426-a18d-d697d3cdfcd4',
            'GANGWON',
            '강원특별자치도 고성군 거진읍 11리 해변 일원',
            '2026-10-02 00:00:00',
            '2026-10-05 23:59:59',
            NULL,
            '입장료 무료(일부 체험 및 판매 유료)',
            '033-682-8008',
            '고성명태축제위원회'
        ),
        (
            '횡성문화관광재단',
            '제22회 횡성한우축제',
            '축제',
            '횡성한우를 중심으로 먹거리와 문화예술, 체험을 함께 즐기는 체류형 관광축제입니다.',
            'https://korean.visitkorea.or.kr/kfes/detail/fstvlDetail.do?Flag=Y&cmsCntntsId=232325',
            'GANGWON',
            '강원특별자치도 횡성군 횡성읍 북천리 221 섬강둔치 일원',
            '2026-10-07 10:00:00',
            '2026-10-11 21:00:00',
            '10:00~21:00(프로그램별 상이)',
            '무료(먹거리 별도 유료)',
            '033-808-8007, 033-808-8009',
            '횡성문화관광재단'
        ),
        (
            '서울특별시',
            '2026 차없는 잠수교 뚜벅뚜벅 축제(하반기)',
            '축제',
            '일요일마다 잠수교를 보행 공간으로 열어 공연, 놀이터, 먹거리와 휴식을 즐기는 한강 축제입니다.',
            'https://festival.seoul.go.kr/festival/main/festivalView.do?festacode=403',
            'SEOUL',
            '서울특별시 잠수교 및 반포한강공원 일원',
            '2026-09-06 14:00:00',
            '2026-10-25 22:00:00',
            '매주 일요일 14:00~22:00',
            NULL,
            '02-6401-9717',
            '서울특별시'
        ),
        (
            '서울문화재단',
            '2026 서울어텀페스타',
            '공연',
            '서울 전역의 공연과 축제를 하나로 연결해 다양한 공연예술을 소개하는 가을 축제입니다.',
            'https://news.seoul.go.kr/culture/archives/534503',
            'SEOUL',
            '서울 전역 공연장·한강공원·서울문화재단 대학로센터 등',
            '2026-09-18 00:00:00',
            '2026-11-29 23:59:59',
            '행사별 상이',
            '공연별 상이',
            '02-2133-2555',
            '서울특별시'
        ),
        (
            '서울특별시',
            '2026 광화문광장 빛모락 가을축제',
            '축제',
            '광화문광장에서 공연과 강연, 글쓰기와 전통놀이 체험을 즐기는 가을 문화행사입니다.',
            'https://festival.seoul.go.kr/festival/main/festivalView.do?festacode=367',
            'SEOUL',
            '서울특별시 종로구 광화문광장 놀이마당',
            '2026-09-24 00:00:00',
            '2026-09-27 23:59:59',
            NULL,
            NULL,
            NULL,
            '서울특별시'
        ),
        (
            '남산골한옥마을',
            '2026 남산골 추석축제 남산달빛마당',
            '축제',
            '남산골한옥마을에서 공연과 전통문화 체험을 즐기는 추석 행사입니다.',
            'https://festival.seoul.go.kr/festival/main/festivalView.do?festacode=852',
            'SEOUL',
            '서울특별시 중구 남산골한옥마을',
            '2026-09-25 10:00:00',
            '2026-09-27 20:00:00',
            '10:00~20:00',
            '무료(일부 체험 및 식음료 유료)',
            '02-6358-5533',
            '서울특별시'
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
    CASE
        WHEN seed.url IN (
            'https://korean.visitkorea.or.kr/kfes/detail/fstvlDetail.do?fstvlCntntsId=3d76c452-c3c6-4959-94e9-07200623c75d',
            'https://korean.visitkorea.or.kr/kfes/detail/fstvlDetail.do?Flag=Y&cmsCntntsId=506670',
            'https://www.ycg.kr/open.content/samgang/notification/event/?i=185296',
            'https://www.busan.go.kr/nbtnewsBU/1755302'
        ) THEN '00000000-0000-7000-8400-000000000001'::UUID
        WHEN seed.url IN (
            'https://www.mcst.go.kr/site/s_culture/festival/festivalView.jsp?pRo=9&pSeq=11596',
            'https://festival.gimje.go.kr/content/content200.do',
            'https://www.recollection.kr/',
            'https://www.gokseong.go.kr/tour/festivity/simcheong/'
        ) THEN '00000000-0000-7000-8400-000000000002'::UUID
        WHEN seed.url IN (
            'https://www.mcst.go.kr/site/s_culture/festival/festivalView.jsp?pRo=79&pSeq=11693',
            'https://www.mcst.go.kr/site/s_culture/festival/festivalView.jsp?pRo=4&pSeq=11672',
            'https://www.insamfestival.co.kr/',
            'https://www.baekjecf.or.kr/event/01.php',
            'https://korean.visitkorea.or.kr/kfes/detail/fstvlDetail.do?fstvlCntntsId=813eddb1-c961-4572-95b5-490dd8f54804'
        ) THEN '00000000-0000-7000-8100-000000000002'::UUID
        ELSE '00000000-0000-7000-8100-000000000003'::UUID
    END,
    'MEMBER',
    seed.inst_nm,
    seed.title,
    seed.category,
    seed.content,
    seed.url,
    seed.begin_de::TIMESTAMP,
    seed.end_de::TIMESTAMP,
    seed.event_tm_info,
    seed.partcpt_expn_info,
    seed.telno_info,
    seed.host_inst_nm,
    CURRENT_TIMESTAMP,
    CASE
        WHEN seed.end_de::TIMESTAMP < CURRENT_TIMESTAMP THEN 'CLOSED'
        ELSE 'OPEN'
    END,
    seed.region,
    seed.region_detail,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
FROM seed
WHERE NOT EXISTS (
    SELECT 1
    FROM festival existing
    WHERE existing.url = seed.url
      AND existing.deleted_at IS NULL
);

WITH seed_submission (submission_id, reference_url) AS (
    VALUES
        ('00000000-0000-7000-9000-000000000001'::UUID, 'https://korean.visitkorea.or.kr/kfes/detail/fstvlDetail.do?fstvlCntntsId=3d76c452-c3c6-4959-94e9-07200623c75d'),
        ('00000000-0000-7000-9000-000000000002'::UUID, 'https://korean.visitkorea.or.kr/kfes/detail/fstvlDetail.do?Flag=Y&cmsCntntsId=506670'),
        ('00000000-0000-7000-9000-000000000003'::UUID, 'https://www.ycg.kr/open.content/samgang/notification/event/?i=185296'),
        ('00000000-0000-7000-9000-000000000004'::UUID, 'https://www.busan.go.kr/nbtnewsBU/1755302'),
        ('00000000-0000-7000-9000-000000000005'::UUID, 'https://www.mcst.go.kr/site/s_culture/festival/festivalView.jsp?pRo=9&pSeq=11596'),
        ('00000000-0000-7000-9000-000000000006'::UUID, 'https://festival.gimje.go.kr/content/content200.do'),
        ('00000000-0000-7000-9000-000000000007'::UUID, 'https://www.recollection.kr/'),
        ('00000000-0000-7000-9000-000000000008'::UUID, 'https://www.gokseong.go.kr/tour/festivity/simcheong/'),
        ('00000000-0000-7000-9000-000000000009'::UUID, 'https://www.mcst.go.kr/site/s_culture/festival/festivalView.jsp?pRo=79&pSeq=11693'),
        ('00000000-0000-7000-9000-000000000010'::UUID, 'https://www.mcst.go.kr/site/s_culture/festival/festivalView.jsp?pRo=4&pSeq=11672'),
        ('00000000-0000-7000-9000-000000000011'::UUID, 'https://www.insamfestival.co.kr/'),
        ('00000000-0000-7000-9000-000000000012'::UUID, 'https://www.baekjecf.or.kr/event/01.php'),
        ('00000000-0000-7000-9000-000000000013'::UUID, 'https://korean.visitkorea.or.kr/kfes/detail/fstvlDetail.do?fstvlCntntsId=813eddb1-c961-4572-95b5-490dd8f54804'),
        ('00000000-0000-7000-9000-000000000014'::UUID, 'https://jacf.or.kr/jacf/pageview.php?keyvalue=sub02&url=sub02a'),
        ('00000000-0000-7000-9000-000000000015'::UUID, 'https://korean.visitkorea.or.kr/kfes/detail/fstvlDetail.do?fstvlCntntsId=0d4b1a68-4a2b-4426-a18d-d697d3cdfcd4'),
        ('00000000-0000-7000-9000-000000000016'::UUID, 'https://korean.visitkorea.or.kr/kfes/detail/fstvlDetail.do?Flag=Y&cmsCntntsId=232325'),
        ('00000000-0000-7000-9000-000000000017'::UUID, 'https://festival.seoul.go.kr/festival/main/festivalView.do?festacode=403'),
        ('00000000-0000-7000-9000-000000000018'::UUID, 'https://news.seoul.go.kr/culture/archives/534503'),
        ('00000000-0000-7000-9000-000000000019'::UUID, 'https://festival.seoul.go.kr/festival/main/festivalView.do?festacode=367'),
        ('00000000-0000-7000-9000-000000000020'::UUID, 'https://festival.seoul.go.kr/festival/main/festivalView.do?festacode=852')
)
INSERT INTO festival_submission (
    id,
    festival_id,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    seed_submission.submission_id,
    festival.id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
FROM seed_submission
JOIN festival
  ON festival.url = seed_submission.reference_url
 AND festival.provider_type = 'MEMBER'
 AND festival.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM festival_submission existing
    WHERE existing.id = seed_submission.submission_id
       OR (existing.festival_id = festival.id AND existing.deleted_at IS NULL)
);

-- 기존 로컬 V22 데이터가 있어도 최종 작성자 분포가 동일하도록 20개 행사의 작성자를 재배치합니다.
WITH festival_author (reference_url, author_email) AS (
    VALUES
        ('https://korean.visitkorea.or.kr/kfes/detail/fstvlDetail.do?fstvlCntntsId=3d76c452-c3c6-4959-94e9-07200623c75d', 'seed-rookie-2@example.com'),
        ('https://korean.visitkorea.or.kr/kfes/detail/fstvlDetail.do?Flag=Y&cmsCntntsId=506670', 'seed-rookie-2@example.com'),
        ('https://www.ycg.kr/open.content/samgang/notification/event/?i=185296', 'seed-rookie-2@example.com'),
        ('https://www.busan.go.kr/nbtnewsBU/1755302', 'seed-rookie-2@example.com'),
        ('https://www.mcst.go.kr/site/s_culture/festival/festivalView.jsp?pRo=9&pSeq=11596', 'seed-rookie-3@example.com'),
        ('https://festival.gimje.go.kr/content/content200.do', 'seed-rookie-3@example.com'),
        ('https://www.recollection.kr/', 'seed-rookie-3@example.com'),
        ('https://www.gokseong.go.kr/tour/festivity/simcheong/', 'seed-rookie-3@example.com'),
        ('https://www.mcst.go.kr/site/s_culture/festival/festivalView.jsp?pRo=79&pSeq=11693', 'seed-maker@example.com'),
        ('https://www.mcst.go.kr/site/s_culture/festival/festivalView.jsp?pRo=4&pSeq=11672', 'seed-maker@example.com'),
        ('https://www.insamfestival.co.kr/', 'seed-maker@example.com'),
        ('https://www.baekjecf.or.kr/event/01.php', 'seed-maker@example.com'),
        ('https://korean.visitkorea.or.kr/kfes/detail/fstvlDetail.do?fstvlCntntsId=813eddb1-c961-4572-95b5-490dd8f54804', 'seed-maker@example.com'),
        ('https://jacf.or.kr/jacf/pageview.php?keyvalue=sub02&url=sub02a', 'seed-master@example.com'),
        ('https://korean.visitkorea.or.kr/kfes/detail/fstvlDetail.do?fstvlCntntsId=0d4b1a68-4a2b-4426-a18d-d697d3cdfcd4', 'seed-master@example.com'),
        ('https://korean.visitkorea.or.kr/kfes/detail/fstvlDetail.do?Flag=Y&cmsCntntsId=232325', 'seed-master@example.com'),
        ('https://festival.seoul.go.kr/festival/main/festivalView.do?festacode=403', 'seed-master@example.com'),
        ('https://news.seoul.go.kr/culture/archives/534503', 'seed-master@example.com'),
        ('https://festival.seoul.go.kr/festival/main/festivalView.do?festacode=367', 'seed-master@example.com'),
        ('https://festival.seoul.go.kr/festival/main/festivalView.do?festacode=852', 'seed-master@example.com')
)
UPDATE festival target
SET member_id = author.id,
    updated_at = CURRENT_TIMESTAMP
FROM festival_author mapping
JOIN member author
  ON author.email = mapping.author_email
 AND author.deleted_at IS NULL
WHERE target.url = mapping.reference_url
  AND target.provider_type = 'MEMBER'
  AND target.deleted_at IS NULL;

-- 대표 민원번호 대신 공식 행사 문의 번호를 사용합니다.
UPDATE festival
SET telno_info = '02-3011-7781',
    updated_at = CURRENT_TIMESTAMP
WHERE url = 'https://itour.incheon.go.kr/ssst/ssst/list.do?fstv_month=09&pageNm=fstv'
  AND provider_type = 'MEMBER'
  AND deleted_at IS NULL;

-- 이전 로컬 V22가 만든 고정 관리자 계정은 권한과 로그인을 제거합니다.
UPDATE member
SET email = 'disabled-roomescape-seed@example.com',
    password = '$2a$10$3J0CLAJcpClv0aXxx62Ir.isAsHcL4iA8CG2ahEZz9vVp9tM4pWge',
    role = 'ROLE_UNVERIFIED',
    nickname = '이전 로컬 시드 계정',
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'roomescape@example.com'
  AND deleted_at IS NULL;

-- 로컬에서 V30이 먼저 만든 목 후기 좋아요를 비운 뒤 운영과 같은 평가자 구성으로 다시 만듭니다.
DELETE FROM "like"
WHERE src_id IN (
    SELECT ('00000000-0000-7000-a030-' || LPAD(post_no::TEXT, 12, '0'))::UUID
    FROM GENERATE_SERIES(1, 18) AS numbers(post_no)
);

-- V30은 운영 DB에서 행사 시드보다 먼저 실행되므로, 행사 생성 후 후기·댓글·좋아요를 다시 보완합니다.
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
        ('seed-warning-voter-02@example.com'),
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

