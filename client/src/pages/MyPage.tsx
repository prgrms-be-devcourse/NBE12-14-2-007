import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
  ArrowRight,
  Check,
  ChevronRight,
  ClipboardList,
  LogOut,
  Mail,
  MessageCircle,
  Pencil,
  Plus,
  ShieldCheck,
  UserRound,
} from "lucide-react";
import { useApp, useLoad } from "../lib/context";
import { dateText, errorText, roleNames } from "../lib/format";
import type { Inquiry, InquiryInput } from "../lib/types";
import {
  Badge,
  Empty,
  ErrorState,
  Field,
  FormError,
  Loading,
  LoginRequired,
  Modal,
  PageTitle,
  Photo,
  SubmitButton,
  Upload,
} from "../components/ui";
import { SubmissionList } from "./Submissions";

export function MyPage() {
  const { member, authLoading, api, setMember, toast } = useApp();
  const [params, setParams] = useSearchParams();
  const [error, setError] = useState("");
  const [loggingOut, setLoggingOut] = useState(false);
  const tab = params.get("tab") || "profile";
  return (
    <div className="container page-space">
      <PageTitle
        eyebrow="MY LITTLE JOURNEY"
        title="마이페이지"
        description="내가 나눈 소식과 이야기를 한곳에서 확인해요."
      />
      {authLoading ? (
        <Loading />
      ) : !member ? (
        <LoginRequired />
      ) : (
        <div className="my-layout">
          <aside className="my-sidebar">
            <div className="my-profile">
              {member.profileImg ? (
                <Photo
                  className="profile-image"
                  src={member.profileImg}
                  alt="내 프로필"
                />
              ) : (
                <span className="avatar profile-avatar">
                  {member.nickname[0]}
                </span>
              )}
              <h2>{member.nickname}</h2>
              <p>{member.email}</p>
              <Badge tone="green">{roleNames[member.role]}</Badge>
            </div>
            <nav aria-label="마이페이지 메뉴">
              {[
                { key: "profile", label: "내 정보", Icon: UserRound },
                {
                  key: "submissions",
                  label: "내 행사 제보",
                  Icon: ClipboardList,
                },
                {
                  key: "inquiries",
                  label: "내 문의·신고",
                  Icon: MessageCircle,
                },
              ].map(({ key, label, Icon }) => (
                <button
                  key={key}
                  className={tab === key ? "active" : ""}
                  onClick={() => setParams({ tab: key })}
                >
                  <Icon size={18} />
                  {label}
                  <ChevronRight size={15} />
                </button>
              ))}
            </nav>
            <button
              className="logout-button"
              disabled={loggingOut}
              onClick={async () => {
                setLoggingOut(true);
                try {
                  await api.logout();
                  setMember(null);
                  toast("로그아웃했어요.");
                } catch (e) {
                  setError(errorText(e));
                  setMember(null);
                } finally {
                  setLoggingOut(false);
                }
              }}
            >
              <LogOut size={16} />
              로그아웃
            </button>
            <FormError message={error} />
          </aside>
          <div className="my-content">
            {tab === "submissions" ? (
              <SubmissionList compact />
            ) : tab === "inquiries" ? (
              <Inquiries />
            ) : (
              <Profile />
            )}
          </div>
        </div>
      )}
    </div>
  );
}
function Profile() {
  const { member, api, setMember, toast, mode } = useApp();
  const [nickname, setNickname] = useState(member!.nickname);
  const [phone, setPhone] = useState(member!.phone || "");
  const [image, setImage] = useState<string | undefined>();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [passwordOpen, setPasswordOpen] = useState(false);
  const restricted = member?.role === "ROLE_WARNING";
  async function submit(e: FormEvent) {
    e.preventDefault();
    if (nickname.trim().length < 2) {
      setError("닉네임은 2자 이상 입력해 주세요.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      const updated = await api.updateMe({
        nickname: nickname.trim(),
        phone,
        ...(image !== undefined ? { profileImg: image } : {}),
      });
      setMember(updated);
      toast(
        mode === "preview"
          ? "미리보기 프로필을 저장했어요."
          : "내 정보를 저장했어요.",
      );
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <div className="section-heading">
        <div>
          <h2>내 정보</h2>
          <p className="muted">이웃에게 보여줄 나를 소개해 주세요.</p>
        </div>
      </div>
      <div className="trust-panel">
        <ShieldCheck size={38} />
        <div>
          <span>나의 활동 등급</span>
          <h3>{roleNames[member!.role]}</h3>
          <p>함께 나누는 정확한 정보가 믿을 수 있는 일상을 만들어요.</p>
        </div>
      </div>
      <form className="profile-form" onSubmit={submit}>
        <fieldset disabled={restricted || busy}>
          <div className="form-grid">
            <Field label="이메일" wide>
              <input type="email" disabled value={member!.email} />
            </Field>
            <Field label="닉네임" required>
              <input
                required
                minLength={2}
                maxLength={30}
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
              />
            </Field>
            <Field label="휴대폰 번호" hint="하이픈 없이 숫자만 입력해 주세요.">
              <input
                type="tel"
                pattern="01[016789][0-9]{7,8}"
                value={phone}
                onChange={(e) => setPhone(e.target.value.replace(/\D/g, ""))}
                placeholder="01012345678"
              />
            </Field>
            <div className="wide">
              <span className="upload-label">프로필 사진</span>
              <Upload
                type="PROFILE"
                value={member!.profileImg}
                onChange={setImage}
                useKey
              />
            </div>
          </div>
        </fieldset>
        {restricted && (
          <p className="form-error">
            활동 제한 등급은 프로필을 수정할 수 없어요.
          </p>
        )}
        <FormError message={error} />
        <div className="form-actions">
          <SubmitButton busy={busy} disabled={restricted}>
            변경 사항 저장
            <Check size={16} />
          </SubmitButton>
        </div>
      </form>
      <div className="account-security">
        <div>
          <h3>계정 보안</h3>
          <p>이메일 인증 후 비밀번호를 변경할 수 있어요.</p>
        </div>
        <button
          className="btn secondary small"
          onClick={() => setPasswordOpen(true)}
        >
          비밀번호 변경
          <ArrowRight size={15} />
        </button>
      </div>
      <div className="joined-date">
        함께한 날 · {dateText(member!.createdAt)}
      </div>
      {passwordOpen && <PasswordModal onClose={() => setPasswordOpen(false)} />}
    </>
  );
}
function PasswordModal({ onClose }: { onClose: () => void }) {
  const { api, mode, setMember, toast } = useApp();
  const navigate = useNavigate();
  const [step, setStep] = useState(0);
  const [code, setCode] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [cooldown, setCooldown] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => {
    if (!cooldown) return;
    const t = setTimeout(() => setCooldown((x) => x - 1), 1000);
    return () => clearTimeout(t);
  }, [cooldown]);
  async function send() {
    setBusy(true);
    setError("");
    try {
      await api.sendPasswordCode();
      setStep(1);
      setCooldown(60);
      toast(
        mode === "preview"
          ? "미리보기에서는 메일을 보내지 않아요. 6자리 숫자를 입력해 보세요."
          : "가입한 이메일로 인증 코드를 보냈어요.",
      );
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  async function submit(e: FormEvent) {
    e.preventDefault();
    setError("");
    if (step === 2 && password !== confirm) {
      setError("비밀번호가 서로 달라요.");
      return;
    }
    setBusy(true);
    try {
      if (step === 1) {
        await api.verifyPasswordCode(code);
        setStep(2);
      } else {
        await api.changePassword(password);
        setMember(null);
        onClose();
        navigate("/login");
        toast("비밀번호를 변경했어요. 다시 로그인해 주세요.");
      }
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal title="비밀번호 변경" onClose={onClose}>
      <p className="muted">가입한 이메일로 본인 확인을 진행해요.</p>
      <div className="step-indicator">
        {["코드 받기", "이메일 인증", "비밀번호 변경"].map((label, i) => (
          <span className={step >= i ? "active" : ""} key={label}>
            {i + 1}. {label}
          </span>
        ))}
      </div>
      {step === 0 ? (
        <button
          className="btn primary full-width"
          disabled={busy}
          onClick={send}
        >
          <Mail size={17} />
          인증 코드 받기
        </button>
      ) : (
        <form onSubmit={submit}>
          {step === 1 ? (
            <>
              <Field label="6자리 인증 코드" required>
                <input
                  required
                  inputMode="numeric"
                  pattern="[0-9]{6}"
                  maxLength={6}
                  autoComplete="one-time-code"
                  value={code}
                  onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))}
                />
              </Field>
              <button
                type="button"
                className="text-button"
                disabled={cooldown > 0 || busy}
                onClick={send}
              >
                {cooldown
                  ? `${cooldown}초 후 다시 받기`
                  : "인증 코드 다시 받기"}
              </button>
            </>
          ) : (
            <>
              <Field
                label="새 비밀번호"
                required
                hint="8~25자, 영문·숫자·특수문자(!@#%^&*) 포함"
              >
                <input
                  type="password"
                  required
                  minLength={8}
                  maxLength={25}
                  pattern="(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#%^&*]).{8,25}"
                  autoComplete="new-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </Field>
              <Field label="새 비밀번호 확인" required>
                <input
                  type="password"
                  required
                  autoComplete="new-password"
                  value={confirm}
                  onChange={(e) => setConfirm(e.target.value)}
                />
              </Field>
            </>
          )}
          <div className="form-actions">
            <SubmitButton busy={busy}>
              {step === 1 ? "코드 확인" : "비밀번호 변경"}
            </SubmitButton>
          </div>
        </form>
      )}
      <FormError message={error} />
    </Modal>
  );
}
function Inquiries() {
  const { api } = useApp();
  const [params, setParams] = useSearchParams();
  const { data, loading, error, reload } = useLoad(
    () => api.inquiries(),
    [api],
  );
  const [open, setOpen] = useState(!!params.get("report"));
  const [selected, setSelected] = useState<Inquiry | null>(null);
  const [edit, setEdit] = useState(false);
  const report = params.get("report") || "";
  function close() {
    setOpen(false);
    setSelected(null);
    setEdit(false);
    if (report) setParams({ tab: "inquiries" }, { replace: true });
  }
  return (
    <>
      <div className="section-heading">
        <div>
          <h2>내 문의·신고</h2>
          <p className="muted">
            궁금한 점이나 확인이 필요한 정보를 알려주세요.
          </p>
        </div>
        <button
          className="btn primary small"
          onClick={() => {
            setSelected(null);
            setEdit(true);
            setOpen(true);
          }}
        >
          <Plus size={16} />
          문의하기
        </button>
      </div>
      {loading ? (
        <Loading />
      ) : error ? (
        <ErrorState message={error} retry={reload} />
      ) : data?.length ? (
        <div className="inquiry-list">
          {data.map((q) => (
            <button
              key={q.id}
              className="inquiry-row"
              onClick={() => {
                setSelected(q);
                setEdit(false);
                setOpen(true);
              }}
            >
              <span className="inquiry-icon">
                <MessageCircle size={19} />
              </span>
              <div>
                <Badge tone={q.status === "ANSWERED" ? "green" : "gray"}>
                  {q.status === "ANSWERED" ? "답변 완료" : "답변 대기"}
                </Badge>
                <h3>{q.title}</h3>
                <p>
                  {q.category === "REPORT" ? "신고" : "일반 문의"} ·{" "}
                  {dateText(q.createdAt)}
                </p>
              </div>
              <ChevronRight size={17} />
            </button>
          ))}
        </div>
      ) : (
        <Empty
          title="아직 남긴 문의가 없어요"
          description="서비스 이용 중 궁금한 점이 있다면 편하게 남겨주세요."
          icon={<MessageCircle size={30} />}
        />
      )}
      <div className="inquiry-note">
        문의 내용과 답변은 작성한 본인만 확인할 수 있어요.
      </div>
      {open && (
        <Modal
          title={
            selected
              ? edit
                ? "문의 수정"
                : "내 문의 상세"
              : report
                ? "정보 신고하기"
                : "문의하기"
          }
          onClose={close}
        >
          {selected && !edit ? (
            <InquiryDetail
              id={selected.id}
              onEdit={(q) => {
                setSelected(q);
                setEdit(true);
              }}
              onDeleted={() => {
                close();
                reload();
              }}
            />
          ) : (
            <InquiryForm
              existing={selected || undefined}
              report={report}
              onSave={() => {
                close();
                reload();
              }}
            />
          )}
        </Modal>
      )}
    </>
  );
}
function InquiryDetail({
  id,
  onEdit,
  onDeleted,
}: {
  id: string;
  onEdit: (q: Inquiry) => void;
  onDeleted: () => void;
}) {
  const { api } = useApp();
  const { data, loading, error, reload } = useLoad(
    () => api.inquiry(id),
    [api, id],
  );
  const [confirm, setConfirm] = useState(false);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState("");
  if (loading) return <Loading />;
  if (error) return <ErrorState message={error} retry={reload} />;
  if (!data) return null;
  return (
    <>
      <Badge tone={data.status === "ANSWERED" ? "green" : "gray"}>
        {data.status === "ANSWERED" ? "답변 완료" : "답변 대기"}
      </Badge>
      <h3 className="inquiry-title">{data.title}</h3>
      <p className="prose">{data.content}</p>
      {data.img && (
        <Photo
          className="inquiry-image"
          src={data.img}
          alt="문의 첨부 이미지"
        />
      )}
      {data.answer && (
        <div className="inquiry-answer">
          <strong>방구석탈출의 답변</strong>
          <p className="prose">{data.answer}</p>
        </div>
      )}
      <FormError message={actionError} />
      {confirm ? (
        <div className="delete-confirm">
          <p>이 문의를 삭제할까요?</p>
          <button
            className="btn secondary small"
            onClick={() => setConfirm(false)}
          >
            취소
          </button>
          <button
            className="btn danger small"
            disabled={busy}
            onClick={async () => {
              setBusy(true);
              try {
                await api.deleteInquiry(id);
                onDeleted();
              } catch (e) {
                setActionError(errorText(e));
              } finally {
                setBusy(false);
              }
            }}
          >
            삭제
          </button>
        </div>
      ) : (
        <div className="form-actions">
          <button
            className="text-button muted"
            onClick={() => setConfirm(true)}
          >
            문의 삭제
          </button>
          {data.status !== "ANSWERED" && (
            <button className="btn secondary" onClick={() => onEdit(data)}>
              <Pencil size={15} />
              수정하기
            </button>
          )}
        </div>
      )}
    </>
  );
}
function InquiryForm({
  existing,
  report,
  onSave,
}: {
  existing?: Inquiry;
  report: string;
  onSave: () => void;
}) {
  const { api, toast, mode } = useApp();
  const [category, setCategory] = useState<InquiryInput["category"]>(
    existing?.category || (report ? "REPORT" : "QUESTION"),
  );
  const [title, setTitle] = useState(existing?.title || "");
  const [content, setContent] = useState(existing?.content || report);
  const [img, setImg] = useState<string | undefined>();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!title.trim() || !content.trim()) {
      setError("제목과 내용을 입력해 주세요.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      const body = {
        category,
        title: title.trim(),
        content: content.trim(),
        ...(img !== undefined ? { img } : {}),
      };
      if (existing) await api.updateInquiry(existing.id, body);
      else await api.createInquiry(body);
      toast(
        mode === "preview"
          ? "미리보기 문의를 저장했어요."
          : "문의를 저장했어요.",
      );
      onSave();
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <form onSubmit={submit}>
      <Field label="문의 종류" required>
        <select
          value={category}
          onChange={(e) =>
            setCategory(e.target.value as InquiryInput["category"])
          }
        >
          <option value="QUESTION">일반 문의</option>
          <option value="REPORT">잘못된 정보·게시물 신고</option>
        </select>
      </Field>
      <Field label="제목" required>
        <input
          required
          maxLength={255}
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="어떤 도움이 필요하신가요?"
        />
      </Field>
      <Field label="내용" required>
        <textarea
          required
          rows={5}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="자세히 알려주시면 확인에 도움이 돼요."
        />
      </Field>
      <Upload type="INQUIRY" value={existing?.img} onChange={setImg} useKey />
      <FormError message={error} />
      <div className="form-actions">
        <SubmitButton busy={busy}>
          {existing ? "수정 내용 저장" : "문의 남기기"}
        </SubmitButton>
      </div>
    </form>
  );
}
