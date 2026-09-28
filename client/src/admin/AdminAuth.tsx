import {
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
} from "react";
import {
  Link,
  Navigate,
  Outlet,
  useLocation,
  useNavigate,
  useSearchParams,
} from "react-router-dom";
import { ArrowDownLeft, ArrowRight, ShieldCheck } from "lucide-react";
import {
  Field,
  FormError,
  Loading,
  Logo,
  SubmitButton,
} from "../components/ui";
import {
  accessTokenClaims,
  clearSession,
  isTokenExpired,
  refreshAccessToken,
} from "../lib/api";
import { AppProvider, useApp } from "../lib/context";
import { errorText } from "../lib/format";
import "./admin.css";

/**
 * /admin 전체의 뿌리.
 *
 * 서비스 화면에서 쓰던 액세스 토큰을 이어받지 않는다. 관리자 화면에 들어오는 순간 토큰을 버리고,
 * 리프레시 쿠키로 자동 복구도 하지 않는다. 그래서 /admin 에 들어오면 항상 /admin/login 부터 거친다.
 * 관리자 화면 안에서 이동할 때는 이 컴포넌트가 계속 살아 있어서 로그인이 유지된다.
 */
export function AdminRoot() {
  // 자식(가드)이 렌더되기 전에 비워야 이전 토큰으로 가드를 통과하지 않는다.
  // useEffect는 자식 렌더 뒤에 돌아서 늦다.
  const cleared = useRef(false);
  if (!cleared.current) {
    cleared.current = true;
    clearSession();
  }
  return (
    <AppProvider restoreSession={false}>
      <Outlet />
    </AppProvider>
  );
}

/**
 * 관리자 화면 가드. 경로가 바뀔 때마다 액세스 토큰의 role을 확인한다.
 *
 * - 토큰이 없거나 role이 ROLE_ADMIN이 아니면 → /admin/login
 * - 토큰이 만료됐으면 → 리프레시 쿠키로 재발급 후 다시 확인, 실패하면 /admin/login
 *
 * 토큰 payload는 서명 검증 없이 읽는다. 화면 분기용일 뿐이고
 * 실제 권한은 서버의 /api/v1/admin/** 규칙(hasRole ADMIN)이 막는다.
 */
export function AdminGuard({ children }: { children: ReactNode }) {
  const { member } = useApp();
  const location = useLocation();
  const [, setRevision] = useState(0);
  const [refreshFailed, setRefreshFailed] = useState(false);
  const claims = accessTokenClaims();
  const expired = claims !== null && isTokenExpired(claims);

  useEffect(() => {
    if (!expired) return;
    let active = true;
    refreshAccessToken()
      .then(() => active && setRevision((x) => x + 1))
      .catch(() => active && setRefreshFailed(true));
    return () => {
      active = false;
    };
  }, [expired, location.pathname]);

  const next = encodeURIComponent(location.pathname + location.search);
  if (!member || !claims || claims.role !== "ROLE_ADMIN" || refreshFailed)
    return <Navigate to={`/admin/login?next=${next}`} replace />;
  if (expired) return <Loading />;
  return <>{children}</>;
}

export function AdminLogin() {
  const { api, setMember } = useApp();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const next = params.get("next");
  const target =
    next?.startsWith("/admin") && !next.startsWith("/admin/login")
      ? next
      : "/admin";

  // 로그인 화면에 오면 남아 있던 관리자 토큰도 버린다.
  useEffect(() => {
    clearSession();
    setMember(null);
  }, [setMember]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      setMember(await api.adminLogin(email.trim(), password));
      navigate(target, { replace: true });
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="adm-login">
      <form className="adm-login-card" onSubmit={submit}>
        <div className="adm-login-brand">
          <Logo />
          <span>ADMIN CONSOLE</span>
        </div>
        <h1>관리자 로그인</h1>
        <p>관리자 계정으로 다시 로그인해 주세요.</p>
        <Field label="이메일" required>
          <input
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="admin@example.com"
          />
        </Field>
        <Field label="비밀번호" required>
          <input
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </Field>
        <FormError message={error} />
        <SubmitButton busy={busy}>
          로그인
          <ArrowRight size={17} />
        </SubmitButton>
        <p className="adm-login-foot">
          <ShieldCheck size={15} />
          관리자 권한은 로그인할 때와 화면을 옮길 때마다 확인합니다.
        </p>
        <Link className="adm-login-back" to="/">
          <ArrowDownLeft size={15} />
          서비스로 돌아가기
        </Link>
      </form>
    </div>
  );
}
