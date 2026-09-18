import { useAuth } from '@/features/auth/authContext'
import { Avatar } from '@/shared/ui/Avatar'
import { Logo } from '@/shared/ui/Logo'
import { Link, NavLink } from 'react-router'

/** 화면정의서 기준 상단 내비게이션. 행사 목록 화면들은 아직 없어서 링크만 잡아둔다. */
const MENUS = [
  { label: '공공행사', to: '/festivals/public' },
  { label: '민간행사', to: '/festivals/member' },
  { label: '커뮤니티', to: '/community' },
]

export function Header() {
  const { me, logout } = useAuth()

  return (
    <header className="sticky top-0 z-10 border-b border-sand-200 bg-white/90 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-5xl items-center justify-between px-4">
        <div className="flex items-center gap-8">
          <Link to="/" aria-label="홈으로">
            <Logo />
          </Link>

          <nav className="hidden gap-6 text-sm text-slate-600 sm:flex">
            {MENUS.map((menu) => (
              <NavLink
                key={menu.to}
                to={menu.to}
                className={({ isActive }) =>
                  isActive ? 'font-semibold text-brand-600' : 'hover:text-slate-900'
                }
              >
                {menu.label}
              </NavLink>
            ))}
          </nav>
        </div>

        {me ? (
          <div className="flex items-center gap-3 text-sm">
            <Link
              to="/mypage"
              className="flex items-center gap-2 font-medium text-slate-700 hover:text-brand-600"
            >
              <Avatar src={me.profileImg} name={me.nickname} />
              {me.nickname}
            </Link>
            <button
              type="button"
              onClick={logout}
              className="rounded-md border border-sand-200 px-3 py-1.5 text-slate-600 hover:bg-sand-100"
            >
              로그아웃
            </button>
          </div>
        ) : (
          <Link
            to="/login"
            className="rounded-md bg-brand-500 px-4 py-1.5 text-sm font-medium text-white hover:bg-brand-600"
          >
            로그인
          </Link>
        )}
      </div>
    </header>
  )
}
