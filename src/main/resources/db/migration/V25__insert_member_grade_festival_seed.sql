-- 회원 신뢰 등급별 화면을 확인하기 위한 행사 제보 초기 데이터입니다.
-- 2026-09-28 기준 진행 중이며, 기존 제보 데이터에 없던 광역 지역의 행사만 사용합니다.

-- 모든 테스트 계정의 초기 비밀번호는 BCrypt로 암호화된 123456a! 입니다.
WITH seed_member (id, email, role, nickname) AS (
    VALUES
        ('00000000-0000-7000-8100-000000000001'::UUID, 'seed-rookie@example.com', 'ROLE_UNVERIFIED', '주말 탈출 꿈나무'),
        ('00000000-0000-7000-8100-000000000002'::UUID, 'seed-maker@example.com', 'ROLE_RECOGNIZED', '동네 탈출 메이커'),
        ('00000000-0000-7000-8100-000000000003'::UUID, 'seed-master@example.com', 'ROLE_TRUSTED', '팔도 탈출 마스터'),
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
    '$2a$10$3J0CLAJcpClv0aXxx62Ir.isAsHcL4iA8CG2ahEZz9vVp9tM4pWgu',
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
            '032-120',
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
        ('https://itour.incheon.go.kr/ssst/ssst/list.do?fstv_month=09&pageNm=fstv', 'roomescape@example.com'),
        ('https://itour.incheon.go.kr/ssst/ssst/list.do?fstv_month=09&pageNm=fstv', 'seed-rookie@example.com'),
        ('https://itour.incheon.go.kr/ssst/ssst/list.do?fstv_month=09&pageNm=fstv', 'seed-master@example.com'),
        ('https://itour.incheon.go.kr/ssst/ssst/list.do?fstv_month=09&pageNm=fstv', 'seed-voter@example.com'),
        ('https://ulsan.go.kr/s/uam/bbs/view.do?bbsId=BBS_0000000000000188&dataId=58173&mId=001007002001000000', 'roomescape@example.com'),
        ('https://ulsan.go.kr/s/uam/bbs/view.do?bbsId=BBS_0000000000000188&dataId=58173&mId=001007002001000000', 'seed-rookie@example.com'),
        ('https://ulsan.go.kr/s/uam/bbs/view.do?bbsId=BBS_0000000000000188&dataId=58173&mId=001007002001000000', 'seed-master@example.com'),
        ('https://m.visitjeju.net/kr/festival/view?contentsid=CNTS_300000000014322&menuId=DOM_000001718007000000', 'roomescape@example.com'),
        ('https://m.visitjeju.net/kr/festival/view?contentsid=CNTS_300000000014322&menuId=DOM_000001718007000000', 'seed-rookie@example.com'),
        ('https://m.visitjeju.net/kr/festival/view?contentsid=CNTS_300000000014322&menuId=DOM_000001718007000000', 'seed-master@example.com'),
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'roomescape@example.com'),
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'seed-rookie@example.com'),
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'seed-maker@example.com'),
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'seed-voter@example.com'),
        ('https://www.ulsan.go.kr/s/bangudae/contents.ulsan?mId=001001004000000000', 'roomescape@example.com'),
        ('https://www.ulsan.go.kr/s/bangudae/contents.ulsan?mId=001001004000000000', 'seed-rookie@example.com'),
        ('https://www.ulsan.go.kr/s/bangudae/contents.ulsan?mId=001001004000000000', 'seed-maker@example.com'),
        ('https://www2.sejong.go.kr/youth/prog/progrm/kor/sub03_02/view.do?progrmNo=2051&tmeNo=1', 'roomescape@example.com'),
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
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'roomescape@example.com'),
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'seed-rookie@example.com'),
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'seed-maker@example.com'),
        ('https://www.incheon.go.kr/IC010101/view?curPage=14&nttNo=2046051&srchKey=srchCn&srchSiteRealmCode=&srchWord=', 'seed-voter@example.com'),
        ('https://www.ulsan.go.kr/s/bangudae/contents.ulsan?mId=001001004000000000', 'roomescape@example.com'),
        ('https://www.ulsan.go.kr/s/bangudae/contents.ulsan?mId=001001004000000000', 'seed-rookie@example.com'),
        ('https://www.ulsan.go.kr/s/bangudae/contents.ulsan?mId=001001004000000000', 'seed-maker@example.com'),
        ('https://www2.sejong.go.kr/youth/prog/progrm/kor/sub03_02/view.do?progrmNo=2051&tmeNo=1', 'roomescape@example.com'),
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
