# roomescape-front

백엔드 인증 로직 검증용 프론트엔드.

## 필요한 것

- Node 22+
- pnpm 10+
- **백엔드가 8080 포트에서 실행 중이어야 합니다**

## 실행

```bash
pnpm install
cp .env.example .env.local
pnpm dev
```

`http://localhost:3000` 으로 열립니다.

> ⚠️ **3000 포트를 바꾸지 마세요.** 백엔드 `SecurityConfig` 의 CORS 허용 목록이
> `localhost:3000`, `localhost:3001` 뿐입니다. 다른 포트로 뜨면 로그인이 CORS 에서 막힙니다.
> `vite.config.ts` 에 `strictPort: true` 를 걸어둬서 포트가 점유돼 있으면 다른 포트로
> 도망가지 않고 에러를 냅니다.

## 스크립트

| 명령              | 설명             |
| ----------------- | ---------------- |
| `pnpm dev`        | 개발 서버 (3000) |
| `pnpm build`      | 프로덕션 빌드    |
| `pnpm lint`       | oxlint           |
| `pnpm lint:fix`   | 자동 수정        |
| `pnpm format`     | Prettier 포맷    |
| `pnpm type-check` | 타입 검사        |

## 스택

| 항목   | 선택                | 비고                                         |
| ------ | ------------------- | -------------------------------------------- |
| 번들러 | Vite                |                                              |
| 언어   | TypeScript          |                                              |
| 스타일 | **Tailwind CSS v4** | 설정 파일 없이 `@import 'tailwindcss'` 한 줄 |
| HTTP   | axios               | 인터셉터로 토큰·refresh 처리                 |
| 린터   | oxlint              | ESLint보다 50~100배 빠름                     |
| 포매터 | Prettier            |                                              |
| 라우팅 | react-router        | _설치만 됨. Phase 1에서 사용_                |

## 폴더 구조

백엔드의 `domain/` 구조를 그대로 따라갑니다.

```
src/
├── features/
│   ├── auth/          # 로그인·로그아웃·토큰 재발급
│   │   ├── api.ts
│   │   └── types.ts
│   └── member/        # 마이페이지
│       ├── api.ts
│       └── types.ts
├── shared/
│   └── api/
│       ├── client.ts      # ★ axios 인스턴스 + 인터셉터 (핵심)
│       ├── types.ts       # ApiResponse, ApiError
│       ├── errorCodes.ts  # AUTH001 등 백엔드 에러 코드
│       └── tokenStore.ts  # Access Token 보관 (메모리)
└── App.tsx            # 검증 화면 (Phase 1에서 pages/ 로 분리)
```

## 인증이 동작하는 방식

### 토큰 두 개의 역할

| 토큰          | 저장 위치                        | 수명 |
| ------------- | -------------------------------- | ---- |
| Access Token  | **메모리** (`tokenStore`)        | 30분 |
| Refresh Token | **HttpOnly 쿠키** (JS가 못 읽음) | 14일 |

Access Token 을 `localStorage` 에 넣지 않습니다. XSS 로 통째로 털리기 때문입니다.
메모리라 새로고침하면 사라지는데, 앱 시작 시 `/auth/refresh` 를 한 번 호출해 복구합니다.

### 요청 흐름

```
요청 → Authorization: Bearer {accessToken} 자동 첨부
     → 401 AUTH202(만료)를 받으면
     → POST /api/v1/auth/refresh  (쿠키 자동 전송)
     → 새 토큰 저장 후 원래 요청 재시도
     → refresh 도 실패하면 토큰 폐기 (로그아웃 상태)
```

동시에 여러 요청이 401 을 받아도 refresh 는 **한 번만** 나갑니다
(`client.ts` 의 `refreshing` Promise).

### 응답 봉투를 벗깁니다

백엔드는 항상 이렇게 내려줍니다.

```json
{ "success": true, "code": "0000", "message": "", "data": { ... } }
```

인터셉터가 `data` 만 꺼내서 넘기므로, 화면 코드에서는 `res.data.data` 를 쓸 일이 없습니다.

```ts
const me = await getMyPage() // 바로 MyPageInfo
```

실패 응답은 `ApiError` 로 변환되어 `code` 와 `message` 를 그대로 들고 옵니다.

```ts
catch (e) {
  if (e instanceof ApiError && e.code === ERROR_CODE.INVALID_CREDENTIALS) { ... }
}
```

## 컨벤션

### 파일·폴더

| 대상     | 규칙         | 예                           |
| -------- | ------------ | ---------------------------- |
| 컴포넌트 | PascalCase   | `LoginPage.tsx`              |
| 그 외    | camelCase    | `client.ts`, `tokenStore.ts` |
| 폴더     | kebab-case   | `features/auth`              |
| 훅       | `use` 접두사 | `useAuth.ts`                 |

### 규칙

1. **컴포넌트에서 `axios` 를 직접 부르지 않습니다.** 반드시 `features/*/api.ts` 를 거칩니다
2. **타입은 백엔드 DTO 와 이름·필드를 그대로 맞춥니다.** `MyPageInfo` → `MyPageInfo`
3. **에러 코드 문자열 하드코딩 금지.** `ERROR_CODE` 상수를 씁니다
4. **import 는 절대경로** `@/features/auth/api`
5. **`any` 금지**

> `@/` 별칭은 `vite.config.ts` 의 `resolve.alias` 와 `tsconfig.app.json` 의 `paths`
> **두 곳에** 있습니다. 하나만 고치면 에디터나 빌드 중 한쪽이 깨집니다.

### 커밋

백엔드와 동일하게 쓰되 브랜치는 `front-` 접두사를 붙입니다.

```
feat: 로그인 화면 구현
fix: refresh 인터셉터 무한루프 수정

브랜치: feat/front-login
```

## 검증 항목

`pnpm dev` 후 아래 5개가 통과하면 인증 로직 전체가 검증된 것입니다.

- [ ] 로그인 → 마이페이지 정보 표시
- [ ] 새로고침 → 로그인 유지 (refresh 로 복구)
- [ ] 로그아웃 → 로그인 화면으로
- [ ] 틀린 비밀번호 → `[AUTH001]` 메시지
- [ ] 30분 방치 후 "내 정보 다시 조회" → 자동 재발급 (Network 탭에 refresh 요청)

## 자주 막히는 것

| 증상                            | 원인                                                                                  |
| ------------------------------- | ------------------------------------------------------------------------------------- |
| CORS 에러                       | 3000 포트가 아님. 백엔드 CORS 허용 목록 확인                                          |
| 로그인은 되는데 refresh 가 401  | `withCredentials: true` 누락                                                          |
| 쿠키가 안 보임                  | 정상입니다. HttpOnly 라 JS 로 못 읽습니다. DevTools → Application → Cookies 에서 확인 |
| `Network Error`                 | 백엔드가 안 떠 있음                                                                   |
| Swagger 는 되는데 프론트만 실패 | 십중팔구 credentials 문제                                                             |

## 다음 단계

- [ ] react-router 로 화면 분리 (`pages/LoginPage`, `pages/MyPage`)
- [ ] 회원가입 화면
- [ ] 회원 정보 수정 / 비밀번호 변경(2단계 이메일 인증)
- [ ] 이미지 업로드 (R2)
