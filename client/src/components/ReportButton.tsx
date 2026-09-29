import { useState, type FormEvent, type ReactNode } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { ApiError } from "../lib/api";
import { useApp } from "../lib/context";
import { errorText } from "../lib/format";
import type { InquiryTargetType } from "../lib/types";
import { Field, FormError, Modal, SubmitButton, Upload } from "./ui";

type ReportKind = "REPORT" | "TIP";

export interface ReportTarget {
  type: InquiryTargetType;
  id: string;
  /** 모달 안내 문구와 접수 제목에 쓰는 이름. 행사명, 후기 제목, 닉네임 등 */
  name: string;
  /** 관리자가 대상을 찾는 데 도움이 되는 추가 정보. 접수 본문에 한 줄씩 붙는다. */
  details?: string[];
}

const kindInfo = {
  REPORT: {
    label: "신고",
    description: "광고, 욕설 등 부적절한 콘텐츠를 알려주세요.",
  },
  TIP: {
    label: "제보",
    description: "잘못되거나 누락된 행사 정보를 알려주세요.",
  },
} as const;

const targetNames: Record<InquiryTargetType, string> = {
  FESTIVAL: "행사",
  POST: "후기",
  COMMENT: "댓글",
  MEMBER: "회원",
};

/** "기타"를 고르면 상세 내용을 필수로 받는다. 제보는 행사에만 받는다. */
const reasons: Record<
  InquiryTargetType,
  Partial<Record<ReportKind, readonly string[]>>
> = {
  FESTIVAL: {
    REPORT: ["부적절한 내용", "광고·홍보성 콘텐츠", "기타"],
    TIP: ["허위·잘못된 정보", "취소·종료된 행사", "누락된 정보", "기타"],
  },
  POST: {
    REPORT: ["욕설·비방", "광고·홍보성 콘텐츠", "행사와 관계없는 내용", "기타"],
  },
  COMMENT: {
    REPORT: ["욕설·비방", "광고·홍보성 콘텐츠", "도배", "기타"],
  },
  MEMBER: {
    REPORT: [
      "욕설·비방을 반복해요",
      "광고·도배 계정이에요",
      "다른 사람을 사칭해요",
      "기타",
    ],
  },
};

/**
 * 신고·제보 버튼. 누르면 모달에서 사유를 골라 문의 API로 접수한다.
 * 대상 종류와 ID를 따로 보내 관리자가 문의 상세에서 대상으로 바로 이동할 수 있게 한다.
 */
export function ReportButton({
  target,
  className = "text-button muted",
  ariaLabel,
  title,
  children,
}: {
  target: ReportTarget;
  className?: string;
  ariaLabel?: string;
  title?: string;
  children: ReactNode;
}) {
  const { member, authLoading } = useApp();
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const kinds = Object.keys(reasons[target.type]) as ReportKind[];
  const targetName = targetNames[target.type];

  function start() {
    if (!member) {
      navigate(
        `/login?next=${encodeURIComponent(location.pathname + location.search)}`,
      );
      return;
    }
    setOpen(true);
  }

  return (
    <>
      <button
        type="button"
        className={className}
        aria-label={ariaLabel}
        title={title}
        disabled={authLoading}
        onClick={start}
      >
        {children}
      </button>
      {open && (
        <Modal
          title={
            kinds.length > 1
              ? `${targetName} 신고 및 제보`
              : `${targetName} ${kindInfo[kinds[0]].label}`
          }
          onClose={() => setOpen(false)}
        >
          <ReportForm
            target={target}
            kinds={kinds}
            onDone={() => setOpen(false)}
          />
        </Modal>
      )}
    </>
  );
}

/** 서버가 탈퇴 회원의 닉네임 대신 내려주는 값 (Member.WITHDRAWN_NICKNAME) */
const WITHDRAWN_NICKNAME = "탈퇴한 사용자";

/**
 * 작성자 닉네임. 눌러서 그 회원을 신고할 수 있다.
 * 본인, 탈퇴 회원, 비로그인 상태에서는 그냥 이름만 보여준다.
 */
