import { useAuth } from '@/features/auth/authContext'
import { ERROR_CODE } from '@/shared/api/errorCodes'
import { ApiError } from '@/shared/api/types'
import { Logo } from '@/shared/ui/Logo'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'

export default function LoginPage() {
  const { me, booting, login } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  // 이미 로그인된 상태로 들어오면 홈으로 돌려보낸다.
  useEffect(() => {
    if (!booting && me) {
      void navigate('/', { replace: true })
    }
  }, [booting, me, navigate])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(email, password)
      await navigate('/', { replace: true })
    } catch (e) {
      setError(toMessage(e))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto flex min-h-[calc(100vh-3.5rem)] max-w-sm flex-col justify-center px-4 py-10">
      <div className="mb-8 flex flex-col items-center gap-3">
        <Logo size="lg" />
        <p className="text-sm text-slate-500">행사를 탐색하고 후기를 남겨보세요</p>
      </div>

      <form
        onSubmit={handleSubmit}
        className="flex flex-col gap-3 rounded-2xl border border-sand-200 bg-white p-6"
      >
        <label className="flex flex-col gap-1.5 text-sm">
          <span className="font-medium text-slate-700">이메일</span>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="user@example.com"
            required
            autoComplete="email"
            className="rounded-lg border border-sand-200 px-3 py-2.5 outline-none focus:border-brand-400"
          />
        </label>

        <label className="flex flex-col gap-1.5 text-sm">
          <span className="font-medium text-slate-700">비밀번호</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="비밀번호"
            required
            autoComplete="current-password"
            className="rounded-lg border border-sand-200 px-3 py-2.5 outline-none focus:border-brand-400"
          />
        </label>

        {error && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={submitting}
          className="mt-2 rounded-lg bg-brand-500 py-2.5 font-medium text-white hover:bg-brand-600 disabled:opacity-60"
        >
          {submitting ? '로그인 중…' : '로그인'}
        </button>
      </form>

      <p className="mt-5 text-center text-sm text-slate-500">
        아직 계정이 없으신가요?{' '}
        <Link to="/signup" className="font-medium text-brand-600 hover:underline">
          회원가입
        </Link>
      </p>
    </div>
  )
}

/** 백엔드 에러 코드를 사용자에게 보여줄 문장으로 바꾼다. */
function toMessage(e: unknown): string {
  if (!(e instanceof ApiError)) {
    return '알 수 없는 오류가 발생했습니다.'
  }
  if (e.code === ERROR_CODE.INVALID_CREDENTIALS) {
    return '이메일 또는 비밀번호가 올바르지 않습니다.'
  }
  if (e.code === '') {
    // 응답 자체가 없는 경우 = 백엔드가 꺼져 있거나 CORS 차단
    return '서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인해 주세요.'
  }
  return e.message
}
