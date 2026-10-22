import { Field } from "../components/ui";
import { deletionReasons, type AdminDeletionReason } from "../lib/deletion";

export function DeletionReasonFields({
  code,
  detail,
  disabled,
  onCode,
  onDetail,
  hint,
}: {
  code: AdminDeletionReason;
  detail: string;
  disabled: boolean;
  onCode: (value: AdminDeletionReason) => void;
  onDetail: (value: string) => void;
  hint?: string;
}) {
  return (
    <>
      <Field label="삭제 사유 분류">
        <select
          value={code}
          disabled={disabled}
          onChange={(event) =>
            onCode(event.target.value as AdminDeletionReason)
          }
        >
          {Object.entries(deletionReasons).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </Field>
      <Field
        label="상세 사유"
        hint={
          hint ?? (code === "OTHER"
            ? "기타는 상세 사유가 필요합니다."
            : "추가 설명은 선택입니다.")
        }
      >
        <textarea
          value={detail}
          disabled={disabled}
          maxLength={500}
          required={code === "OTHER"}
          onChange={(event) => onDetail(event.target.value)}
        />
      </Field>
    </>
  );
}
