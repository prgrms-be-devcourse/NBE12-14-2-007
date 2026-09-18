import { client } from '@/shared/api/client'
import type { MyPageInfo } from './types'

/**
 * 마이페이지 조회.
 * 회원 식별자를 보내지 않는다. 서버가 토큰에서 꺼내므로 남의 정보는 조회할 수 없다.
 */
export async function getMyPage(): Promise<MyPageInfo> {
  return client.get<unknown, MyPageInfo>('/api/v1/members/me')
}
