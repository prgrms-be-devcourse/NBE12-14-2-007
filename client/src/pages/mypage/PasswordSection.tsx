import { useAuth } from '@/features/auth/authContext'
import { changePassword, sendPasswordCode, verifyPasswordCode } from '@/features/member/api'
import { ApiError } from '@/shared/api/types'
import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'

/** 백엔드 @ValidPassword 와 동일한 규칙 */
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#%^&*]).*$/

/**
 * 비밀번호 변경. 백엔드가 세 단계로 나뉘어 있어 화면도 같은 순서로 진행한다.
 *
 *   idle     코드 받기 전
 *   sent     메일 발송됨 — 코드 입력 단계
 *   verified 코드 확인됨 — 새 비밀번호 입력 단계
 *
 * 코드와 새 비밀번호를 한 번에 받지 않는 이유는, 코드가 틀렸을 때
 * 비밀번호까지 다시 입력하게 만들지 않기 위해서다.
 */
type Step = 'idle' | 'sent' | 'verified'

export function PasswordSection() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  const [step, setStep] = useState<Step>('idle')
  const [code, setCode] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  function fail(e: unknown) {
    setError(e instanceof ApiError ? e.message : '알 수 없는 오류가 발생했습니다.')
  }

  async function handleSend() {
    setError(null)
    setBusy(true)
    try {
      await sendPasswordCode()
      setStep('sent')
      setNotice('가입한 이메일로 인증 코드를 보냈습니다. 유효시간은 2분입니다.')
    } catch (e) {
      fail(e)
    } finally {
      setBusy(false)
    }
  }

  async function handleVerify(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await verifyPasswordCode(code)
      setStep('verified')
      setNotice('인증되었습니다. 새 비밀번호를 입력해 주세요.')
    } catch (e) {
      fail(e)
    } finally {
      setBusy(false)
    }
  }

  async function handleChange(event: FormEvent) {
    event.preventDefault()

    if (password.length < 8 || password.length > 25) {
      setError('비밀번호는 8~25자여야 합니다.')
      return
    }
    if (!PASSWORD_PATTERN.test(password)) {
      setError('비밀번호는 영문, 숫자, 특수문자(!@#%^&*)를 모두 포함해야 합니다.')
      return
    }
    if (password !== passwordConfirm) {
      setError('비밀번호가 일치하지 않습니다.')
      return
    }

    setError(null)
    setBusy(true)
    try {
      await changePassword(password)
      // 서버가 Refresh Token 을 지웠으므로 현재 세션도 정리하고 다시 로그인시킨다.
      await logout()
      await navigate('/login', { replace: true })
    } catch (e) {
      fail(e)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="mt-5 rounded-xl border border-sand-200 bg-white p-6">
      <h2 className="font-semibold text-slate-900">비밀번호 변경</h2>
      <p className="mt-1 text-sm text-slate-500">
        본인 확인을 위해 가입한 이메일로 인증 코드를 보냅니다.
      </p>

      {step === 'idle' && (
        <button
          type="button"
          onClick={handleSend}
          disabled={busy}
          className="mt-4 rounded-lg border border-sand-200 px-4 py-2 text-sm hover:bg-sand-100 disabled:opacity-60"
        >
          {busy ? '보내는 중…' : '인증 코드 받기'}
        </button>
      )}

      {step === 'sent' && (
        <form onSubmit={handleVerify} className="mt-4 flex gap-2">
          <input
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
            placeholder="6자리 코드"
            inputMode="numeric"
            className="w-32 rounded-lg border border-sand-200 px-3 py-2 text-sm outline-none focus:border-brand-400"
          />
          <button
            type="submit"
            disabled={busy || code.length !== 6}
            className="rounded-lg bg-brand-500 px-4 py-2 text-sm text-white hover:bg-brand-600 disabled:opacity-60"
          >
            확인
          </button>
          <button
            type="button"
            onClick={handleSend}
            disabled={busy}
            className="rounded-lg border border-sand-200 px-3 py-2 text-sm text-slate-600 hover:bg-sand-100 disabled:opacity-60"
          >
            재발송
          </button>
        </form>
      )}

      {step === 'verified' && (
        <form onSubmit={handleChange} className="mt-4 flex flex-col gap-3">
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="새 비밀번호 (8~25자, 영문·숫자·특수문자)"
            autoComplete="new-password"
            className="rounded-lg border border-sand-200 px-3 py-2 text-sm outline-none focus:border-brand-400"
          />
          <input
            type="password"
            value={passwordConfirm}
            onChange={(e) => setPasswordConfirm(e.target.value)}
            placeholder="새 비밀번호 확인"
            autoComplete="new-password"
            className="rounded-lg border border-sand-200 px-3 py-2 text-sm outline-none focus:border-brand-400"
          />
          <button
            type="submit"
            disabled={busy}
            className="rounded-lg bg-brand-500 py-2.5 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-60"
          >
            {busy ? '변경 중…' : '비밀번호 변경'}
          </button>
          <p className="text-xs text-slate-400">
            변경하면 모든 기기에서 로그아웃되며 다시 로그인해야 합니다.
          </p>
        </form>
      )}

      {notice && !error && (
        <p className="mt-3 rounded-lg bg-sand-100 px-3 py-2 text-sm text-slate-600">{notice}</p>
      )}
      {error && (
        <p role="alert" className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      )}
    </section>
  )
}
