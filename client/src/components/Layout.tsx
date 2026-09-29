import { useEffect, useState } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { ChevronDown, Compass, Menu, Plus, UserRound, X } from "lucide-react";
import { useApp } from "../lib/context";
import { Logo } from "./ui";

export function Layout() {
  const { member } = useApp();
  const [open, setOpen] = useState(false);
  const location = useLocation();
  useEffect(() => {
    setOpen(false);
    window.scrollTo({ top: 0, behavior: "instant" });
  }, [location.pathname]);
  return (
    <>
      <a className="skip-link" href="#main">
        본문으로 바로가기
      </a>
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
            <NavLink to="/submissions/new">행사 제보</NavLink>
            <NavLink to="/reviews">행사 후기</NavLink>
            <NavLink to="/community">커뮤니티</NavLink>
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
      <main id="main" className="main-content">
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
              <Link to="/submissions/new">행사 제보</Link>
              <Link to="/reviews">행사 후기</Link>
              <Link to="/community">커뮤니티</Link>
              <Link to="/mypage?tab=inquiries">문의하기</Link>
              <Link to="/admin">시스템 관리자</Link>
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
            <span>일상에 즐거움을 더하다</span>
          </div>
        </div>
      </footer>
    </>
  );
}
