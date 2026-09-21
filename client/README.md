# 방구석탈출(roomescape) 프론트엔드

`src/main/java`의 Controller·Service·DTO·SecurityConfig와 제공된 레퍼런스를 기준으로 작성한 React + TypeScript + Vite 프론트엔드입니다. 팀 합의에 따라 기존 프론트엔드를 대체하며, 프로젝트의 `client` 폴더에서 관리합니다.

## 실행

```powershell
cd client
pnpm install --frozen-lockfile
pnpm dev
```

브라우저에서 http://localhost:3001 을 엽니다. 기본 화면은 **디자인 미리보기**입니다. 화면 위의 `실제 API 연결`을 누르면 백엔드 모드로 전환됩니다.

```powershell
pnpm build
pnpm exec playwright install chromium
pnpm test
```

프로젝트는 `pnpm@10.28.1`과 `pnpm-lock.yaml`을 사용합니다. `packageManager`에 버전을 고정했고, dependency build script는 Vite에 필요한 `esbuild`만 허용합니다.

`pnpm preview`는 빌드 산출물 확인용입니다. API 연동은 `pnpm dev`의 proxy를 사용합니다. 정적 호스팅 시 `/api` reverse proxy와 SPA 경로 fallback을 별도로 설정해야 합니다.

## 이름을 정한 근거

| 화면 이름     | 백엔드의 실제 의미                                              | 구현                                                                                  |
| ------------- | --------------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| 지역 문화행사 | `ProviderType.PUBLIC`: 경기도 문화행사 Open API에서 수집한 정보 | 미리보기의 목록·검색·지역·날짜·종류 필터·상세. 실제 모드는 준비 중 상태               |
| 행사 제보     | `ProviderType.MEMBER`: 회원이 알려온 행사 정보                  | 전체 회원 제보 목록·검색·상태 필터·공개 행사 상세는 Mock, 등록 API 연결               |
| 행사 후기     | 행사에 연결된 후기                                              | 전체 후기 feed·행사 상세 이동은 Mock, 행사별 후기·작성·수정·삭제·댓글·좋아요 API 연결 |

`PUBLIC/MEMBER`는 주최자의 공공/민간 구분이 아니라 **데이터의 유입 경로**입니다. 미리보기의 행사 제보 탭에서는 다른 회원의 제보를 포함한 행사 정보를 둘러봅니다. 개인 제보 내용 및 수정·삭제는 **마이페이지 → 내 행사 제보**에서 관리하며, 행사 탐색 화면에는 개인 제보 내용과 회원 이메일·연락처를 표시하지 않습니다.

## 화면과 동작

- 홈: 행사 탐색, 검색, 서비스 바로가기, 예시 행사와 후기.
- 지역 문화행사: 시·도 → 시·군·구 2단계 지역 선택, 일자·종류 필터, 정렬, 카드/목록 전환, 빈 결과 상태. 홈 검색과 같은 지역 선택을 사용하며 URL·새로고침·뒤로 가기로 선택을 복구합니다. 시·도를 변경하면 이전 시·군·구 선택과 페이지를 초기화합니다.
- 행사 제보: 모든 회원이 제보한 행사 목록·검색·상태 필터·제보자 표시. 등록 form의 필수 필드·지역 enum·종료일 검증과 이미지 업로드.
- 상세: 사진, 일정·주소·비용·운영 정보, 소개/후기 탭, 링크 복사, 등록된 참고 링크, 문의 연결.
- 후기: 미리보기에서 좋아요순을 기본으로 전체 데이터를 정렬한 후 pagination을 적용합니다. 동률은 최신 시각순이며 최신순·오래된순도 선택할 수 있습니다. 홈 후기와 행사별 후기 탭에도 같은 기본 정렬을 적용합니다. 후기 제목→후기 상세 / 행사명→행사 상세로 이동합니다. 댓글 시각은 한국 시간 `YYYY.MM.DD HH:mm`으로 표시합니다. 실제 API의 `date`는 기존 백엔드의 `updatedAt` 값이므로, 수정한 댓글은 수정 시각이 표시됩니다. Mock에서는 작성 시각을 유지합니다.
- 마이페이지: 프로필·닉네임·연락처, 실제 role에 따른 등급, 내 제보, 비공개 문의·신고 및 답변.
- 인증: 회원가입, 로그인, 로그아웃, refresh cookie를 이용한 세션 복구, 401 재발급·재시도, 비밀번호 이메일 인증 흐름.
- 반응형: 모바일 메뉴, 320px 이상의 화면 대응, 키보드 focus, native dialog, loading/error/empty 상태.

