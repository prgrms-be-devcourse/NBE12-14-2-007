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

## 설정 파일 (local / prod)

설정은 프로필로 나뉩니다. 로컬은 로컬 설정만, 운영은 운영 설정만 읽습니다.

| 파일 | 언제 | 내용 |
| --- | --- | --- |
| `application.yaml` | 항상 | 공통 설정 (JPA, 메일, JWT, R2, 배치 주기 …) |
| `application-local.yaml` | 로컬 (프로필 미지정 시 기본) | `.env` 로딩, 개발 DB 기본값, CORS `localhost:3000/3001`, 쿠키 `Secure=false` |
| `application-prod.yaml` | 운영 (Railway) | `.env` 안 읽음, 모든 값은 Railway Variables, `PORT` 사용, 쿠키 `Secure=true` |

- IDE나 `bootRun`으로 켜면 아무 설정 없이 **local**입니다.
- Docker 이미지로 뜨면 `Dockerfile`의 `SPRING_PROFILES_ACTIVE=prod` 때문에 **prod**입니다 (Railway, 부하 테스트).
- 로컬에서 운영 설정으로 켜 보고 싶으면 `SPRING_PROFILES_ACTIVE=prod` 환경 변수를 주면 됩니다. 이때는 `.env`를 읽지 않습니다.

### 운영(Railway) Variables

`application.yaml`의 공통 키(아래 표) + 이것들을 Railway 서비스 Variables에 넣습니다.

| 키 | 값 |
| --- | --- |
| `DATASOURCE_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
| `DATASOURCE_USERNAME` | `${{Postgres.PGUSER}}` |
| `DATASOURCE_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
| `CORS_ALLOWED_ORIGINS` | (선택) 쉼표 구분. 프론트가 Vercel rewrite로 붙으면 필요 없음 |

`${{Postgres.…}}`는 Railway의 참조 변수 문법입니다. DB 서비스 이름이 `Postgres`가 아니면 그 이름으로 바꿉니다.

## 설정 (.env)

로컬에서는 `.env` 파일 하나로 **`docker compose`와 스프링 앱이 모두** 설정을 읽습니다.
`application-local.yaml`의 이 설정 덕분입니다. 운영(prod)에서는 읽지 않습니다.

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
```

> ⚠️ `.env`는 `.gitignore`에 있어서 **git으로 공유되지 않습니다.**
> clone 후 직접 만들어야 앱이 뜹니다. (플레이스홀더에 기본값이 없어 `.env`가 없으면 기동 실패)

### 필요한 키

| 키 | 용도 |
| --- | --- |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_PORT` | docker compose가 사용 |
| `DATASOURCE_URL` / `DATASOURCE_USERNAME` / `DATASOURCE_PASSWORD` | 스프링 DB 접속. 로컬은 생략하면 아래 기본 DB 설정 |
| `JWT_SECRET` / `JWT_REFRESH_SECRET` | 토큰 서명 키 |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Gmail 발송 계정 (앱 비밀번호) |
| `PUBLIC_FESTIVAL_SERVICE_KEY` | 공공 행사 API 키 |
| `KMA_WEATHER_SERVICE_KEY` | 기상청 날씨 API 키 |
| `R2_ACCOUNT_ID` / `R2_ACCESS_KEY` / `R2_SECRET_KEY` / `R2_BUCKET` / `R2_PUBLIC_URL` | 이미지 저장소 (Cloudflare R2) |

기본 DB 설정은 이렇습니다.

| 항목        | 값                                             |
| --------- | --------------------------------------------- |
| DB 이름     | `roomescape`                                  |
| 계정 / 비밀번호 | `roomescape` / `roomescape`                   |
| 포트        | `5432`                                        |
| JDBC      | `jdbc:postgresql://localhost:5432/roomescape` |

### 작성 시 주의

