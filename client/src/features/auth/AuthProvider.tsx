import { login as loginApi, logout as logoutApi, refresh } from '@/features/auth/api'
import { AuthContext } from '@/features/auth/authContext'
import { getMyPage } from '@/features/member/api'
import type { MyPageInfo } from '@/features/member/types'
import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<MyPageInfo | null>(null)
  const [booting, setBooting] = useState(true)

  // Access Token 은 메모리에만 있어서 새로고침하면 사라진다.
  // HttpOnly 쿠키로 refresh 를 호출해 세션을 복구한다.
  // 실패는 "로그인 안 된 상태"라는 정상적인 결과이므로 조용히 넘어간다.
  useEffect(() => {
    refresh()
      .then(() => getMyPage())
      .then(setMe)
      .catch(() => setMe(null))
      .finally(() => setBooting(false))
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    await loginApi({ email, password })
    setMe(await getMyPage())
  }, [])

  const logout = useCallback(async () => {
    await logoutApi()
    setMe(null)
  }, [])

  const refreshMe = useCallback(async () => {
    setMe(await getMyPage())
  }, [])

  const value = useMemo(
    () => ({ me, booting, login, logout, refreshMe }),
    [me, booting, login, logout, refreshMe],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}
