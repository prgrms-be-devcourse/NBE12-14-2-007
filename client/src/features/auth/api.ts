import { client } from '@/shared/api/client'
import { tokenStore } from '@/shared/api/tokenStore'
import type { LoginRequest, SignupRequest, TokenResponse } from './types'

/** 화면에서 axios 를 직접 부르지 말고 이 파일을 거친다. */

export async function signup(request: SignupRequest): Promise<void> {
  await client.post('/api/v1/auth/signup', request)
}

export async function login(request: LoginRequest): Promise<TokenResponse> {
  const token = await client.post<unknown, TokenResponse>('/api/v1/auth/login', request)
  tokenStore.set(token.accessToken)
  return token
}

/**
 * 새로고침 후 세션 복구용.
 * Access Token 은 메모리에만 있어서 새로고침하면 사라지므로, 앱 시작 시 한 번 호출한다.
 * 쿠키가 없거나 만료면 실패하는데, 그건 "로그인 안 된 상태"라는 정상적인 결과다.
 */
export async function refresh(): Promise<TokenResponse> {
  const token = await client.post<unknown, TokenResponse>('/api/v1/auth/refresh')
  tokenStore.set(token.accessToken)
  return token
}

/** 로그아웃은 204(본문 없음)로 응답한다. 봉투 구조를 쓰지 않는 유일한 API다. */
export async function logout(): Promise<void> {
  try {
    await client.post('/api/v1/auth/logout')
  } finally {
    // 서버 요청이 실패해도 클라이언트 토큰은 반드시 버린다.
    tokenStore.clear()
  }
}
