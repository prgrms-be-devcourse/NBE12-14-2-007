import { useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ArrowLeft, ArrowRight, Eye, EyeOff, ShieldCheck } from "lucide-react";
import { useApp } from "../lib/context";
import { errorText } from "../lib/format";
import { Field, FormError, Logo, SubmitButton } from "../components/ui";

export function AuthPage({ signup = false }: { signup?: boolean }) {
  const { api, setMember, toast, authLoading } = useApp();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [nickname, setNickname] = useState("");
  const [phone, setPhone] = useState("");
  const [consents, setConsents] = useState({
    terms: false,
    privacy: false,
    age: false,
    marketing: false,
  });
  const [visible, setVisible] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const next = params.get("next");
  const target =
    next?.startsWith("/") && !next.startsWith("//") && !next.includes("\\")
      ? next
      : "/mypage";
  async function submit(e: FormEvent) {
    e.preventDefault();
    setError("");
    if (signup && password !== confirm) {
      setError("비밀번호가 서로 달라요. 다시 확인해 주세요.");
      return;
    }
    setBusy(true);
    try {
      if (signup) {
        await api.signup({
          email: email.trim(),
          password,
          nickname: nickname.trim(),
          ...(phone ? { phone } : {}),
        });
        toast("가입했어요. 이제 로그인해 주세요.");
        navigate(`/login?next=${encodeURIComponent(target)}`);
      } else {
        setMember(await api.login(email.trim(), password));
        navigate(target);
        toast("다시 만나 반가워요.");
      }
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="container auth-page">
      <div className="auth-visual">
        <img src="/images/garden.jpg" alt="가을 정원" />
        <div />
        <section>
          <span>EVERYDAY, A LITTLE MORE SPECIAL</span>
          <h2>
            우리의 다음 즐거움은
            <br />
            생각보다 가까이에.
          </h2>
          <p>
            새로운 발견과 소중한 순간을
            <br />
            방구석탈출에서 함께 나눠요.
          </p>
        </section>
      </div>
      <div className="auth-panel">
        <Link className="back-link" to="/">
          <ArrowLeft size={15} />
          홈으로
        </Link>
        <Logo />
        <h1>{signup ? "새로운 즐거움에 함께해요" : "다시 만나 반가워요"}</h1>
        <p className="muted">
          {signup
            ? "일상을 특별하게 만드는 첫걸음, 회원가입."
            : "로그인하고 나만의 즐거운 순간을 기록해요."}
        </p>
        <form onSubmit={submit}>
          <Field label="이메일" required>
            <input
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="example@email.com"
            />
          </Field>
          <Field
            label="비밀번호"
            required
            hint={
              signup ? "8~25자, 영문·숫자·특수문자(!@#%^&*) 포함" : undefined
            }
          >
            <span className="password-field">
              <input
                type={visible ? "text" : "password"}
                required
                autoComplete={signup ? "new-password" : "current-password"}
                minLength={signup ? 8 : undefined}
                maxLength={signup ? 25 : undefined}
                pattern={
                  signup
                    ? "(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!@#%^&*]).{8,25}"
                    : undefined
                }
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="비밀번호를 입력해 주세요"
              />
              <button
                type="button"
                aria-label={visible ? "비밀번호 숨기기" : "비밀번호 표시"}
                onClick={() => setVisible(!visible)}
              >
                {visible ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </span>
          </Field>
          {signup && (
            <>
              <Field label="비밀번호 확인" required>
                <input
                  type="password"
                  autoComplete="new-password"
                  required
                  value={confirm}
                  onChange={(e) => setConfirm(e.target.value)}
                  placeholder="비밀번호를 다시 입력해 주세요"
                />
              </Field>
              <Field label="닉네임" required hint="2~30자로 입력해 주세요.">
                <input
                  required
                  minLength={2}
                  maxLength={30}
                  autoComplete="nickname"
                  value={nickname}
                  onChange={(e) => setNickname(e.target.value)}
                  placeholder="이웃에게 보여줄 이름"
                />
              </Field>
              <Field
                label="휴대폰 번호"
                hint="선택 항목 · 하이픈 없이 숫자만 입력해 주세요."
              >
                <input
                  type="tel"
                  autoComplete="tel"
                  pattern="01[016789][0-9]{7,8}"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value.replace(/\D/g, ""))}
                  placeholder="01012345678"
                />
              </Field>
              <Consents value={consents} onChange={setConsents} />
            </>
          )}
          <FormError message={error} />
          <SubmitButton
            busy={busy || authLoading}
            // 필수 동의를 받지 않으면 가입 자체를 막는다.
            disabled={
              signup && !(consents.terms && consents.privacy && consents.age)
            }
          >
            {signup ? "회원가입" : "로그인"}
            <ArrowRight size={17} />
          </SubmitButton>
        </form>
        <p className="auth-switch">
          {signup ? "이미 함께하고 계신가요?" : "방구석탈출이 처음이신가요?"}
          <Link
            to={`${signup ? "/login" : "/signup"}?next=${encodeURIComponent(target)}`}
          >
            {signup ? "로그인" : "회원가입"}
          </Link>
        </p>
        <p className="auth-foot">
          <ShieldCheck size={15} />
          당신의 새로운 일상을 응원해요.
        </p>
      </div>
    </div>
  );
}