- **따옴표를 쓰지 마세요.** properties 형식이라 `A="b"` 는 따옴표까지 값이 됩니다
- `=` 앞뒤에 공백을 넣지 마세요
- `R2_PUBLIC_URL` 끝에 슬래시(`/`)를 붙이지 마세요
- **실행 위치가 프로젝트 루트여야** `.env`를 찾습니다. IDE에서 안 읽히면 Run Configuration의 Working directory를 확인하세요
- OS 환경 변수가 있으면 `.env` 값보다 우선합니다

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
| POST | `/api/v1/auth/signup`  | 이메일 회원가입. 성공 시 로그인과 같이 Access + Refresh 발급 |
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

회원가입 예시:

```http
POST http://localhost:8080/api/v1/auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password1!",
  "nickname": "닉네임",
  "phone": "01012345678"
}
```

`phone`은 생략 가능합니다. 권한은 `ROLE_UNVERIFIED`로 저장되고, 비밀번호는 BCrypt로 해시합니다. 이미 있는 이메일이면 `409` (`MEMBER001`).

## 권한

권한은 `member.role` 컬럼 하나로 관리합니다. **신뢰 등급과 시큐리티 권한을 겸하며**, 위 등급은 아래 등급의 권한을 모두 포함합니다.

```
ROLE_ADMIN        관리자
    ↑
ROLE_TRUSTED      여러 차례 정상 개최
    ↑
ROLE_NORMAL       정상적인 행사 개최 이력
    ↑
ROLE_UNVERIFIED   신규 주최자 (가입 기본값)
    ↑
ROLE_WARNING      허위/미개최/중대한 신고
```

| 등급 | 의미 | 부여 방식 |
| --- | --- | --- |
| `ROLE_WARNING` | 허위/미개최/중대한 신고 누적 | 자동 (제재) |
| `ROLE_UNVERIFIED` | 신규 주최자 — **가입 시 기본값** | 자동 |
| `ROLE_NORMAL` | 정상적인 행사 개최 이력 보유 | 자동 |
| `ROLE_TRUSTED` | 여러 차례 정상 개최 | 자동 |
| `ROLE_ADMIN` | 관리자 | 수동 (DB 직접 변경) |

### 컨트롤러에 거는 법

```java
@PreAuthorize("hasRole('ADMIN')")     // ADMIN만
@PreAuthorize("hasRole('TRUSTED')")   // TRUSTED, ADMIN
@PreAuthorize("hasRole('NORMAL')")    // NORMAL, TRUSTED, ADMIN
```

- 계층이 `SecurityConfig.roleHierarchy()`에 선언돼 있어서 **상위 등급을 일일이 나열할 필요가 없습니다.** `hasAnyRole('TRUSTED', 'ADMIN')` 대신 `hasRole('TRUSTED')` 하나면 됩니다.
- `hasRole`에는 `ROLE_` 접두사를 **빼고** 씁니다. `hasRole('ROLE_ADMIN')`으로 쓰면 `ROLE_ROLE_ADMIN`을 찾아서 **항상 실패**합니다.
- 로그인만 필요한 API는 따로 붙일 게 없습니다. `SecurityConfig`의 `anyRequest().authenticated()`가 이미 막고 있습니다.
- 권한 부족이면 `403` + `AUTH100`이 내려갑니다.

### ⚠️ `ROLE_WARNING`은 게이트로 쓰면 안 됩니다

최하위 등급이라 `hasRole('WARNING')`은 **로그인한 모두가 통과**합니다. 제재는 반대로 최소 등급을 요구하는 방식으로 겁니다.

```java
@PreAuthorize("hasRole('WARNING')")   // ❌ 전원 통과 (의미 없음)
@PreAuthorize("hasRole('NORMAL')")    // ✅ WARNING, UNVERIFIED 차단
```

### 서비스 코드에서 등급 비교

`RoleHierarchy`는 시큐리티 표현식에만 적용됩니다. 자바 코드에서는 아래를 쓰세요.

```java
member.hasPrivilegeOf(MemberRole.ROLE_TRUSTED);  // 계층 반영 (TRUSTED 이상이면 true)
MemberRole.ROLE_ADMIN.includes(MemberRole.ROLE_NORMAL);  // enum 직접 비교
principal.isAdmin();                             // ROLE_ADMIN 정확히 일치할 때만 true
```