별점, 행사 참가 신청, 찜, 싫어요, 신뢰도 점수·활동 점수는 현재 Controller/DTO에 없어 만들지 않았습니다. 후기 목록의 행사 선택 박스는 제거했으며, 후기 작성은 행사 상세에서 시작할 수 있습니다.

## 모드와 설정

### 시스템 관리자 Mock 화면

`http://localhost:3001/admin` 또는 서비스 하단의 **시스템 관리자 미리보기**에서 확인합니다. 행사 주최자가 아닌 서비스 전체 운영자를 위한 화면입니다.

- 운영 대시보드: 회원·행사·후기 수, 미처리 문의·신고, 콘텐츠 검토 현황, 최근 운영 기록.
- 회원 관리: 닉네임·이메일·ID 검색, 등급 필터, 사유를 남기는 신뢰 등급 변경. 관리자 계정의 권한 변경은 제외합니다.
- 행사·후기 관리: 검색과 상태 필터, 상세 검토, 공개·숨김 처리. 행사는 유입 경로 필터와 검토 대기 상태도 제공합니다.
- 문의·신고: 유형·상태 필터, 접수 내용 확인, 신고 대상 콘텐츠로 이동, 운영팀 답변 등록 및 완료 상태 확인.
- 운영 기록: 등급·노출 상태·답변 변경 기록을 검색하고 영역별로 확인합니다.

관리자 화면은 항상 Mock으로 동작하며, 공개 서비스의 API 모드와 별도 React provider 및 `eventus.admin.mock.v1` sessionStorage를 사용합니다. 관리자 진입 시 실제 인증·관리 API를 호출하지 않으며 일반 서비스의 예시 데이터도 변경하지 않습니다. 현재 탭에서 새로고침해도 변경 사항은 유지되고, 상단 **예시 초기화**로 복구할 수 있습니다.

`ROLE_*`, `QUESTION/REPORT`, `PENDING/ANSWERED` 명칭은 기존 백엔드 모델을 따릅니다. 콘텐츠의 공개·검토 대기·숨김은 **관리자 UI용 예시 상태**이며, 백엔드의 행사 진행 상태 `OPEN/CLOSED`와 다릅니다. 관리자 권한 검증과 실제 관리 API 연동은 구현하지 않았습니다. Mock 페이지이므로 로그인 없이 접근할 수 있습니다.

구현은 `src/admin/Admin.tsx`, `src/admin/store.tsx`, `src/admin/admin.css`에 있습니다.

### 일반 서비스 설정

`.env.example`을 `.env.local`로 복사해 필요할 때만 변경합니다. 루트의 백엔드 `.env`는 읽거나 복사하지 않습니다.

| 변수                  | 기본값                  | 용도                                                                               |
| --------------------- | ----------------------- | ---------------------------------------------------------------------------------- |
| `API_PROXY_TARGET`    | `http://localhost:8080` | Vite의 `/api` proxy 대상. 브라우저에 노출되는 비밀 설정이 아님                     |
| `VITE_DATA_MODE`      | `preview`               | `api`로 지정하면 처음부터 실제 API 모드                                            |
| `VITE_IMAGE_BASE_URL` | 없음                    | 기존 후기/댓글 DTO가 이미지 URL 대신 R2 key를 반환하는 경우의 공개 이미지 base URL |

미리보기는 `eventus.preview.v1` sessionStorage를 사용하는 별도 저장소입니다. 예시 행사명·일정·후기·회원은 실제 서비스 데이터가 아닙니다. 이 모드의 등록·수정·삭제는 서버에 전송되지 않습니다. 업로드 사진은 이 탭의 임시 object URL로 표시하므로 새로고침 이후에는 다시 선택해야 할 수 있습니다.

실제 모드에서 API 요청이 실패해도 예시 데이터로 대체하지 않습니다. access token은 메모리에만 보관하고, refresh token은 백엔드의 HttpOnly cookie를 사용합니다. 선택한 모드만 sessionStorage에 저장됩니다. 모드 변경 시 진행 중인 이전 세션의 결과는 버립니다.

## 확인된 연동 범위

아래 연결 범위는 2026-09-21 최초 프론트엔드 구현 때 소스와 `localhost:8080/v3/api-docs`를 함께 확인한 내용입니다. 이후 `origin/dev` 병합으로 추가된 API의 연결 상태는 표 아래에 따로 기록했습니다.

