import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { createApi, setCurrentMember, type Api } from "./api";
import type { Member, Mode } from "./types";

/**
 * API 모드는 실제 백엔드를 사용하고, preview 모드는 아직 API 연결이 끝나지 않은
 * 화면을 예시 데이터로 확인하기 위해 사용한다.
 */
const CONTENT_MODE: Mode =
  import.meta.env.VITE_CONTENT_MODE === "api" ||
  sessionStorage.getItem("eventus.mode") === "api"
    ? "api"
    : "preview";

interface AppContextValue {
  mode: Mode;
  api: Api;
  member: Member | null;
  setMember: (member: Member | null) => void;
  authLoading: boolean;
  toast: (message: string) => void;
}
const Context = createContext<AppContextValue | null>(null);
/**
 * restoreSession=false 면 리프레시 쿠키로 로그인을 복구하지 않는다.
 * 관리자 화면은 서비스 쪽 로그인을 이어받지 않고 항상 다시 로그인하게 하려고 끈다.
 */
export function AppProvider({
  children,
  restoreSession = true,
}: {
  children: ReactNode;
  restoreSession?: boolean;
}) {
  const [member, setMember] = useState<Member | null>(null);
  const [authLoading, setAuthLoading] = useState(restoreSession);
  const [message, setMessage] = useState("");
  const api = useMemo(() => createApi(CONTENT_MODE), []);
  useEffect(() => {
    if (!restoreSession) return;
    let active = true;
    setAuthLoading(true);
    api
      .restore()
      .then((m) => {
        if (active) setMember(m);
      })
      .catch(() => {
        if (active) setMember(null);
      })
      .finally(() => {
        if (active) setAuthLoading(false);
      });
    return () => {
      active = false;
    };
  }, [api, restoreSession]);
  useEffect(() => {
    setCurrentMember(member);
  }, [member]);
  useEffect(() => {
    const fn = () => setMember(null);
    window.addEventListener("eventus:session-expired", fn);
    return () => window.removeEventListener("eventus:session-expired", fn);
  }, []);
  useEffect(() => {
    if (!message) return;
    const t = window.setTimeout(() => setMessage(""), 4500);
    return () => clearTimeout(t);
  }, [message]);
  return (
    <Context.Provider
      value={{
        mode: CONTENT_MODE,
        api,
        member,
        setMember,
        authLoading,
        toast: setMessage,
      }}
    >
      {children}
      {message && (
        <div className="toast" role="status">
          {message}
          <button onClick={() => setMessage("")} aria-label="알림 닫기">
            ×
          </button>
        </div>
      )}
    </Context.Provider>
  );
}
export function useApp() {
  const context = useContext(Context);
  if (!context) throw new Error("Missing AppProvider");
  return context;
}
export function useLoad<T>(loader: () => Promise<T>, dependencies: unknown[]) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    let current = true;
    setLoading(true);
    setError("");
    setData(null);
    loader()
      .then((value) => {
        if (current) setData(value);
      })
      .catch((e) => {
        if (current)
          setError(e instanceof Error ? e.message : "불러오지 못했어요.");
      })
      .finally(() => {
        if (current) setLoading(false);
      });
    return () => {
      current = false;
    };
  }, [...dependencies, revision]);
  return {
    data,
    loading,
    error,
    reload: () => setRevision((x) => x + 1),
    setData,
  };
}
