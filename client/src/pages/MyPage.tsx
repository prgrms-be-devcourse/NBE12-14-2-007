import { useAuth } from '@/features/auth/authContext'
import { uploadImage, validateImage } from '@/features/image/api'
import { updateMyPage } from '@/features/member/api'
import type { MemberRole, MyPageInfo } from '@/features/member/types'
import { ApiError } from '@/shared/api/types'
import { formatPhone, isValidPhone, toDigits } from '@/shared/lib/phone'
import { Avatar } from '@/shared/ui/Avatar'
import { useEffect, useMemo, useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { Navigate } from 'react-router'
import { PasswordSection } from './mypage/PasswordSection'

/** 화면정의서 8번. 신뢰도·활동 탭은 관련 API 가 없어 자리만 잡아둔다. */
const TABS = [
  { key: 'profile', label: '내 정보' },
  { key: 'trust', label: '신뢰도 현황' },
  { key: 'posts', label: '내가 쓴 글' },
  { key: 'activity', label: '내 활동' },
] as const

type TabKey = (typeof TABS)[number]['key']

const ROLE_LABEL: Record<MemberRole, string> = {
  ROLE_WARNING: '제재 중',
  ROLE_UNVERIFIED: '신규 주최자',
  ROLE_NORMAL: '일반',
  ROLE_TRUSTED: '신뢰 주최자',
  ROLE_ADMIN: '관리자',
}

export default function MyPage() {
  const { me, booting } = useAuth()
  const [tab, setTab] = useState<TabKey>('profile')

  if (booting) {
    return null
  }
  if (!me) {
    return <Navigate to="/login" replace />
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-6">
      <h1 className="flex items-center gap-2 text-xl font-bold text-slate-900">
        <span aria-hidden>🎁</span> 마이페이지
      </h1>

      <nav className="mt-5 flex gap-1 border-b border-sand-200">
        {TABS.map((item) => (
          <button
            key={item.key}
            type="button"
            onClick={() => setTab(item.key)}
            className={
              tab === item.key
                ? '-mb-px border-b-2 border-brand-500 px-4 py-2 text-sm font-semibold text-brand-600'
                : 'px-4 py-2 text-sm text-slate-500 hover:text-slate-800'
            }
          >
            {item.label}
          </button>
        ))}
      </nav>

      <div className="mt-6">
        {tab === 'profile' ? (
          <>
            <ProfileSection me={me} />
            <PasswordSection />
          </>
        ) : (
          <Placeholder />
        )}
      </div>
    </div>
  )
}

/** 신뢰도·게시글·활동은 백엔드 API 가 아직 없다. */
function Placeholder() {
  return (
    <p className="rounded-xl border border-dashed border-sand-200 px-4 py-12 text-center text-sm text-slate-400">
      아직 준비 중인 기능입니다.
    </p>
  )
}
/**
 * 내 정보. 기본은 읽기 전용이고 "수정하기"를 눌러야 입력창으로 바뀐다.
 * 실수로 값을 건드리는 걸 막기 위한 장치이며, 서버는 토큰만 확인한다.
 */
function ProfileSection({ me }: { me: MyPageInfo }) {
  const { refreshMe } = useAuth()

  const [editing, setEditing] = useState(false)
  const [nickname, setNickname] = useState(me.nickname)
  const [phone, setPhone] = useState(formatPhone(me.phone ?? ''))
  const [file, setFile] = useState<File | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)
  const [saving, setSaving] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const preview = useMemo(() => (file ? URL.createObjectURL(file) : null), [file])

  useEffect(() => {
    if (!preview) {
      return
    }
    return () => URL.revokeObjectURL(preview)
  }, [preview])

  function startEditing() {
    setEditing(true)
    setDone(false)
    setError(null)
  }

  /** 편집한 내용을 버리고 서버 값으로 되돌린다. */
  function cancelEditing() {
    setEditing(false)
    setError(null)
    setNickname(me.nickname)
    setPhone(formatPhone(me.phone ?? ''))
    setFile(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const selected = event.target.files?.[0]
    if (!selected) {
      return
    }
    const invalid = validateImage(selected)
    if (invalid) {
      setError(invalid)
      event.target.value = ''
      return
    }
    setError(null)
    setFile(selected)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()

    if (nickname.length < 2 || nickname.length > 30) {
      setError('닉네임은 2~30자여야 합니다.')
      return
    }
    if (!isValidPhone(toDigits(phone))) {
      setError('휴대폰 번호 형식이 올바르지 않습니다. (예: 010-1234-5678)')
      return
    }

    setError(null)
    setSaving(true)
    try {
      const profileImg = file ? (await uploadImage(file, 'PROFILE')).key : undefined

      await updateMyPage({
        nickname,
        // 빈 문자열을 보내면 서버가 번호를 지운다. 이게 "삭제"를 표현하는 방법이다.
        phone: toDigits(phone),
        profileImg,
      })

      // 헤더 아바타·닉네임이 바로 바뀌도록 전역 상태를 다시 읽는다.
      await refreshMe()
      setFile(null)
      setEditing(false)
      setDone(true)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : '알 수 없는 오류가 발생했습니다.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="rounded-xl border border-sand-200 bg-white p-6">
      <div className="flex flex-col items-center gap-3">
        {editing ? (
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            className="rounded-full ring-offset-2 hover:ring-2 hover:ring-brand-400"
            aria-label="프로필 이미지 변경"
          >
            <Avatar src={preview ?? me.profileImg} name={me.nickname} size="lg" />
          </button>
        ) : (
          <Avatar src={me.profileImg} name={me.nickname} size="lg" />
        )}

        <input
          ref={fileInputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          onChange={handleFileChange}
          className="hidden"
        />

        {editing && (
          <span className="text-xs text-slate-400">
            {file ? '저장을 눌러야 반영됩니다' : '이미지를 눌러 변경'}
          </span>
        )}
      </div>

      <dl className="mt-6 flex flex-col gap-4 text-sm">
        <Row label="이메일">
          {/* 이메일은 로그인 식별자라 수정 API 가 없다 */}
          <span className="text-slate-500">{me.email}</span>
        </Row>

        <Row label="등급">
          <span className="rounded bg-sand-100 px-2 py-0.5 text-xs text-slate-600">
            {ROLE_LABEL[me.role]}
          </span>
        </Row>

        <Row label="가입일">
          <span className="text-slate-500">{me.createdAt.slice(0, 10)}</span>
        </Row>

        <Row label="닉네임">
          {editing ? (
            <input
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              className="w-full rounded-lg border border-sand-200 px-3 py-2 outline-none focus:border-brand-400"
            />
          ) : (
            <span className="text-slate-800">{me.nickname}</span>
          )}
        </Row>

        <Row label="휴대폰">
          {editing ? (
            <input
              type="tel"
              value={phone}
              onChange={(e) => setPhone(formatPhone(e.target.value))}
              placeholder="010-1234-5678"
              className="w-full rounded-lg border border-sand-200 px-3 py-2 outline-none focus:border-brand-400"
            />
          ) : (
            <span className="text-slate-800">{formatPhone(me.phone ?? '') || '-'}</span>
          )}
        </Row>
      </dl>

      {error && (
        <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      )}
      {done && !editing && (
        <p className="mt-4 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
          저장되었습니다.
        </p>
      )}

      {editing ? (
        <div className="mt-5 flex gap-2">
          <button
            type="button"
            onClick={cancelEditing}
            disabled={saving}
            className="flex-1 rounded-lg border border-sand-200 py-2.5 text-sm text-slate-600 hover:bg-sand-100 disabled:opacity-60"
          >
            취소
          </button>
          <button
            type="submit"
            disabled={saving}
            className="flex-1 rounded-lg bg-brand-500 py-2.5 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-60"
          >
            {saving ? '저장 중…' : '저장'}
          </button>
        </div>
      ) : (
        <button
          type="button"
          onClick={startEditing}
          className="mt-5 w-full rounded-lg border border-sand-200 py-2.5 text-sm font-medium text-slate-700 hover:bg-sand-100"
        >
          수정하기
        </button>
      )}
    </form>
  )
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="grid grid-cols-[5rem_1fr] items-center gap-3">
      <dt className="text-slate-500">{label}</dt>
      <dd>{children}</dd>
    </div>
  )
}
