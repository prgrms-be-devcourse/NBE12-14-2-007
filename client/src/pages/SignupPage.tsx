import { signup } from '@/features/auth/api'
import { useAuth } from '@/features/auth/authContext'
import { uploadImage, validateImage } from '@/features/image/api'
import { ERROR_CODE } from '@/shared/api/errorCodes'
import { ApiError } from '@/shared/api/types'
import { formatPhone, isValidPhone, toDigits } from '@/shared/lib/phone'
import { Logo } from '@/shared/ui/Logo'
import { useEffect, useMemo, useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'

/**
 * 백엔드 @ValidPassword 와 동일한 규칙.
 * 서버에서도 검증하지만, 제출 전에 알려줘야 왕복 한 번을 아낀다.
 */
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#%^&*]).*$/

export default function SignupPage() {
  const { me, booting, login } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [nickname, setNickname] = useState('')
  const [phone, setPhone] = useState('')
  const [profileFile, setProfileFile] = useState<File | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (!booting && me) {
      void navigate('/', { replace: true })
    }
  }, [booting, me, navigate])

  // 미리보기 주소는 파일에서 바로 파생시킨다. 상태로 들고 있으면 렌더가 한 번 더 돈다.
  const profilePreview = useMemo(
    () => (profileFile ? URL.createObjectURL(profileFile) : null),
    [profileFile],
  )

  // createObjectURL 로 만든 주소는 직접 해제하지 않으면 메모리에 남는다.
  useEffect(() => {
    if (!profilePreview) {
      return
    }
    return () => URL.revokeObjectURL(profilePreview)
  }, [profilePreview])

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) {
      return
    }
    const invalid = validateImage(file)
    if (invalid) {
      setError(invalid)
      event.target.value = ''
      return
    }
    setError(null)
    setProfileFile(file)
  }

  function clearFile() {
    setProfileFile(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()

    const invalid = validate({ password, passwordConfirm, nickname, phone })
    if (invalid) {
      setError(invalid)
      return
    }

    setError(null)
    setSubmitting(true)
    try {
      // 업로드 API 는 비로그인도 허용하므로 가입 전에 먼저 올려 key 를 받아둔다.
      // 이렇게 해야 계정이 만들어지는 시점에 프로필까지 한 번에 채워진다.
      let profileImg: string | undefined
      if (profileFile) {
        console.log('[가입] 1/3 이미지 업로드 시작', {
          name: profileFile.name,
          type: profileFile.type,
          size: profileFile.size,
        })
        const uploaded = await uploadImage(profileFile, 'PROFILE')
        profileImg = uploaded.key
        console.log('[가입] 1/3 이미지 업로드 완료', uploaded)
      } else {
        console.log('[가입] 1/3 이미지 없음 — 건너뜀')
      }

      const payload = {
        email,
        password,
        nickname,
        // 화면에는 하이픈이 보여도 서버에는 숫자만 보낸다. 안 적었으면 아예 빼서 보낸다.
        phone: toDigits(phone) || undefined,
        profileImg,
      }
      console.log('[가입] 2/3 회원가입 요청', { ...payload, password: '***' })
      await signup(payload)
      console.log('[가입] 2/3 회원가입 완료')

      // 가입 직후 바로 로그인시켜 다시 입력하게 만들지 않는다.
      console.log('[가입] 3/3 자동 로그인 시작')
      await login(email, password)
      console.log('[가입] 3/3 자동 로그인 완료 — 홈으로 이동')

      await navigate('/', { replace: true })
    } catch (e) {
      console.error('[가입] 실패', e)
      setError(toMessage(e))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto flex min-h-[calc(100vh-3.5rem)] max-w-sm flex-col justify-center px-4 py-10">
      <div className="mb-8 flex flex-col items-center gap-3">
        <Logo size="lg" />
        <p className="text-sm text-slate-500">회원가입하고 행사 후기를 남겨보세요</p>
      </div>

      <form
        onSubmit={handleSubmit}
        className="flex flex-col gap-3 rounded-2xl border border-sand-200 bg-white p-6"
      >
        {/* 프로필 이미지 — 선택 사항. 제출 시 가입보다 먼저 업로드된다 */}
        <div className="flex flex-col items-center gap-2 pb-1">
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            className="size-20 overflow-hidden rounded-full border border-sand-200 bg-sand-100 transition hover:border-brand-400"
            aria-label="프로필 이미지 선택"
          >
            {profilePreview ? (
              <img src={profilePreview} alt="" className="size-full object-cover" />
            ) : (
              <span className="grid size-full place-items-center text-2xl text-slate-400">＋</span>
            )}
          </button>

          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            onChange={handleFileChange}
            className="hidden"
          />

          {profileFile ? (
            <button
              type="button"
              onClick={clearFile}
              className="text-xs text-slate-500 hover:text-red-600"
            >
              이미지 제거
            </button>
          ) : (
            <span className="text-xs text-slate-400">프로필 이미지 (선택)</span>
          )}
        </div>

        <Field
          label="이메일"
          type="email"
          value={email}
          onChange={setEmail}
          placeholder="user@example.com"
          autoComplete="email"
          required
        />

        <Field
          label="비밀번호"
          type="password"
          value={password}
          onChange={setPassword}
          placeholder="8~25자, 영문·숫자·특수문자 포함"
          autoComplete="new-password"
          required
          hint="특수문자는 ! @ # % ^ & * 만 사용할 수 있습니다"
        />

        <Field
          label="비밀번호 확인"
          type="password"
          value={passwordConfirm}
          onChange={setPasswordConfirm}
          placeholder="비밀번호 재입력"
          autoComplete="new-password"
          required
        />

        <Field
          label="닉네임"
          type="text"
          value={nickname}
          onChange={setNickname}
          placeholder="2~30자"
          autoComplete="nickname"
          required
        />

        <Field
          label="휴대폰 번호 (선택)"
          type="tel"
          value={phone}
          // 숫자만 쳐도 하이픈이 자동으로 붙는다. 저장은 숫자만 한다.
          onChange={(value) => setPhone(formatPhone(value))}
          placeholder="010-1234-5678"
          autoComplete="tel"
        />

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
          {submitting ? '가입 중…' : '회원가입'}
        </button>
      </form>

      <p className="mt-5 text-center text-sm text-slate-500">
        이미 계정이 있으신가요?{' '}
        <Link to="/login" className="font-medium text-brand-600 hover:underline">
          로그인
        </Link>
      </p>
    </div>
  )
}

interface FieldProps {
  label: string
  type: string
  value: string
  onChange: (value: string) => void
  placeholder: string
  autoComplete?: string
  required?: boolean
  hint?: string
}

function Field({
  label,
  type,
  value,
  onChange,
  placeholder,
  autoComplete,
  required,
  hint,
}: FieldProps) {
  return (
    <label className="flex flex-col gap-1.5 text-sm">
      <span className="font-medium text-slate-700">{label}</span>
      <input
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        autoComplete={autoComplete}
        required={required}
        className="rounded-lg border border-sand-200 px-3 py-2.5 outline-none focus:border-brand-400"
      />
      {hint && <span className="text-xs text-slate-400">{hint}</span>}
    </label>
  )
}

/** 제출 전 검증. 문제가 있으면 메시지를, 없으면 null 을 돌려준다. */
function validate({
  password,
  passwordConfirm,
  nickname,
  phone,
}: {
  password: string
  passwordConfirm: string
  nickname: string
  phone: string
}): string | null {
  if (password.length < 8 || password.length > 25) {
    return '비밀번호는 8~25자여야 합니다.'
  }
  if (!PASSWORD_PATTERN.test(password)) {
    return '비밀번호는 영문, 숫자, 특수문자(!@#%^&*)를 모두 포함해야 합니다.'
  }
  if (password !== passwordConfirm) {
    return '비밀번호가 일치하지 않습니다.'
  }
  if (nickname.length < 2 || nickname.length > 30) {
    return '닉네임은 2~30자여야 합니다.'
  }
  if (!isValidPhone(toDigits(phone))) {
    return '휴대폰 번호 형식이 올바르지 않습니다. (예: 010-1234-5678)'
  }
  return null
}

function toMessage(e: unknown): string {
  if (!(e instanceof ApiError)) {
    return '알 수 없는 오류가 발생했습니다.'
  }
  if (e.code === ERROR_CODE.EMAIL_DUPLICATED) {
    return '이미 사용 중인 이메일입니다.'
  }
  if (e.code === ERROR_CODE.NICKNAME_DUPLICATED) {
    return '이미 사용 중인 닉네임입니다.'
  }
  if (e.code === '') {
    return '서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인해 주세요.'
  }
  return e.message
}
