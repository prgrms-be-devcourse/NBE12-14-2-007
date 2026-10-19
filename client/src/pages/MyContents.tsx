import { useState } from "react";
import { Link } from "react-router-dom";
import { Badge, Empty, ErrorState, Loading, Modal } from "../components/ui";
import { useApp, useLoad } from "../lib/context";
import { dateText } from "../lib/format";
import type { DeletionTarget, MyContentItem } from "../lib/deletion";

const labels: Record<DeletionTarget, string> = {
  FESTIVAL: "행사 제보",
  POST: "후기",
  COMMUNITY_POST: "커뮤니티 글",
  COMMENT: "후기 댓글",
  COMMUNITY_COMMENT: "커뮤니티 댓글",
};

export function MyContents({ comments }: { comments: boolean }) {
  const { api } = useApp();
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<MyContentItem | null>(null);
  const result = useLoad(
    () => api.myContents(comments, page),
    [api, comments, page],
  );
  return (
    <section aria-label={comments ? "내가 작성한 댓글" : "내가 작성한 글"}>
      <h2>{comments ? "내가 작성한 댓글" : "내가 작성한 글"}</h2>
      {!comments && (
        <p>
          <Link className="btn secondary" to="/submissions/new">
            행사 제보하기
          </Link>
        </p>
      )}
      {result.loading ? (
        <Loading />
      ) : result.error ? (
        <ErrorState message={result.error} retry={result.reload} />
      ) : !result.data?.content.length ? (
        <Empty title="작성한 내용이 없습니다" />
      ) : (
        <>
          <div className="my-content-table-wrap">
            <table className="my-content-table">
              <thead>
                <tr>
                  <th>분류</th>
                  <th>{comments ? "댓글" : "제목"}</th>
                  <th>상태</th>
                  <th>작성일</th>
                  <th>확인</th>
                </tr>
              </thead>
              <tbody>
                {result.data.content.map((item) => (
                  <tr
                    key={`${item.type}-${item.targetId}`}
                    className={item.deletedAt ? "my-content-deleted" : ""}
                  >
                    <td>
                      <Badge tone="gray">{labels[item.type]}</Badge>
                    </td>
                    <td className="my-content-title">
                      {item.title || "제목 없음"}
                    </td>
                    <td>
                      {item.deletedAt
                        ? "삭제됨"
                        : item.parentDeleted
                          ? "원문 삭제"
                          : "게시 중"}
                    </td>
                    <td>{dateText(item.createdAt)}</td>
                    <td>
                      {item.deletedAt || item.parentDeleted ? (
                        <button
                          type="button"
                          className="btn secondary"
                          onClick={() => setSelected(item)}
                        >
                          {item.deletedAt ? "삭제 사유 보기" : "원문 삭제 안내"}
                        </button>
                      ) : (
                        item.originalPath && (
                          <Link
                            className="btn secondary"
                            to={item.originalPath}
                          >
                            원문 보기
                          </Link>
                        )
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <nav className="my-content-pagination" aria-label="내 콘텐츠 페이지">
            <button
              className="btn secondary"
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
            >
              이전
            </button>
            <span>
              {page + 1} / {result.data.totalPages}
            </span>
            <button
              className="btn secondary"
              disabled={page + 1 >= result.data.totalPages}
              onClick={() => setPage(page + 1)}
            >
              다음
            </button>
          </nav>
        </>
      )}
      {selected && (
        <Modal
          title={selected.deletedAt ? "삭제 사유" : "원문 삭제 안내"}
          onClose={() => setSelected(null)}
        >
          <h3>{selected.title}</h3>
          {selected.deletedAt ? (
            <>
              <p>삭제일: {dateText(selected.deletedAt)}</p>
              <p>
                {selected.deletion?.action === "DELETE"
                  ? `${selected.deletion.actorType === "ADMIN" ? "관리자 삭제" : "작성자 삭제"} · ${selected.deletion.reasonLabel}`
                  : "기록된 삭제 사유가 없습니다."}
              </p>
              {selected.deletion?.action === "DELETE" &&
                selected.deletion.reasonDetail && (
                  <p>{selected.deletion.reasonDetail}</p>
                )}
            </>
          ) : (
            <p>
              부모 글이 삭제되어 원문으로 이동할 수 없습니다. 이 댓글 자체가
              삭제된 것은 아닙니다.
            </p>
          )}
          <button className="btn secondary" onClick={() => setSelected(null)}>
            닫기
          </button>
        </Modal>
      )}
    </section>
  );
}
