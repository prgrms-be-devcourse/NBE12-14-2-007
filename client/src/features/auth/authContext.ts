import type { MyPageInfo } from '@/features/member/types'
import { createContext, use } from 'react'

/**
 * 컴포넌트가 아닌 것(컨텍스트·훅)은 이 파일에 둔다.
 * AuthProvider.tsx 에 같이 두면 Fast Refresh 가 동작하지 않는다.
 */
export interface AuthValue {
  /** 로그인한 회원. 비로그인이면 null */
  me: MyPageInfo | null
  /** 첫 진입 시 세션 복구가 끝났는지. 끝나기 전에 화면을 그리면 깜빡인다 */
  booting: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  /** 프로필을 수정한 뒤 헤더 등에 최신 값을 반영하려면 호출한다. */
  refreshMe: () => Promise<void>
}

export const AuthContext = createContext<AuthValue | null>(null)

export function useAuth(): AuthValue {
  const value = use(AuthContext)
  if (!value) {
    throw new Error('useAuth 는 AuthProvider 안에서만 쓸 수 있습니다.')
  }
  return value
}