| 기능                        | 연결한 API (`/api/v1` 기준)                                                            | 비고                                                                      |
| --------------------------- | -------------------------------------------------------------------------------------- | ------------------------------------------------------------------------- |
| 로그인·가입·재발급·로그아웃 | `POST /auth/login`, `/signup`, `/refresh`, `/logout`                                   | 응답 `data.accessToken`, Authorization Bearer, cookie 포함                |
| 내 정보                     | `GET/PATCH /members/me`                                                                | `id,email,nickname,profileImg,phone,role,createdAt,updatedAt`             |
| 비밀번호 변경               | `POST /members/me/password/verification-code`, `/verify`, `PATCH /members/me/password` | 자동 발송하지 않으며 사용자가 버튼을 눌러 진행                            |
| 행사 제보                   | `POST /festivals/submissions`                                                          | `festivalContent`, `submissionContent` 분리. 참가 신청 아님               |
| 내 제보                     | `GET /members/me/submissions`, `GET/PATCH/DELETE /members/me/submissions/{id}`         | 상세 응답은 `data.submission.festival`                                    |
| 후기                        | `GET/POST /festivals/{id}/posts`, `GET/PATCH/DELETE /posts/{id}`                       | 목록은 Spring Page, 제목 2~30자, 별점 필드 없음                           |
| 댓글                        | `GET/POST /posts/{id}/comments`, `PATCH/DELETE /comments/{id}`                         | 목록은 배열, 페이지 크기 20, 내용 최대 500자                              |
| 좋아요                      | `GET /posts/{id}/likes`, `POST/DELETE /posts/{id}/likes/me`                            | 개수는 Long, 등록/취소는 `{likeCount}`. 내 상태 조회 API 없음             |
| 문의·신고                   | `GET /inquiries/me`, `POST /inquiries`, `GET/PATCH/DELETE /inquiries/{id}`             | `QUESTION/REPORT`, `PENDING/ANSWERED`, 답변 후 수정 제한                  |
| 이미지                      | `POST /images`                                                                         | multipart `file`, `type=POST/PROFILE/INQUIRY/FESTIVAL`, 5MB, JPG/PNG/WEBP |

이미지는 **프로필·문의에 `key`**, 행사·후기에 해당 DTO가 요구하는 **`url`**을 전달합니다. 백엔드의 image API 설명은 key 저장을 권장하지만, 현재 Festival/Post Service는 URL 문자열을 그대로 저장하고 응답하므로 해당 DTO/Service에 맞췄습니다.

최초 확인 당시 8080 서버의 OpenAPI에는 소스에 있는 **문의 API와 좋아요 취소 DELETE가 나타나지 않았습니다**. 이 두 기능은 서버와 소스 버전을 맞춘 뒤 실제 연동 확인이 필요합니다. 이번 폴더 변경에서는 백엔드 서버를 재시작하지 않았습니다.

`GET /api/v1/festivals` 지역 문화행사 목록은 `region`과 `date`가 필수이며, 현재 프론트의 지역 문화행사 목록은 여전히 준비 중 상태입니다. 전체 후기, 전체 회원 제보 목록, 공개 행사 상세 조회 API는 현재 백엔드에 없습니다. 해당 탐색 화면은 Mock 모드에서만 제공하고 실제 API 모드에서는 준비 중 상태를 표시하며, 존재하지 않는 조회 API를 호출하지 않습니다. 기존 행사별 후기 API와 마이페이지의 내 제보 CRUD는 유지합니다. 이 변경의 범위는 프론트엔드이며 백엔드 변경은 없습니다.

