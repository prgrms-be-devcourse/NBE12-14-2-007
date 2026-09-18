/**
 * Access Token 보관소.
 *
 * localStorage 가 아니라 모듈 변수(메모리)에 둔다.
 * localStorage 에 넣으면 XSS 로 토큰이 통째로 털린다.
 *
 * 메모리라 새로고침하면 사라지는데, 그건 앱 시작 시 /auth/refresh 를 한 번 호출해
 * 복구한다. Refresh Token 은 HttpOnly 쿠키라 JS 가 읽을 수 없고, 그게 정상이다.
 */
let accessToken: string | null = null

export const tokenStore = {
  get(): string | null {
    return accessToken
  },

  set(token: string): void {
    accessToken = token
  },

  clear(): void {
    accessToken = null
  },
}