### 등급 변경

```java
member.applyTrustGrade(MemberRole.ROLE_NORMAL);  // 자동 재계산용. ADMIN은 보호되어 안 바뀜
member.changeRole(MemberRole.ROLE_ADMIN);        // 관리자가 직접 지정할 때만
```

### 로그인한 사용자 정보

```java
@AuthenticationPrincipal CustomUserDetails principal
// principal.getId(), principal.getNickname(), principal.getRole()
```

### 등급을 추가하거나 순서를 바꿀 때

**아래 세 곳을 반드시 함께** 고쳐야 합니다. 하나라도 빠지면 컴파일은 되는데 권한만 조용히 어긋납니다.

1. `MemberRole` enum — **선언 순서가 곧 계층 순서**입니다
2. `SecurityConfig.roleHierarchy()` — enum 순서와 동일하게
3. `member` 테이블의 `ck_member_role` 제약 — 새 마이그레이션 파일로 (`V9` 참고)

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

## 이미지 (Cloudflare R2)

### 핵심 규칙 — DB엔 `key`, 응답엔 `url`

| | 예시 | 어디에 |
| --- | --- | --- |
| `key` | `posts/0befc150-....png` | **DB에 저장** |
| `url` | `https://pub-xxxx.r2.dev/posts/0befc150-....png` | **응답으로 내보냄** |

```
url = R2_PUBLIC_URL + "/" + key
      └ 설정값 1곳 ┘   └ DB 저장 ┘
```

URL을 통째로 저장하면 도메인을 바꿀 때 **모든 테이블을 UPDATE** 해야 합니다.
`key`만 저장하면 `.env` 한 줄만 고치면 되고, 로컬/운영이 같은 데이터로 각자 다른 URL을 씁니다.

### 업로드 흐름

```
1) 프론트: POST /api/v1/images?type=POST   (multipart, 파트명 "file")
          → { "key": "posts/abc.png", "url": "https://..." }

2) 프론트: url 로 미리보기, key 는 들고 있기

3) 프론트: 실제 등록 요청에 key 를 담아 전송
          { "title": "...", "thumbnail": "posts/abc.png" }

4) 서버:   받은 key 를 그대로 엔티티에 저장
```

`type` 은 `POST` / `PROFILE` / `INQUIRY` / `FESTIVAL` 중 하나이며, 버킷 안의 폴더가 됩니다.

### 응답에 이미지를 담는 법

`ImageUrlResolver` 를 주입해서 `key` 를 `url` 로 바꿔 내보냅니다.

```java
@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final ImageUrlResolver imageUrlResolver;   // ← 이것만 주입

	@Transactional(readOnly = true)
	public MemberResponse.MyPageInfo getMyPage(UUID memberId) {
		Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));

		return MemberResponse.MyPageInfo.from(member, imageUrlResolver.resolve(member.getProfileImg()));
	}
}
```

`resolve()` 는 `key` 가 `null` 이거나 비어 있으면 `null` 을 돌려줍니다. 이미지가 없는 경우를 따로 분기할 필요가 없습니다.

### ⚠️ URL만 필요하면 `R2StorageService` 를 주입하지 마세요

| 클래스 | 하는 일 | 부수효과 | 언제 |
| --- | --- | --- | --- |
| `ImageUrlResolver` | `key` → `url` | 없음 (순수 변환) | 이미지를 **보여줄 때** |
| `R2StorageService` | `upload()` / `delete()` | R2 네트워크 호출 | 이미지를 **다룰 때** |

`R2StorageService` 를 주입하면 `S3Client` 까지 딸려와서, 도메인 서비스가 인프라에 묶이고 테스트할 때 mock이 필요해집니다. 조회만 한다면 리졸버로 충분합니다.

### 이미지를 교체·삭제할 때

R2는 파일을 자동으로 지워주지 않습니다. **기존 `key` 로 직접 지워야** 쓰레기 파일이 안 쌓입니다.

