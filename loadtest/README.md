# 부하 테스트

Railway 무료 플랜과 **같은 자원 한도(1 vCPU / 512MB)** 로 앱과 DB를 로컬 Docker에 띄우고, [k6](https://k6.io)로 부하를 줍니다.
Railway에서 직접 부하 테스트를 하면 크레딧이 빠르게 소모되고 서버가 죽을 수 있어서 로컬에서 합니다.

## 필요한 것

- Docker Desktop
- 루트의 `.env` (앱 실행용과 같은 파일)

k6는 Docker 이미지로 돌아가서 따로 설치하지 않아도 됩니다.

## 실행

프로젝트 루트에서:

```bash
# 1) 앱 + DB 기동 (처음엔 이미지 빌드로 몇 분 걸림)
docker compose -f docker-compose.loadtest.yml up -d --build

# 2) 앱이 떴는지 확인. "Started RoomescapeApplication" 이 보이면 준비 완료 (약 30초)
docker compose -f docker-compose.loadtest.yml logs -f app

# 3) 부하 테스트 실행 (약 4분)
docker compose -f docker-compose.loadtest.yml run --rm k6

# 4) 정리 (볼륨 없음 → 테스트 데이터 삭제)
docker compose -f docker-compose.loadtest.yml down
```

테스트 중에는 다른 터미널에서 자원 사용량을 같이 봅니다.

```bash
docker stats roomescape-loadtest-app-1 roomescape-loadtest-db-1
```

- 앱: [http://localhost:18080](http://localhost:18080) (개발용 8080과 겹치지 않게 18080)
- DB는 외부 포트를 열지 않습니다. 개발용 `docker compose` DB(5432)와 따로 돌아갑니다.

## 자원 한도 바꾸기

기본값은 서비스당 **1 vCPU / 512MB, 스왑 없음** 입니다.
Railway 대시보드의 서비스 → Settings → Resource Limits 값이 다르면 `.env`에 추가해서 맞춥니다.

```properties
LOADTEST_CPUS=2
LOADTEST_MEMORY=1g
```

스왑을 막아둔 이유: 스왑으로 버티면 실제 Railway보다 결과가 좋게 나옵니다.

## 시나리오 (`scenario.js`)

실제 사용자 비율을 흉내 냅니다. 요청 사이에 1~3초 쉽니다 (화면 보는 시간).

| 비율 | 동작 | API |
| --- | --- | --- |
| 50% | 메인 축제 목록 | `GET /api/v1/festivals?page=0~2` |
| 30% | 축제 상세 + 해당 축제 게시글 | `GET /api/v1/festivals/{id}`, `GET /api/v1/festivals/{id}/posts` |
| 15% | 커뮤니티 게시글 목록 | `GET /api/v1/posts` |
| 5% | 내 정보 (JWT 검증) | `GET /api/v1/members/me` |

동시 사용자(VU)는 이렇게 늘어납니다.

```
10명 (30초, 워밍업) → 30명 (1분) → 50명 (1분) → 100명 (1분) → 0명 (30초)
```

합격 기준 (`thresholds`). 하나라도 넘으면 k6가 실패로 끝납니다.

- 에러율 1% 미만
- 응답시간 p95 800ms 미만

`BASE_URL` 환경 변수로 대상 서버를 바꿀 수 있습니다. 기본값은 compose 안의 `http://app:8080` 입니다.

### 일부러 뺀 것

외부 서비스를 부르거나 비용이 드는 API는 넣지 않았습니다.

- 이메일 인증 코드 발송 — Gmail 하루 발송 한도
- 이미지 업로드 — R2 저장 용량
- 공공 행사 동기화 — 공공 API 호출 한도

로그인도 `setup()`에서 **딱 한 번**만 합니다. 로그인마다 BCrypt가 돌아서 CPU를 많이 먹기 때문에, VU마다 로그인하면 실제와 다른 병목이 생깁니다.
테스트 계정은 `loadtest-{시각}@example.com` 으로 매번 새로 만듭니다.

## JVM 메모리 설정

512MB 컨테이너에서 JVM 기본값으로 띄우면 **기동 중에 OOMKilled** 됩니다 (exit 137).
자바 힙 부족이 아니라 힙 + 힙 바깥 메모리(메타스페이스, JIT 코드 캐시, 스레드 스택)를 합친 전체가 한도를 넘기 때문입니다.

그래서 루트 `Dockerfile`의 `JAVA_TOOL_OPTIONS`로 줄여 두었습니다.

| 옵션 | 의미 |
| --- | --- |
| `-XX:MaxRAMPercentage=50` | 힙은 컨테이너 메모리의 절반 (512MB면 256MB) |
| `-XX:MaxMetaspaceSize=160m` | 클래스 메타데이터 상한 |
| `-XX:ReservedCodeCacheSize=64m` | JIT 컴파일 코드 상한 (기본 240MB) |
| `-XX:MaxDirectMemorySize=32m` | NIO 다이렉트 버퍼 상한 |
| `-Xss512k` | 스레드 스택 (기본 1MB). Tomcat 스레드가 많아져도 덜 먹음 |
| `-XX:+UseSerialGC` | CPU 1개에선 G1보다 부가 메모리가 적음 |
| `-XX:+ExitOnOutOfMemoryError` | OOM 시 좀비로 남지 않고 죽어서 Railway가 재시작 |

이 `Dockerfile`은 **Railway 배포에도 그대로 쓰입니다.** 로컬에서 버티면 Railway에서도 같은 조건입니다.

## 결과 보는 법

k6가 끝나면 요약이 나옵니다. 이 줄들을 보면 됩니다.

```
http_req_failed......: 0.00%       ← 에러율
http_req_duration....: p(95)=5.77ms ← 95%의 요청이 이 시간 안에 응답
http_reqs............: 6299        ← 총 요청 수
✓ 'p(95)<800'                       ← 합격 기준 통과 여부 (✗ 면 실패)
```

`docker stats`에서는 **앱 메모리가 한도에 얼마나 붙는지**가 제일 중요합니다.
한도에 닿으면 Railway에서도 앱이 재시작됩니다.

### 기준 결과 (2026-09-28, 시드 데이터만 있는 상태)

| 항목 | 값 |
| --- | --- |
| 총 요청 | 6,299건 (최대 100 VU) |
| 에러율 | 0% |
| p95 응답시간 | 5.8ms |
| 앱 메모리 (대기 → 최대) | 462MB → **508MB / 512MB** |
| 앱 재시작 | 없음 |

응답은 빠르지만 **메모리 여유가 거의 없습니다.**
데이터가 쌓이면 응답이 느려지고 메모리도 더 먹으니, 기능을 추가한 뒤에는 다시 돌려서 이 표와 비교하세요.

## 자주 막히는 것

| 증상 | 원인 / 대처 |
| --- | --- |
| `app` 컨테이너가 사라짐, exit 137 | 메모리 한도 초과 (OOMKilled). `docker inspect roomescape-loadtest-app-1 --format "{{.State.OOMKilled}}"` 로 확인 |
| k6가 `connection refused` | 앱이 아직 기동 중. `logs -f app`에서 `Started` 확인 후 실행 |
| `signup 201` 체크 실패 | 회원가입 검증 규칙이 바뀜. `scenario.js`의 가입 요청 본문 확인 |
| 축제 상세 요청이 안 나감 | DB에 축제가 없음. 시드 마이그레이션(`V25`) 적용 여부 확인 |
| 결과가 매번 크게 다름 | 다른 프로그램이 CPU를 쓰는 중. 테스트 중엔 IDE 빌드 등을 멈추세요 |
