import { useState } from 'react'

/**
 * 프로필 이미지. 없으면 닉네임 첫 글자로 대체한다.
 *
 * 기본 이미지를 R2 에 올려두지 않는 이유는, 모든 회원이 같은 파일을 가리키는
 * 의미 없는 참조가 생기고 "설정 안 함"과 구분이 어려워지기 때문이다.
 */
const PALETTE = [
  'bg-brand-400',
  'bg-rose-400',
  'bg-emerald-400',
  'bg-sky-400',
  'bg-violet-400',
  'bg-amber-400',
]

/** 같은 닉네임이면 항상 같은 색이 나오도록 이름에서 색을 뽑는다. */
function colorOf(name: string): string {
  let hash = 0
  for (const char of name) {
    hash = (hash + char.codePointAt(0)!) % PALETTE.length
  }
  return PALETTE[hash]
}

const SIZES = {
  sm: 'size-7 text-xs',
  md: 'size-10 text-base',
  lg: 'size-20 text-2xl',
}

interface AvatarProps {
  /** 공개 URL. 없으면 닉네임 첫 글자로 그린다 */
  src: string | null
  name: string
  size?: keyof typeof SIZES
}

export function Avatar({ src, name, size = 'sm' }: AvatarProps) {
  // 파일이 지워졌거나 URL 이 깨진 경우 깨진 이미지 아이콘 대신 첫 글자로 되돌린다.
  const [broken, setBroken] = useState(false)
  const box = `${SIZES[size]} shrink-0 overflow-hidden rounded-full`

  if (src && !broken) {
    return (
      <img src={src} alt="" onError={() => setBroken(true)} className={`${box} object-cover`} />
    )
  }

  return (
    <span
      className={`${box} ${colorOf(name)} grid place-items-center font-semibold text-white`}
      aria-hidden
    >
      {[...name][0] ?? '?'}
    </span>
  )
}