interface ConsentState {
  terms: boolean;
  privacy: boolean;
  age: boolean;
  marketing: boolean;
}
/**
 * 가입 시 법적 고지·동의.
 *
 * 개인정보 수집 항목·목적·보유기간은 개인정보보호법이 고지를 요구하는 항목이라
 * 링크 뒤에 숨기지 않고 화면에 바로 적는다.
 *
 * TODO 동의 시각과 약관 버전을 서버에 기록할 것.
 *      동의를 받았다는 입증 책임은 서비스 쪽에 있는데 지금은 화면에서만 확인한다.
 *      member_consent 테이블과 가입 API 파라미터가 필요하다.
 */
function Consents({
  value,
  onChange,
}: {
  value: ConsentState;
  onChange: (next: ConsentState) => void;
}) {
  const set = (key: keyof ConsentState) => (checked: boolean) =>
    onChange({ ...value, [key]: checked });
  const allRequired = value.terms && value.privacy && value.age;
  const all = allRequired && value.marketing;
  return (
    <div className="consent-box">
      <label className="consent-all">
        <input
          type="checkbox"
          checked={all}
          onChange={(e) => {
            const next = e.target.checked;
            onChange({
              terms: next,
              privacy: next,
              age: next,
              marketing: next,
            });
          }}
        />
        <strong>전체 동의</strong>
        <span>선택 항목을 포함합니다.</span>
      </label>
      <label className="consent-row">
        <input
          type="checkbox"
          checked={value.age}
          onChange={(e) => set("age")(e.target.checked)}
        />
        <span>
          <b>[필수]</b> 만 14세 이상입니다.
        </span>
      </label>
      <label className="consent-row">
        <input
          type="checkbox"
          checked={value.terms}
          onChange={(e) => set("terms")(e.target.checked)}
        />
        <span>
          <b>[필수]</b> 이용약관에 동의합니다.
        </span>
      </label>
      <label className="consent-row">
        <input
          type="checkbox"
          checked={value.privacy}
          onChange={(e) => set("privacy")(e.target.checked)}
        />
        <span>
          <b>[필수]</b> 개인정보 수집·이용에 동의합니다.
        </span>
      </label>
      <dl className="consent-detail">
        <div>
          <dt>수집 항목</dt>
          <dd>이메일, 비밀번호, 닉네임 (선택: 휴대폰 번호, 프로필 사진)</dd>
        </div>
        <div>
          <dt>이용 목적</dt>
          <dd>회원 식별과 로그인, 행사 제보·후기 작성, 문의 응대</dd>
        </div>
        <div>
          <dt>보유 기간</dt>
          <dd>
            탈퇴 후 6개월까지 보관한 뒤 파기합니다. 이 기간에는 같은 이메일로
            다시 가입할 수 없습니다.
          </dd>
        </div>
      </dl>
      <p className="consent-note">
        필수 항목에 동의하지 않으면 가입할 수 없지만, 동의를 거부할 권리가
        있습니다.
      </p>
      <label className="consent-row">
        <input
          type="checkbox"
          checked={value.marketing}
          onChange={(e) => set("marketing")(e.target.checked)}
        />
        <span>
          <b className="optional">[선택]</b> 새로운 행사 소식 메일을 받겠습니다.
        </span>
      </label>
    </div>
  );
}
