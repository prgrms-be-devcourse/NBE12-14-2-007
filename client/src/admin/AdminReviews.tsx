import { Link, useSearchParams } from "react-router-dom";
import { ReviewSearch, useReviewQuery } from "../components/ReviewSearch";
import { ReviewRow } from "../components/ReviewRow";
import {
  Empty,
  ErrorState,
  Loading,
  LoginRequired,
  Modal,
  Pagination,
  PageTitle,
  Photo,
} from "../components/ui";
import { ApiError } from "../lib/api";
import { useApp, useLoad } from "../lib/context";
import { dateTimeText } from "../lib/format";

export function AdminReviews() {
  const { member, authLoading } = useApp();
  const [params] = useSearchParams();
  const localDev =
    import.meta.env.DEV &&
    ["localhost", "127.0.0.1", "[::1]"].includes(window.location.hostname);
  const testMode = localDev && params.get("test") === "1";
  return (
    <div className="admin-review-page">
      <PageTitle
        eyebrow="MOMENTS WE SHARE · ADMIN"
        title="후기 관리"
        description="함께 나눈 이야기를 살펴보세요. 삭제된 후기도 여기에서 확인할 수 있어요."
      />
      {testMode && (
        <p className="adm-dialog-note" role="status">
          로그인 없는 로컬 검색 테스트입니다. 실제 DB에서 후기를 조회하며, 상세
          조회와 관리 작업은 로그인 후 사용할 수 있습니다.{" "}
          <Link to="/admin/reviews">테스트 종료</Link>
        </p>
      )}
      {testMode ? (
        <AdminReviewList testMode />
      ) : authLoading ? (
        <Loading />
      ) : !member ? (
        <LoginRequired />
      ) : member.role !== "ROLE_ADMIN" ? (
        <Empty
          title="관리자 권한이 필요합니다"
          description="관리자 계정으로 로그인해 주세요."
        />
      ) : (
        <AdminReviewList />
      )}
      {localDev && !testMode && member?.role !== "ROLE_ADMIN" && (
        <p className="adm-footnote">
          <Link className="btn secondary" to="/admin/reviews?test=1">
            로그인 없이 검색 테스트
          </Link>
        </p>
      )}
    </div>
  );
}

function AdminReviewList({ testMode = false }: { testMode?: boolean }) {
  const { api } = useApp();
  const { page, sort, type, keyword, update } = useReviewQuery();
  const [params, setParams] = useSearchParams();
  const selectedId = params.get("item");
  const { data, loading, error, reload } = useLoad(
    () =>
      api.adminPosts({
        page,
        size: 6,
        sort: sort === "createdAt,asc" ? sort : "createdAt,desc",
        type,
        keyword,
      }),
    [api, page, sort, type, keyword],
  );
  function select(id: string) {
    setParams((current) => {
      const next = new URLSearchParams(current);
      if (id) next.set("item", id);
      else next.delete("item");
      return next;
    });
  }
  return (
    <>
      <section className="festival-posts">
        <ReviewSearch
          type={type}
          keyword={keyword}
          onSearch={(type, keyword) => update({ type, keyword, item: "" })}
        />
        <div className="section-heading">
          <h2 role="status">
            함께 나눈 후기{" "}
            <span className="count">
              {loading || error ? "—" : (data?.totalElements ?? 0)}
            </span>
          </h2>
          <div className="action-row">
            <select
              aria-label="후기 정렬"
              className="plain-select"
              value={sort}
              onChange={(event) =>
                update({ sort: event.target.value, item: "" })
              }
            >
              <option value="createdAt,desc">최신순</option>
              <option value="createdAt,asc">오래된순</option>
            </select>
          </div>
        </div>
        <p className="quiet-note review-sort-note">
          삭제된 후기도 포함됩니다. 삭제된 후기는 ‘삭제됨’ 표시와 삭제 시각으로
          구분할 수 있어요.
        </p>
        {loading ? (
          <Loading />
        ) : error ? (
          <ErrorState message={error} retry={reload} />
        ) : data?.content.length ? (
          <>
            <div className="post-list" aria-label="관리자 후기 목록">
              {data.content.map((post) => (
                <ReviewRow
                  key={post.id}
                  post={post}
                  management={{
                    deletedAt: post.deletedAt,
                    onOpen: testMode ? undefined : () => select(post.id),
                  }}
                />
              ))}
            </div>
            <Pagination
              page={data.number}
              total={data.totalPages}
              onChange={(page) => update({ page, item: "" })}
            />
          </>
        ) : (
          <Empty
            title="조건에 맞는 후기가 없습니다"
            description="검색 조건을 바꾸거나 첫 페이지에서 다시 확인해 주세요."
            action={
              page > 0 ? (
                <button
                  className="btn secondary"
                  onClick={() => update({ page: 0 })}
                >
                  첫 페이지로
                </button>
              ) : undefined
            }
          />
        )}
      </section>
      {selectedId && !testMode && (
        <AdminReviewDetail
          key={selectedId}
          id={selectedId}
          onClose={() => select("")}
        />
      )}
    </>
  );
}

function AdminReviewDetail({
  id,
  onClose,
}: {
  id: string;
  onClose: () => void;
}) {
  const { api } = useApp();
  const { data, loading, error, reload } = useLoad(async () => {
    try {
      return await api.adminPost(id);
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        throw new Error(
          "삭제되었거나 찾을 수 없는 후기입니다. 목록에는 삭제된 후기도 포함됩니다.",
        );
      }
      throw error;
    }
  }, [api, id]);
  return (
    <Modal title="후기 상세" onClose={onClose}>
      {loading ? (
        <Loading />
      ) : error ? (
        <ErrorState message={error} retry={reload} />
      ) : (
        data && (
          <>
            <div className="adm-dialog-title">
              <h3>{data.title}</h3>
              <p>
                {data.member.nickname} · 최근 수정 {dateTimeText(data.date)}
              </p>
            </div>
            <Link className="adm-target-link" to={`/events/${data.festivalId}`}>
              {data.festivalTitle}
            </Link>
            {data.thumbnail && (
              <Photo src={data.thumbnail} alt="후기 첨부 사진" />
            )}
            <p className="adm-content-body">{data.content}</p>
          </>
        )
      )}
      <div className="adm-dialog-actions">
        <button className="btn secondary" onClick={onClose}>
          닫기
        </button>
      </div>
    </Modal>
  );
}
