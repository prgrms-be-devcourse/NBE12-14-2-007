import { useEffect, useState } from "react";
import {
  Link,
  NavLink,
  Outlet,
  useLocation,
  useNavigate,
} from "react-router-dom";
import {
  ArrowRight,
  ChevronDown,
  Compass,
  Menu,
  Plus,
  UserRound,
  X,
} from "lucide-react";
import { useApp } from "../lib/context";
import { Logo } from "./ui";

export function Layout() {
  const { mode, setMode, member } = useApp();
  const [open, setOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  useEffect(() => {
    setOpen(false);
    window.scrollTo({ top: 0, behavior: "instant" });
  }, [location.pathname]);
  return (
    <>
      <a className="skip-link" href="#main">
        본문으로 바로가기
      </a>
      <div className="preview-bar">
        <div className="container">
          <span>
            <span className={`mode-dot ${mode}`} />
            {mode === "preview"
              ? "디자인 미리보기 · 행사와 활동은 예시 데이터입니다"
              : "실제 서비스 · 로그인 후 내 활동을 확인하세요"}
          </span>
          <button
            onClick={() => {
              setMode(mode === "preview" ? "api" : "preview");
              navigate("/");
            }}
          >
            {mode === "preview" ? "실제 API 연결" : "미리보기로 돌아가기"}
            <ArrowRight size={13} />
          </button>
        </div>
      </div>
      <header className="site-header">
        <div className="container header-inner">
          <Link to="/" aria-label="방구석탈출 홈">
            <Logo />
          </Link>
          <nav
            className={`main-nav ${open ? "open" : ""}`}
            aria-label="메인 메뉴"
          >
            <NavLink to="/" end>
              홈
            </NavLink>
            <NavLink to="/explore">지역 문화행사</NavLink>
            <NavLink to="/submissions">행사 제보</NavLink>
            <NavLink to="/reviews">행사 후기</NavLink>
          </nav>
          <div className="header-actions">
            <Link className="header-submit" to="/submissions/new">
              <Plus size={15} />
              행사 제보하기
            </Link>
            <span className="header-divider" />
            <Link
              to={member ? "/mypage" : "/login"}
              className="account-link"
              aria-label={member ? "마이페이지" : "로그인"}
            >
              <UserRound size={18} />
              <span>{member ? "마이페이지" : "로그인"}</span>
              <ChevronDown size={12} />
            </Link>
            <button
              className="icon-btn mobile-menu"
              aria-label={open ? "메뉴 닫기" : "메뉴 열기"}
              aria-expanded={open}
              onClick={() => setOpen(!open)}
            >
              {open ? <X /> : <Menu />}
            </button>
          </div>
        </div>
      </header>
      <main id="main" className="main-content" key={mode}>
        <Outlet />
      </main>
      <footer className="site-footer">
        <div className="container">
          <div className="footer-top">
            <div>
              <Link to="/">
                <Logo />
              </Link>
              <p>
                일상 밖, 새로운 즐거움.
                <br />
                발견하고, 함께하고, 기록해요.
              </p>
            </div>
            <div className="footer-links">
              <Link to="/explore">지역 문화행사</Link>
              <Link to="/submissions">행사 제보</Link>
              <Link to="/reviews">행사 후기</Link>
              <Link to="/mypage?tab=inquiries">문의하기</Link>
              <Link to="/admin">시스템 관리자 미리보기</Link>
            </div>
            <div className="footer-message">
              <Compass size={24} />
              <span>
                우리의 다음 즐거움은
                <br />
                <strong>생각보다 가까이에.</strong>
              </span>
            </div>
          </div>
          <div className="footer-bottom">
            <span>© 2026 roomescape. 함께 만드는 즐거운 일상.</span>
            <span>
              {mode === "preview" ? "DESIGN PREVIEW" : "일상에 즐거움을 더하다"}
            </span>
          </div>
        </div>
      </footer>
    </>
  );
}