지역 선택 데이터는 `src/lib/regions.ts`, 공통 선택 UI는 `src/components/RegionSelects.tsx`에서 관리합니다. 17개 시·도 아래에 시·군·구 옵션을 두며, 세종은 시 전체로 선택합니다. 경기도의 기존 `GYEONGGI_*` 검색 URL은 유지합니다. 그 외 세부 지역의 `시도코드:지역명` 값은 프론트 검색용으로만 사용하고 제보 등록 DTO에는 추가하지 않습니다. 현재 예시 문화행사는 경기도에만 있어 다른 지역을 선택하면 빈 결과를 표시합니다. 서울 자치구는 [서울시 안내](https://www.seoul.go.kr/seoul/autonomy.do), 인천의 제물포구·영종구·검단구는 [인천시 행정체제 개편 안내](https://www.incheon.go.kr/IC010601/2187729)를 참고했습니다.

기존 후기 목록 API와 Post DTO는 좋아요순 정렬을 지원하지 않습니다. 실제 API 모드의 행사별 후기에는 최신순·오래된순만 제공하고, 좋아요순 옵션은 준비 중으로 비활성화합니다. 좋아요순을 서버로 보내거나 현재 페이지만 재정렬해 전체 인기순처럼 표시하지 않습니다.

## 검증

- 최종 결과: production build 성공, Playwright 31개 테스트 통과.
- TypeScript type check 및 production build.
- Playwright: 미리보기 격리, 검색/필터/URL 복구, 제보·후기·댓글 CRUD, 좋아요 토글, 실제 DTO 형식, private 제보 상세 응답, 실패 시 예시 fallback 금지, 401 재발급, 로그인과 token 비영속화, role 제한, 문의 상태, 화면 크기별 overflow.
- axe-core: 홈·제보 form·마이페이지·로그인·행사 상세의 WCAG A/AA 자동 검사.
- 관리자: 실제 API 모드와 Mock 격리, 회원 등급 변경·초기화·새로고침 유지, 행사 공개·후기 복구, 신고 콘텐츠 숨김 및 별도 답변, 운영 기록 반영. 관리자 6개 화면과 회원 관리 dialog의 axe-core 자동 검사 및 320·390·768·1024px overflow 검사.
- 홈 검색창: 320~1440px의 15개 폭에서 라벨 한 줄 유지·입력칸 너비·overflow 확인. 1100px 이하는 2열, 380px 이하는 1열로 배치합니다.
- 지역·후기 필터: 상위 지역 변경 시 하위 선택 초기화, 기존 URL 복구, 다른 지역·세종·빈 결과, 좋아요 동률·페이지 이동·좋아요 반영·정렬 변경 시 첫 페이지 복귀, 실제 API 정렬 필드 제한을 검증합니다. 지역 검색의 모바일·데스크톱 접근성과 320~1440px overflow도 확인합니다.
- 후기·행사 제보: Mock 전체 feed·행사 링크 분리, 다른 회원 제보 조회와 개인 관리 분리, 한국 시간 댓글 표시, 네비게이션 밑줄과 글자 사이 간격, 실제 모드의 미지원 조회 차단과 모바일 접근성 확인.
- 실제 로컬 서버: 프론트 HTTP 200, proxy를 통한 미인증 `/members/me` HTTP 401, OpenAPI 경로 확인.
- 백엔드 계정을 새로 생성하거나 DB 데이터를 쓰지 않았고, 메일 발송·R2 업로드도 실제로 실행하지 않았습니다. 인증 후 쓰기 동작은 격리된 preview와 응답 mock 기반 브라우저 테스트로 검증했습니다.

`src/lib/api.ts`에 API adapter, `src/lib/types.ts`에 DTO, `src/lib/demo.ts`에 예시 데이터가 있습니다. 주요 화면은 `src/pages`, 공통 UI는 `src/components`에 분리했습니다. 이미지 출처는 [ASSETS.md](./ASSETS.md)에 기록했습니다.

Vite proxy와 effect cleanup 구현은 [Vite 공식 문서](https://vite.dev/config/server-options#server-proxy), [React 공식 문서](https://react.dev/reference/react/useEffect)를 확인했습니다.

## 화면 캡처

- [데스크톱 홈](./docs/screenshots/home-desktop.png) / [모바일 홈](./docs/screenshots/home-mobile.png)
- [데스크톱 상세](./docs/screenshots/detail-desktop.png) / [모바일 상세](./docs/screenshots/detail-mobile.png)
- [내 행사 제보](./docs/screenshots/submissions-desktop.png) / [마이페이지](./docs/screenshots/mypage-desktop.png)
- [관리자 대시보드](./docs/screenshots/admin-desktop.png) / [회원 관리](./docs/screenshots/admin-members.png) / [모바일 관리자](./docs/screenshots/admin-mobile.png)
- [검색창 수정 후 태블릿 홈](./docs/screenshots/home-tablet.png)
- [전체 후기와 행사 링크](./docs/screenshots/reviews-feed.png) / [회원 제보 목록](./docs/screenshots/community-submissions.png) / [다른 회원의 행사 상세](./docs/screenshots/community-detail.png) / [댓글 작성 시각](./docs/screenshots/comments-time.png)
- [2단계 지역 검색 데스크톱](./docs/screenshots/regions-1440.png) / [2단계 지역 검색 모바일](./docs/screenshots/regions-390.png)
