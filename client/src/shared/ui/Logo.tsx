/** EventUs 로고. 화면정의서의 주황 아이콘 + 워드마크 조합. */
export function Logo({ size = 'md' }: { size?: 'md' | 'lg' }) {
  const box = size === 'lg' ? 'size-10 text-xl' : 'size-7 text-sm'
  const text = size === 'lg' ? 'text-2xl' : 'text-lg'

  return (
    <span className="flex items-center gap-2">
      <span
        className={`${box} grid place-items-center rounded-lg bg-brand-500 font-bold text-white`}
        aria-hidden
      >
        E
      </span>
      <span className={`${text} font-bold text-slate-900`}>EventUs</span>
    </span>
  )
}
