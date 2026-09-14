# roomescape

문화행사/후기 백엔드 (Spring Boot).  
이 문서는 **로컬에서 프로젝트를 켜고 API를 호출하는 방법**입니다.

## 필요한 것

- JDK **25**
- Docker Desktop (PostgreSQL용)
- IDE: IntelliJ 또는 Cursor  
Lombok 플러그인을 켜 두세요. 안 켜면 빨간 줄이 많이 납니다.

## 한 번에 실행

프로젝트 루트(`roomescape` 폴더, `build.gradle.kts`가 있는 곳)에서:

```bash
# 1) DB 켜기 (볼륨 없음 → 컨테이너 지우면 데이터도 삭제)
docker compose up -d
```

- 앱: [http://localhost:8080](http://localhost:8080)
- Swagger: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

DB가 `healthy`가 된 뒤에 앱을 켜세요. 너무 빨리 켜면 연결 실패가 납니다.

```bash
docker compose ps
```

끌 때:

```bash
# 앱은 IDE/터미널에서 중지
docker compose down
```

볼륨이 없어서 `down` 하면 **회원·토큰 데이터가 전부 사라집니다.** 다시 `up` 하면 빈 DB + Flyway가 테이블을 새로 만듭니다.

## 설정 (.env)

`docker compose`는 같은 폴더의 `.env`를 읽습니다.  
스프링 앱은 `.env`를 **자동으로 읽지 않습니다.** 로컬은 `application.yaml` 기본값으로 DB에 붙습니다. 값이 같게 맞춰 두었습니다.


| 항목        | 값                                             |
| --------- | --------------------------------------------- |
| DB 이름     | `roomescape`                                  |
| 계정 / 비밀번호 | `roomescape` / `roomescape`                   |
| 포트        | `5432`                                        |
| JDBC      | `jdbc:postgresql://localhost:5432/roomescape` |


DB 계정이나 포트를 바꾸면 `.env`와 `application.yaml` **둘 다** 맞추거나, 실행 전에 환경 변수를 넣어야 합니다.

## 패키지 구조

기능은 도메인 폴더 안에 넣습니다.

```
com.team007.room_escape
  domain
    auth / member / festival / post / comment / like / inquiry
      controller   # API
      service      # 비즈니스 로직
      dto          # 요청/응답
      infra
        entity     # JPA
        repository # DB 조회
  global           # JWT, 시큐리티, 전역 에러, 공통 응답 (가급적 도메인 로직 넣지 않기)
```

새 API 예: 후기 작성이면 `domain.post.controller` → `service` → `repository` 순으로 가면 됩니다.

## API / 인증

공개되는 것:

- `/api/v1/auth/**` (로그인, 토큰 재발급, 로그아웃, 대충 공개할 것들)
- Swagger

나머지는 JWT가 필요합니다. 없으면 `401` (`AUTH200` 토큰 없음).


| 메서드  | 경로                     | 설명                                             |
| ---- | ---------------------- | ---------------------------------------------- |
| POST | `/api/v1/auth/login`   | 이메일 + 비밀번호. Access는 body, Refresh는 HttpOnly 쿠키 |
| POST | `/api/v1/auth/refresh` | 쿠키의 Refresh로 Access 재발급                        |
| POST | `/api/v1/auth/logout`  | Refresh 삭제 + 쿠키 제거                             |
| GET  | `/api/v1/members/me`   | 내 정보 (토큰 필요)                                   |


로그인 예시:

```http
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password"
}
```

성공하면 `data.accessToken`이 옵니다. 이후 요청:

```http
Authorization: Bearer {accessToken}
```

Swagger에서는 우측 상단 **Authorize**에 Access Token만 넣으면 됩니다. `Bearer ` 접두사는 자동으로 붙습니다.

**아직 회원가입 API는 없습니다.**  
로그인하려면 DB `member`에 행이 있어야 하고, `password`는 BCrypt 해시여야 합니다. 평문 `password`를 넣으면 로그인이 실패합니다.

## 권한

- DB `member.role`: `USER` 또는 `ADMIN`
- 시큐리티에서는 `ROLE_USER`, `ROLE_ADMIN`으로 봅니다.
- 로그인만 되면 되는 API는 `SecurityConfig`에서 이미 막고 있습니다.
- **중요!!!!!!!!!!!!!!!!**
- 관리자만 되면 컨트롤러에 `@PreAuthorize("hasRole('ADMIN')")` 을 붙입니다.
- 로그인한 사람 정보: `@AuthenticationPrincipal CustomUserDetails principal` → `principal.getId()` 등



## 응답 형식

성공/실패 모두 이 형태입니다.

```json
{
  "success": true,
  "code": "0000",
  "message": "",
  "data": {}
}
```

실패 예: `success: false`, `code`: `AUTH001` (이메일/비밀번호 틀림).  
코드 의미는 `global/response/code` 아래 enum 주석을 보면 됩니다.

## 스키마

테이블은 Flyway가 만듭니다. (`src/main/resources/db/migration`)

- JPA `ddl-auto`는 `validate`입니다. 엔티티와 DB가 다르면 앱이 안 뜹니다.
- 컬럼을 바꾸면 **엔티티 + 새** `V3__....sql` 둘 다 수정하세요. 이미 적용된 `V1` 파일을 고치면 checksum 에러가 납니다.
- 즉 엔티티 변경시 `resources`안에  `db/migration` 에다가 `Vn_~~~~.sql` 로 만들어달라는겁니다.
- 조회용 인덱스는 아직 없습니다. 유니크만 있습니다 (이메일, 좋아요 중복, 계정당 Refresh 1개).



## 자주 막히는 것


| 증상                   | 원인 / 대처                                                               |
| -------------------- | --------------------------------------------------------------------- |
| DB 연결 실패             | `docker compose up -d` 안 함. 또는 5432 포트가 이미 사용 중                       |
| Lombok 빨간 줄          | IDE Lombok 플러그인 + Annotation Processing 켜기                            |
| 로그인 401 `AUTH001`    | 회원이 없거나, 비밀번호가 BCrypt가 아님                                             |
| 다른 API 401 `AUTH200` | `Authorization: Bearer ...` 없음                                        |
| Flyway checksum      | 이미 실행된 마이그레이션 파일을 수정함. 로컬이면 `docker compose down` 후 다시 `up` (데이터 삭제됨) |
| Java 버전 오류           | JDK 25가 아님. `java -version` 확인                                        |




## 아직 안 된 것

- 회원가입
- 소셜 로그인
- 행사/후기/댓글 등 실제 비즈니스 API (컨트롤러 토대만 있음)
- 프로필 수정 후 토큰 재발급

