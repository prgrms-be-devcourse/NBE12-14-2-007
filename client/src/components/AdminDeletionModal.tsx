import { useState } from "react";
import { DeletionReasonFields } from "../admin/DeletionReasonFields";
import { useApp } from "../lib/context";
import type { AdminDeletionReason, DeletionTarget } from "../lib/deletion";
import { errorText } from "../lib/format";
import { FormError, Modal } from "./ui";

/** 공개 원문에서도 관리자 삭제는 사유를 남기는 공통 API를 사용한다. */
export function AdminDeletionModal({
  targetType,
  targetId,
  onClose,
  onDeleted,
}: {
  targetType: DeletionTarget;
  targetId: string | number;
  onClose: () => void;
  onDeleted: () => void;
}) {
  const { api } = useApp();
  const [code, setCode] = useState<AdminDeletionReason>(
    "INCORRECT_INFORMATION",
  );
  const [detail, setDetail] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  return (
    <Modal
      title="관리자 권한으로 삭제할까요?"
      onClose={() => {
        if (!busy) onClose();
      }}
    >
      <p className="muted">
        삭제 사유가 작성자에게 안내되고 처리 이력에 기록됩니다.
      </p>
      <DeletionReasonFields
        code={code}
        detail={detail}
        disabled={busy}
        onCode={setCode}
        onDetail={setDetail}
      />
      <FormError message={error} />
      <div className="form-actions">
        <button className="btn secondary" disabled={busy} onClick={onClose}>
          취소
        </button>
        <button
          className="btn danger"
          disabled={busy || (code === "OTHER" && !detail.trim())}
          onClick={async () => {
            setBusy(true);
            setError("");
            try {
              await api.adminDeleteContent(targetType, targetId, code, detail);
              onDeleted();
              onClose();
            } catch (e) {
              setError(errorText(e));
            } finally {
              setBusy(false);
            }
          }}
        >
          {busy ? "삭제 중…" : "삭제"}
        </button>
      </div>
    </Modal>
  );
}
