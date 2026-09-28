// 실제 사용자 흐름을 흉내 낸 부하 시나리오.
// 대부분은 비로그인 조회이고, 일부만 로그인해서 내 정보를 본다.
// 메일 발송·이미지 업로드·공공 API 동기화처럼 외부 서비스를 부르는 API는 넣지 않는다.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:18080';
const PASSWORD = 'Loadtest1!';

export const options = {
  stages: [
    { duration: '30s', target: 10 },  // 워밍업 (JIT 컴파일 전 수치는 버린다)
    { duration: '1m', target: 30 },
    { duration: '1m', target: 50 },
    { duration: '1m', target: 100 },  // 여기서 버티는지가 관건
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
  },
};

// 테스트 전용 계정을 하나 만들고 로그인 토큰을 받아 둔다.
// 로그인마다 BCrypt가 돌아 CPU를 많이 먹으므로 VU마다 로그인하지 않는다.
export function setup() {
  const email = `loadtest-${Date.now()}@example.com`;
  const phone = `010${String(Math.floor(Math.random() * 1e8)).padStart(8, '0')}`;
  const json = { headers: { 'Content-Type': 'application/json' } };

  const signup = http.post(`${BASE_URL}/api/v1/auth/signup`,
    JSON.stringify({ email, password: PASSWORD, nickname: 'loadtest', phone }), json);
  check(signup, { 'signup 201': (r) => r.status === 201 });

  const login = http.post(`${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email, password: PASSWORD }), json);
  check(login, { 'login 200': (r) => r.status === 200 });

  const list = http.get(`${BASE_URL}/api/v1/festivals?size=50`);
  const content = list.json('data.content') || [];

  return {
    email,
    token: login.json('data.accessToken'),
    festivalIds: content.map((f) => f.festivalId),
  };
}

export default function (data) {
  const roll = Math.random();

  if (roll < 0.5) {
    // 메인 화면: 축제 목록
    const page = Math.floor(Math.random() * 3);
    const res = http.get(`${BASE_URL}/api/v1/festivals?page=${page}`,
      { tags: { name: 'festival-list' } });
    check(res, { 'festival list 200': (r) => r.status === 200 });
  } else if (roll < 0.8 && data.festivalIds.length > 0) {
    // 축제 상세 + 그 축제의 게시글
    const id = data.festivalIds[Math.floor(Math.random() * data.festivalIds.length)];
    const detail = http.get(`${BASE_URL}/api/v1/festivals/${id}`,
      { tags: { name: 'festival-detail' } });
    check(detail, { 'festival detail 200': (r) => r.status === 200 });

    const posts = http.get(`${BASE_URL}/api/v1/festivals/${id}/posts`,
      { tags: { name: 'festival-posts' } });
    check(posts, { 'festival posts 200': (r) => r.status === 200 });
  } else if (roll < 0.95) {
    // 커뮤니티 게시글 목록
    const res = http.get(`${BASE_URL}/api/v1/posts`, { tags: { name: 'post-list' } });
    check(res, { 'post list 200': (r) => r.status === 200 });
  } else {
    // 로그인 사용자: 내 정보 (JWT 검증 경로)
    const res = http.get(`${BASE_URL}/api/v1/members/me`, {
      headers: { Authorization: `Bearer ${data.token}` },
      tags: { name: 'member-me' },
    });
    check(res, { 'member me 200': (r) => r.status === 200 });
  }

  // 실제 사용자는 화면을 보는 시간이 있다.
  sleep(Math.random() * 2 + 1);
}
