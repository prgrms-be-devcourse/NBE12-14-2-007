import { client } from '@/shared/api/client'
import type { MyPageInfo, UpdateMyPageRequest } from './types'

/**
 * 마이페이지 조회.
 * 회원 식별자를 보내지 않는다. 서버가 토큰에서 꺼내므로 남의 정보는 조회할 수 없다.
 */
export async function getMyPage(): Promise<MyPageInfo> {
  return client.get<unknown, MyPageInfo>('/api/v1/members/me')
}

/**
 * 마이페이지 수정. 보낸 필드만 반영된다.
 * profileImg 에는 업로드 API 가 돌려준 key 를 넣는다. 공개 URL 이 아니다.
 */
export async function updateMyPage(request: UpdateMyPageRequest): Promise<MyPageInfo> {
  return client.patch<unknown, MyPageInfo>('/api/v1/members/me', request)
}

/**
 * 비밀번호 변경 1단계. 가입 이메일로 6자리 인증 코드를 보낸다.
 * 받는 주소는 보내지 않는다. 서버가 DB 에서 읽으므로 남의 계정 코드를 받을 수 없다.
 * 재발송은 60초 뒤부터 가능하다(AUTH303).
 */
export async function sendPasswordCode(): Promise<void> {
  await client.post('/api/v1/members/me/password/verification-code')
}

/** 2단계. 코드가 맞는지만 확인한다. 통과하면 5분간 인증 상태가 유지된다. */
export async function verifyPasswordCode(code: string): Promise<void> {
  await client.post('/api/v1/members/me/password/verify', { code })
}

/**
 * 3단계. 인증을 통과한 상태에서 새 비밀번호를 설정한다.
 * 성공하면 서버가 Refresh Token 을 지우므로 다시 로그인해야 한다.
 */
export async function changePassword(newPassword: string): Promise<void> {
  await client.patch('/api/v1/members/me/password', { newPassword })
}
