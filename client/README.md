# EventUs 프론트엔드

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

| 화면 이름     | 백엔드의 실제 의미                                                    | 구현                                                                    |
| ------------- | --------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| 지역 문화행사 | `ProviderType.PUBLIC`: 경기도 문화행사 Open API에서 수집한 정보       | 미리보기의 목록·검색·지역·날짜·종류 필터·상세. 실제 모드는 준비 중 상태 |
| 행사 제보     | `ProviderType.MEMBER`: 회원이 행사 정보를 제보하고 자신의 제보를 관리 | 등록·내 목록·상세·수정·삭제 API 연결                                    |
| 행사 후기     | 특정 `festivalId`에 대한 후기                                         | 행사 선택·목록·상세·작성·수정·삭제, 댓글·좋아요 연결                    |

`PUBLIC/MEMBER`는 주최자의 공공/민간 구분이 아니라 **데이터의 유입 경로**입니다. 제보는 참가 신청이 아니며, 다른 회원의 제보를 모아 보는 API도 없습니다. 따라서 두 번째 화면은 공개 민간행사 게시판이 아니라 **내 행사 제보 관리**로 구현했습니다.

## 화면과 동작

- 홈: 행사 탐색, 검색, 서비스 바로가기, 예시 행사와 후기.
- 지역 문화행사: 검색 조건을 URL에 저장, 지역·일자·종류 필터, 정렬, 카드/목록 전환, 빈 결과 상태.
- 행사 제보: 필수 필드 검증, 지역 enum, 종료일 검증, 이미지 업로드, 등록/수정/삭제.
- 상세: 사진, 일정·주소·비용·운영 정보, 소개/후기 탭, 링크 복사, 등록된 참고 링크, 문의 연결.
- 후기: 행사별 pagination·정렬, 이미지, 수정·삭제, 댓글 pagination·수정·삭제, 좋아요·취소.
- 마이페이지: 프로필·닉네임·연락처, 실제 role에 따른 등급, 내 제보, 비공개 문의·신고 및 답변.
- 인증: 회원가입, 로그인, 로그아웃, refresh cookie를 이용한 세션 복구, 401 재발급·재시도, 비밀번호 이메일 인증 흐름.
- 반응형: 모바일 메뉴, 320px 이상의 화면 대응, 키보드 focus, native dialog, loading/error/empty 상태.

별점, 행사 참가 신청, 찜, 싫어요, 신뢰도 점수·활동 점수는 현재 Controller/DTO에 없어 만들지 않았습니다. 후기 목록은 전체 feed가 아닌 **행사별** API이므로 실제 모드에서는 내 제보로 확인할 수 있는 행사를 선택합니다.

## 모드와 설정

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

`origin/dev`의 `207febb`를 병합하면서 `GET /api/v1/festivals` 목록 API가 추가되었습니다. `region`과 `date`가 필수이며 Spring Page로 응답합니다. 현재 프론트에는 아직 연결하지 않아 실제 모드의 지역 문화행사는 준비 중 화면을 유지합니다. 행사 상세 GET API는 아직 없습니다. 이 변경에서는 폴더명과 프로젝트 설정만 정리했습니다.

## 검증

- 최종 결과: production build 성공, Playwright 15개 테스트 통과.
- TypeScript type check 및 production build.
- Playwright: 미리보기 격리, 검색/필터/URL 복구, 제보·후기·댓글 CRUD, 좋아요 토글, 실제 DTO 형식, private 제보 상세 응답, 실패 시 예시 fallback 금지, 401 재발급, 로그인과 token 비영속화, role 제한, 문의 상태, 화면 크기별 overflow.
- axe-core: 홈·제보 form·마이페이지·로그인·행사 상세의 WCAG A/AA 자동 검사.
- 실제 로컬 서버: 프론트 HTTP 200, proxy를 통한 미인증 `/members/me` HTTP 401, OpenAPI 경로 확인.
- 백엔드 계정을 새로 생성하거나 DB 데이터를 쓰지 않았고, 메일 발송·R2 업로드도 실제로 실행하지 않았습니다. 인증 후 쓰기 동작은 격리된 preview와 응답 mock 기반 브라우저 테스트로 검증했습니다.

`src/lib/api.ts`에 API adapter, `src/lib/types.ts`에 DTO, `src/lib/demo.ts`에 예시 데이터가 있습니다. 주요 화면은 `src/pages`, 공통 UI는 `src/components`에 분리했습니다. 이미지 출처는 [ASSETS.md](./ASSETS.md)에 기록했습니다.

Vite proxy와 effect cleanup 구현은 [Vite 공식 문서](https://vite.dev/config/server-options#server-proxy), [React 공식 문서](https://react.dev/reference/react/useEffect)를 확인했습니다.

## 화면 캡처

- [데스크톱 홈](./docs/screenshots/home-desktop.png) / [모바일 홈](./docs/screenshots/home-mobile.png)
- [데스크톱 상세](./docs/screenshots/detail-desktop.png) / [모바일 상세](./docs/screenshots/detail-mobile.png)
- [내 행사 제보](./docs/screenshots/submissions-desktop.png) / [마이페이지](./docs/screenshots/mypage-desktop.png)