export function ReportableName({
  memberId,
  nickname,
}: {
  memberId: string | null | undefined;
  nickname: string;
}) {
  const { member } = useApp();
  if (
    !member ||
    !memberId ||
    memberId === member.id ||
    nickname === WITHDRAWN_NICKNAME
  )
    return <strong>{nickname}</strong>;
  return (
    <ReportButton
      className="member-report-name"
      ariaLabel={`${nickname} 회원 신고`}
      title="눌러서 회원 신고"
      target={{ type: "MEMBER", id: memberId, name: nickname }}
    >
      <strong>{nickname}</strong>
    </ReportButton>
  );
}

function ReportForm({
  target,
  kinds,
  onDone,
}: {
  target: ReportTarget;
  kinds: ReportKind[];
  onDone: () => void;
}) {
  const { api, toast } = useApp();
  const [kind, setKind] = useState<ReportKind | null>(
    kinds.length === 1 ? kinds[0] : null,
  );
  const [reason, setReason] = useState<string | null>(null);
  const [detail, setDetail] = useState("");
  const [img, setImg] = useState<string | undefined>();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const targetName = targetNames[target.type];
  const detailRequired = reason === "기타";

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!kind) {
      setError("신고와 제보 중 접수 유형을 선택해 주세요.");
      return;
    }
    const kindLabel = kindInfo[kind].label;
    if (!reason) {
      setError(`${kindLabel} 사유를 선택해 주세요.`);
      return;
    }
    if (detailRequired && !detail.trim()) {
      setError("기타 사유는 내용을 적어 주세요.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      await api.createInquiry({
        category: kind,
        targetType: target.type,
        targetId: target.id,
        title: `[${targetName} ${kindLabel}] ${target.name} - ${reason}`.slice(
          0,
          255,
        ),
        content: [
          `${kindLabel} 사유: ${reason}`,
          `${targetName}: ${target.name} (ID ${target.id})`,
          ...(target.details ?? []),
          ...(detail.trim() ? ["", detail.trim()] : []),
        ].join("\n"),
        ...(img ? { img } : {}),
      });
      toast(
        `${kindLabel}가 접수됐어요. 마이페이지에서 처리 결과를 볼 수 있어요.`,
      );
      onDone();
    } catch (e) {
      setError(
        e instanceof ApiError && e.code === "INQUIRY005"
          ? `이미 삭제되었거나 찾을 수 없는 ${targetName}이에요.`
          : errorText(e),
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="report-form" onSubmit={submit}>
      <p className="muted">
        <strong>{target.name}</strong>
        {kinds.length > 1
          ? "에 대해 어떤 내용을 접수하시나요?"
          : ` ${targetName} 신고 사유를 알려주세요.`}
      </p>
      {kinds.length > 1 && (
        <fieldset className="report-reasons report-kinds">
          <legend>
            접수 유형<b className="required">*</b>
          </legend>
          {kinds.map((value) => (
            <label key={value} className={kind === value ? "selected" : ""}>
              <input
                type="radio"
                name="inquiry-kind"
                value={value}
                checked={kind === value}
                onChange={() => {
                  setKind(value);
                  setReason(null);
                  setError("");
                }}
              />
              <span>
                <strong>{kindInfo[value].label}</strong>
                <small>{kindInfo[value].description}</small>
              </span>
            </label>
          ))}
        </fieldset>
      )}
      {kind && (
        <fieldset className="report-reasons">
          <legend>
            {kindInfo[kind].label} 사유
            <b className="required">*</b>
          </legend>
          {reasons[target.type][kind]!.map((label) => (
            <label key={label} className={reason === label ? "selected" : ""}>
              <input
                type="radio"
                name="report-reason"
                value={label}
                checked={reason === label}
                onChange={() => setReason(label)}
              />
              {label}
            </label>
          ))}
        </fieldset>
      )}
      <Field
        label="상세 내용"
        required={detailRequired}
        hint={detailRequired ? undefined : "선택 항목이에요."}
      >
        <textarea
          rows={4}
          maxLength={2000}
          value={detail}
          onChange={(e) => setDetail(e.target.value)}
          placeholder="확인한 내용을 적어 주시면 처리에 도움이 돼요."
        />
      </Field>
      <Upload type="INQUIRY" onChange={setImg} useKey />
      <FormError message={error} />
      <div className="form-actions">
        <button type="button" className="btn secondary" onClick={onDone}>
          취소
        </button>
        <SubmitButton busy={busy}>
          {kind ? `${kindInfo[kind].label} 접수하기` : "접수하기"}
        </SubmitButton>
      </div>
    </form>
  );
}
