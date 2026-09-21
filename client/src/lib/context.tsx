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
 * 로그인·마이페이지는 항상 실제 백엔드를 사용한다.
 * 아직 API가 없는 행사 탐색·후기 화면만 예시 데이터로 채우기 위해 남겨둔 값.
 */
const CONTENT_MODE: Mode = "preview";

interface AppContextValue {
  mode: Mode;
  api: Api;
  member: Member | null;
  setMember: (member: Member | null) => void;
  authLoading: boolean;
  toast: (message: string) => void;
}
const Context = createContext<AppContextValue | null>(null);
export function AppProvider({ children }: { children: ReactNode }) {
  const [member, setMember] = useState<Member | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [message, setMessage] = useState("");
  const api = useMemo(() => createApi(CONTENT_MODE), []);
  useEffect(() => {
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
  }, [api]);
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