```java
String oldKey = member.getProfileImg();
member.changeProfileImg(newKey);
r2StorageService.delete(oldKey);   // 삭제 실패는 로그만 남고 흐름을 막지 않는다
```

## 스키마

테이블은 Flyway가 만듭니다. (`src/main/resources/db/migration`)

- JPA `ddl-auto`는 `validate`입니다. 엔티티와 DB가 다르면 앱이 안 뜹니다.
- 컬럼을 바꾸면 **엔티티 + 새** `V3__....sql` 둘 다 수정하세요. 이미 적용된 `V1` 파일을 고치면 checksum 에러가 납니다.
- 즉 엔티티 변경시 `resources`안에  `db/migration` 에다가 `Vn_~~~~.sql` 로 만들어달라는겁니다.
- 조회용 인덱스는 아직 없습니다. 유니크만 있습니다 (이메일, 좋아요 중복, 계정당 Refresh 1개).

### 시드 데이터 (`db/seed`)

데모 회원·행사 같은 **데이터만 넣는 SQL은 `db/seed`** 에 둡니다. 스키마(테이블·컬럼·제약)는 `db/migration` 입니다.

| 폴더 | local | prod |
| --- | --- | --- |
| `db/migration` | ✅ | ✅ |
| `db/seed` | ✅ | ❌ |

- 운영 DB에 데모 계정(관리자 포함)이 들어가지 않게 하려고 나눴습니다. 설정은 `application-local.yaml`의 `spring.flyway.locations`.
- 버전 번호는 두 폴더가 **같은 순번을 공유**합니다. 새 시드를 만들 때도 `db/migration`의 마지막 번호 다음 번호를 쓰세요 (겹치면 Flyway가 기동 실패).
- 시드 SQL에는 `CREATE`/`ALTER` 같은 스키마 변경을 넣지 마세요. 운영에는 안 돌아서 로컬과 스키마가 달라집니다.



## 부하 테스트

Railway 무료 플랜과 같은 자원 한도(1 vCPU / 512MB)로 로컬에서 부하 테스트를 합니다.
실행 방법과 결과 보는 법은 [loadtest/README.md](loadtest/README.md)를 보세요.

```bash
docker compose -f docker-compose.loadtest.yml up -d --build
docker compose -f docker-compose.loadtest.yml run --rm k6
```

## 자주 막히는 것


| 증상                   | 원인 / 대처                                                               |
| -------------------- | --------------------------------------------------------------------- |
| 기동 즉시 `Could not resolve placeholder` | `.env` 가 없거나 키가 빠짐. 루트에 `.env` 생성 (git으로 공유 안 됨)             |
| 이미지 URL이 `null`      | DB에 저장된 `key` 가 비어 있음. 업로드 응답의 `key` 를 저장했는지 확인            |
| DB 연결 실패             | `docker compose up -d` 안 함. 또는 5432 포트가 이미 사용 중                       |
| Lombok 빨간 줄          | IDE Lombok 플러그인 + Annotation Processing 켜기                            |
| 로그인 401 `AUTH001`    | 회원이 없거나, 비밀번호가 BCrypt가 아님                                             |
| 다른 API 401 `AUTH200` | `Authorization: Bearer ...` 없음                                        |
| API 403 `AUTH100`    | 등급이 부족함. `member.role` 확인. 계층은 `SecurityConfig.roleHierarchy()` 참고            |
| Flyway checksum      | 이미 실행된 마이그레이션 파일을 수정함. 로컬이면 `docker compose down` 후 다시 `up` (데이터 삭제됨) |
| Java 버전 오류           | JDK 25가 아님. `java -version` 확인                                        |




## 아직 안 된 것

- 소셜 로그인
- 행사/후기/댓글 등 실제 비즈니스 API (컨트롤러 토대만 있음)
- 프로필 수정 후 토큰 재발급
- 신뢰 등급 자동 승강 (현재는 전원 `ROLE_UNVERIFIED`로 가입, 변경은 수동)

