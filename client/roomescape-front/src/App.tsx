import { login, logout, refresh } from '@/features/auth/api'
import { getMyPage } from '@/features/member/api'
import type { MyPageInfo } from '@/features/member/types'
import { ApiError } from '@/shared/api/types'
import { useEffect, useState, type FormEvent } from 'react'

/**
 * 인증 흐름 검증용 화면.
 * 라우팅·화면 분리는 Phase 1에서 pages/ 로 옮긴다.
 */
export default function App() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [me, setMe] = useState<MyPageInfo | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [booting, setBooting] = useState(true)

  // 새로고침하면 메모리의 Access Token 이 사라진다.
  // 쿠키로 refresh 를 시도해 세션을 복구한다. 실패하면 로그인 안 된 상태로 둔다.
  useEffect(() => {
    refresh()
      .then(() => getMyPage())
      .then(setMe)
      .catch(() => setMe(null))
      .finally(() => setBooting(false))
  }, [])

  function describe(e: unknown): string {
    return e instanceof ApiError ? `[${e.code}] ${e.message}` : '알 수 없는 오류'
  }

  async function handleLogin(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login({ email, password })
      setMe(await getMyPage())
    } catch (e) {
      setError(describe(e))
    } finally {
      setLoading(false)
    }
  }

  async function handleLogout() {
    await logout()
    setMe(null)
    setError(null)
  }

  /** 토큰이 살아있는지 다시 확인. 30분 뒤 눌러 보면 자동 재발급이 도는지 볼 수 있다. */
  async function handleReload() {
    setError(null)
    try {
      setMe(await getMyPage())
    } catch (e) {
      setError(describe(e))
      setMe(null)
    }
  }

  if (booting) {
    return <main className="p-8 text-slate-500">세션 확인 중…</main>
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-md flex-col justify-center gap-6 p-6">
      <h1 className="text-2xl font-bold text-slate-900">로그인 검증</h1>

      {me ? (
        <section className="flex flex-col gap-4 rounded-lg border border-slate-200 p-5">
          <p className="font-semibold text-slate-900">로그인됨</p>
          <dl className="grid grid-cols-[6rem_1fr] gap-y-1 text-sm text-slate-700">
            <dt className="text-slate-500">닉네임</dt>
            <dd>{me.nickname}</dd>
            <dt className="text-slate-500">이메일</dt>
            <dd className="break-all">{me.email}</dd>
            <dt className="text-slate-500">등급</dt>
            <dd>{me.role}</dd>
            <dt className="text-slate-500">연락처</dt>
            <dd>{me.phone ?? '-'}</dd>
            <dt className="text-slate-500">가입일</dt>
            <dd>{me.createdAt.slice(0, 10)}</dd>
          </dl>

          <div className="flex gap-2">
            <button
              type="button"
              onClick={handleReload}
              className="flex-1 rounded border border-slate-300 py-2 text-sm hover:bg-slate-50"
            >
              내 정보 다시 조회
            </button>
            <button
              type="button"
              onClick={handleLogout}
              className="flex-1 rounded bg-slate-900 py-2 text-sm text-white hover:bg-slate-700"
            >
              로그아웃
            </button>
          </div>
        </section>
      ) : (
        <form onSubmit={handleLogin} className="flex flex-col gap-3">
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="이메일"
            required
            className="rounded border border-slate-300 px-3 py-2"
          />
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="비밀번호"
            required
            className="rounded border border-slate-300 px-3 py-2"
          />
          <button
            type="submit"
            disabled={loading}
            className="rounded bg-slate-900 py-2 text-white hover:bg-slate-700 disabled:opacity-50"
          >
            {loading ? '로그인 중…' : '로그인'}
          </button>
        </form>
      )}

      {error && <p className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
    </main>
  )
}
