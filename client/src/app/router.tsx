import { useAuth } from '@/features/auth/authContext'
import HomePage from '@/pages/HomePage'
import LoginPage from '@/pages/LoginPage'
import MyPage from '@/pages/MyPage'
import SignupPage from '@/pages/SignupPage'
import { Header } from '@/shared/ui/Header'
import { Outlet, Route, Routes } from 'react-router'

/** 헤더를 공유하는 기본 레이아웃. */
function Layout() {
  const { booting } = useAuth()

  // 세션 복구 전에 화면을 그리면 로그인 버튼이 잠깐 보였다 사라진다.
  if (booting) {
    return (
      <div className="grid min-h-screen place-items-center text-sm text-slate-400">
        불러오는 중…
      </div>
    )
  }

  return (
    <>
      <Header />
      <Outlet />
    </>
  )
}

export function Router() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/mypage" element={<MyPage />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}

function NotFound() {
  return (
    <div className="mx-auto max-w-5xl px-4 py-20 text-center text-slate-500">
      아직 준비되지 않은 화면입니다.
    </div>
  )
}
