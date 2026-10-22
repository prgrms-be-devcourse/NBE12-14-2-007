import type { FestivalDeletion } from "../lib/types";
import { dateText } from "../lib/format";

export function FestivalDeletionNotice({
  deletion,
}: {
  deletion: FestivalDeletion;
}) {
  const actor =
    deletion.type === "ADMIN"
      ? "관리자 삭제"
      : deletion.type === "AUTHOR"
        ? "작성자 삭제"
        : "삭제 주체 확인 불가";
  return (
    <section className="festival-deletion-notice" aria-label="행사 삭제 정보">
      <h2>삭제된 행사입니다</h2>
      <p>
        {actor} · {dateText(deletion.deletedAt)}
      </p>
      <p>삭제 사유: {deletion.reason || "기록된 사유가 없습니다."}</p>
    </section>
  );
}
