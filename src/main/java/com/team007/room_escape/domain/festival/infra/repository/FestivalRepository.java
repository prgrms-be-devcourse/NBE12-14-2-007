package com.team007.room_escape.domain.festival.infra.repository;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FestivalRepository extends JpaRepository<Festival, Long> {

    // PostgreSQL 트랜잭션 종료 시 자동 해제. 같은 중복 조건의 등록/수정을 직렬화한다.
    @Query(value = "SELECT count(*) FROM pg_advisory_xact_lock(:key)", nativeQuery = true)
    long lockSubmissionDuplicateKey(@Param("key") long key);

    @Query(value = """
        SELECT EXISTS (
        SELECT 1 FROM festival f
        WHERE f.deleted_at IS NULL
          AND (CAST(:currentId AS bigint) IS NULL OR f.id <> :currentId)
          AND f.begin_de >= :beginStart AND f.begin_de < :beginEnd
          AND f.end_de >= :endStart AND f.end_de < :endEnd
          AND f.region = :region
          AND rtrim(trim(f.url), '/') = :url
        )
        """, nativeQuery = true)
    boolean existsSubmissionDuplicate(
            @Param("currentId") Long currentId,
            @Param("beginStart") LocalDateTime beginStart,
            @Param("beginEnd") LocalDateTime beginEnd,
            @Param("endStart") LocalDateTime endStart,
            @Param("endEnd") LocalDateTime endEnd,
            @Param("region") String region,
            @Param("url") String url);

/** 저장 건수 조회 메서드 **/
	long countByProviderType(ProviderType providerType);

	/** 연도 범위(yearStart 이상 ~ yearEnd 미만)에 해당하는 저장 건수 조회 */
	long countByProviderTypeAndBeginDeGreaterThanEqualAndBeginDeLessThan(
		ProviderType providerType, LocalDateTime yearStart, LocalDateTime yearEnd);

	// TODO: PostgreSQL의 null 파라미터 타입 오류로 사용 중인 hasXxx 플래그를 QueryDSL 도입 시 제거
	/** 목록 카드에 제보자 닉네임·등급을 보여줘서 회원을 함께 불러온다. */
	@EntityGraph(attributePaths = "member")
	@Query("""
        SELECT f
        FROM Festival f
        WHERE f.deletedAt IS NULL
          AND (:hasKeyword = false
                OR LOWER(COALESCE(f.title, ''))
                    LIKE CONCAT('%', :keyword, '%')
                OR LOWER(COALESCE(f.instNm, ''))
                    LIKE CONCAT('%', :keyword, '%')
                OR LOWER(COALESCE(f.regionDetail, ''))
                    LIKE CONCAT('%', :keyword, '%'))
          AND (:hasRegion = false OR f.region = :region)
          AND (:hasProviderType = false OR f.providerType = :providerType)
          AND (:hasCategory = false OR LOWER(COALESCE(f.category, '')) = :category)
          AND (:hasDate = false OR (f.beginDe < :dateEnd AND (f.endDe IS NULL OR f.endDe >= :dateStart)))
          AND (:excludeClosed = false OR f.endDe IS NULL OR f.endDe >= CURRENT_TIMESTAMP)
        """)
	Page<Festival> searchFestivals(
			@Param("hasKeyword") boolean hasKeyword,
			@Param("keyword") String keyword,

			@Param("hasRegion") boolean hasRegion,
			@Param("region") FestivalRegion region,

			@Param("hasProviderType") boolean hasProviderType,
			@Param("providerType") ProviderType providerType,

			@Param("hasCategory") boolean hasCategory,
			@Param("category") String category,

			@Param("hasDate") boolean hasDate,
			@Param("dateStart") LocalDateTime dateStart,
			@Param("dateEnd") LocalDateTime dateEnd,
			@Param("excludeClosed") boolean excludeClosed,

			Pageable pageable
	);

	/**
	 * 기본 정렬(오늘 기준 가까운 시작일순) 행사 검색. 검색 조건은 searchFestivals 와 같다.
	 * 오늘 이후 시작하는 행사를 가까운 순으로 먼저, 이미 시작한 행사는 최근에 시작한 순으로 그 뒤에 둔다.
	 * 시작일이 없는 행사는 맨 뒤, 같은 시작일이면 id 로 순서를 고정한다.
	 */
	@EntityGraph(attributePaths = "member")
	@Query(
		value = """
        SELECT f
        FROM Festival f
        WHERE f.deletedAt IS NULL
          AND (:hasKeyword = false
                OR LOWER(COALESCE(f.title, ''))
                    LIKE CONCAT('%', :keyword, '%')
                OR LOWER(COALESCE(f.instNm, ''))
                    LIKE CONCAT('%', :keyword, '%')
                OR LOWER(COALESCE(f.regionDetail, ''))
                    LIKE CONCAT('%', :keyword, '%'))
          AND (:hasRegion = false OR f.region = :region)
          AND (:hasProviderType = false OR f.providerType = :providerType)
          AND (:hasCategory = false OR LOWER(COALESCE(f.category, '')) = :category)
          AND (:hasDate = false OR (f.beginDe < :dateEnd AND (f.endDe IS NULL OR f.endDe >= :dateStart)))
          AND (:excludeClosed = false OR f.endDe IS NULL OR f.endDe >= CURRENT_TIMESTAMP)
        ORDER BY
          CASE
            WHEN f.beginDe IS NULL THEN 2
            WHEN f.beginDe >= :today THEN 0
            ELSE 1
          END ASC,
          CASE WHEN f.beginDe >= :today THEN f.beginDe END ASC,
          f.beginDe DESC,
          f.id DESC
        """,
		countQuery = """
        SELECT COUNT(f)
        FROM Festival f
        WHERE f.deletedAt IS NULL
          AND (:hasKeyword = false
                OR LOWER(COALESCE(f.title, ''))
                    LIKE CONCAT('%', :keyword, '%')
                OR LOWER(COALESCE(f.instNm, ''))
                    LIKE CONCAT('%', :keyword, '%')
                OR LOWER(COALESCE(f.regionDetail, ''))
                    LIKE CONCAT('%', :keyword, '%'))
          AND (:hasRegion = false OR f.region = :region)
          AND (:hasProviderType = false OR f.providerType = :providerType)
          AND (:hasCategory = false OR LOWER(COALESCE(f.category, '')) = :category)
          AND (:hasDate = false OR (f.beginDe < :dateEnd AND (f.endDe IS NULL OR f.endDe >= :dateStart)))
          AND (:excludeClosed = false OR f.endDe IS NULL OR f.endDe >= CURRENT_TIMESTAMP)
        """)
	Page<Festival> searchFestivalsOrderByNearestStart(
			@Param("hasKeyword") boolean hasKeyword,
			@Param("keyword") String keyword,

			@Param("hasRegion") boolean hasRegion,
			@Param("region") FestivalRegion region,

			@Param("hasProviderType") boolean hasProviderType,
			@Param("providerType") ProviderType providerType,

			@Param("hasCategory") boolean hasCategory,
			@Param("category") String category,

			@Param("hasDate") boolean hasDate,
			@Param("dateStart") LocalDateTime dateStart,
			@Param("dateEnd") LocalDateTime dateEnd,
			@Param("excludeClosed") boolean excludeClosed,

			@Param("today") LocalDateTime today,

			Pageable pageable
	);

	/**
	 * 좋아요순 행사 검색. 검색 조건은 searchFestivals 와 같다.
	 * like 를 행사별로 먼저 한 번 집계한 뒤 붙인다. (행사마다 세거나 like 를 그대로 JOIN 하면 느리다)
	 * 동점이면 곧 시작하는 행사 먼저(기본 목록과 같은 기준), 그래도 같으면 id 로 순서를 고정한다.
	 */
	@EntityGraph(attributePaths = "member")
	@Query(
		value = """
        SELECT f
        FROM Festival f
        LEFT JOIN (
            SELECT l.festival.id AS festivalId, COUNT(l) AS likeCount
            FROM Like l
            WHERE l.festival IS NOT NULL
            GROUP BY l.festival.id
        ) lc ON lc.festivalId = f.id
        WHERE f.deletedAt IS NULL
          AND (:hasKeyword = false
                OR LOWER(COALESCE(f.title, ''))
                    LIKE CONCAT('%', :keyword, '%')
                OR LOWER(COALESCE(f.instNm, ''))
                    LIKE CONCAT('%', :keyword, '%')
                OR LOWER(COALESCE(f.regionDetail, ''))
                    LIKE CONCAT('%', :keyword, '%'))
          AND (:hasRegion = false OR f.region = :region)
          AND (:hasProviderType = false OR f.providerType = :providerType)
          AND (:hasCategory = false OR LOWER(COALESCE(f.category, '')) = :category)
          AND (:hasDate = false OR (f.beginDe < :dateEnd AND (f.endDe IS NULL OR f.endDe >= :dateStart)))
          AND (:excludeClosed = false OR f.endDe IS NULL OR f.endDe >= CURRENT_TIMESTAMP)
        ORDER BY COALESCE(lc.likeCount, 0) DESC, f.beginDe ASC, f.id DESC
        """,
		countQuery = """
        SELECT COUNT(f)
        FROM Festival f
        WHERE f.deletedAt IS NULL
          AND (:hasKeyword = false
                OR LOWER(COALESCE(f.title, ''))
                    LIKE CONCAT('%', :keyword, '%')
                OR LOWER(COALESCE(f.instNm, ''))
                    LIKE CONCAT('%', :keyword, '%')
                OR LOWER(COALESCE(f.regionDetail, ''))
                    LIKE CONCAT('%', :keyword, '%'))
          AND (:hasRegion = false OR f.region = :region)
          AND (:hasProviderType = false OR f.providerType = :providerType)
          AND (:hasCategory = false OR LOWER(COALESCE(f.category, '')) = :category)
          AND (:hasDate = false OR (f.beginDe < :dateEnd AND (f.endDe IS NULL OR f.endDe >= :dateStart)))
          AND (:excludeClosed = false OR f.endDe IS NULL OR f.endDe >= CURRENT_TIMESTAMP)
        """)
	Page<Festival> searchFestivalsOrderByLikeCount(
			@Param("hasKeyword") boolean hasKeyword,
			@Param("keyword") String keyword,

			@Param("hasRegion") boolean hasRegion,
			@Param("region") FestivalRegion region,

			@Param("hasProviderType") boolean hasProviderType,
			@Param("providerType") ProviderType providerType,

			@Param("hasCategory") boolean hasCategory,
			@Param("category") String category,

			@Param("hasDate") boolean hasDate,
			@Param("dateStart") LocalDateTime dateStart,
			@Param("dateEnd") LocalDateTime dateEnd,
			@Param("excludeClosed") boolean excludeClosed,

			Pageable pageable
	);

	/** 종료일이 지났는데 아직 OPEN인 행사 조회 (CLOSED로 갱신하기 전에, 어떤 행사가 바뀌는지 응답에 담으려고) */
	List<Festival> findByStatusAndEndDeBefore(FestivalStatus status, LocalDateTime now);

	/**
	 * imgUrl이 아직 R2 공개 URL로 시작하지 않는(=외부 원본 URL을 그대로 쓰는) 행사를 조회한다.
	 * 동기화 배치가 돌 때마다 이 중 일부를 R2로 이관한다.
	 */
	@Query("SELECT f FROM Festival f WHERE f.imgUrl IS NOT NULL AND f.imgUrl NOT LIKE CONCAT(:r2PublicUrl, '%')")
	Page<Festival> findLegacyImages(@Param("r2PublicUrl") String r2PublicUrl, Pageable pageable);

	/** endDe가 지난 OPEN 행사를 CLOSED로 일괄 갱신 (반환값: 갱신된 건수)
	 *  clearAutomatically=true: 벌크 UPDATE는 영속성 컨텍스트를 거치지 않아서,
	 *  같은 트랜잭션에서 이후에 Festival을 다시 조회하면 캐시된 옛날 status를 볼 수 있음 -> 캐시 비우기 */
	@Modifying(clearAutomatically = true)
	@Query("UPDATE Festival f SET f.status = com.team007.room_escape.domain.festival.infra.entity.FestivalStatus.CLOSED "
		+ "WHERE f.status = com.team007.room_escape.domain.festival.infra.entity.FestivalStatus.OPEN AND f.endDe < :now")
	int closeExpiredFestivals(@Param("now") LocalDateTime now);

	/** 삭제되지 않은 행사 1건 조회 (공공/민간 구분은 providerType으로) */
	Optional<Festival> findByIdAndProviderTypeAndDeletedAtIsNull(
			Long festivalId,
			ProviderType providerType
	);

	@EntityGraph(attributePaths = "member")
	Optional<Festival> findByIdAndDeletedAtIsNull(Long festivalId);

	boolean existsByIdAndDeletedAtIsNull(Long festivalId);

	/**
	 * 관리자용 행사 검색. includeDeleted 가 true면 삭제된 행사까지 함께 조회한다.
	 * 공개 검색(searchFestivals)과 달리 삭제된 행사를 볼 수 있어야 복구가 가능하다.
	 *
	 * keyword 는 null 대신 빈 문자열을 받는다. null을 LIKE 에 넘기면 Postgres 가
	 * 파라미터 타입을 추론하지 못해 'operator does not exist: text ~~ bytea' 로 실패한다.
	 * 빈 문자열이면 LIKE '%%' 가 되어 조건을 걸지 않은 것과 같다.
	 * enum 인 providerType 은 IS NULL 비교가 정상 동작하므로 플래그가 필요 없다.
	 */
	@Query(value = """
		SELECT f
		FROM Festival f
		WHERE (:includeDeleted = true OR f.deletedAt IS NULL)
		  AND (LOWER(COALESCE(f.title, '')) LIKE CONCAT('%', :keyword, '%')
		       OR LOWER(COALESCE(f.instNm, '')) LIKE CONCAT('%', :keyword, '%')
		       OR LOWER(COALESCE(f.regionDetail, '')) LIKE CONCAT('%', :keyword, '%'))
		  AND (:providerType IS NULL OR f.providerType = :providerType)
		  AND (:excludeClosed = false OR f.endDe IS NULL OR f.endDe >= CURRENT_TIMESTAMP)
		""",
		countQuery = """
		SELECT COUNT(f)
		FROM Festival f
		WHERE (:includeDeleted = true OR f.deletedAt IS NULL)
		  AND (LOWER(COALESCE(f.title, '')) LIKE CONCAT('%', :keyword, '%')
		       OR LOWER(COALESCE(f.instNm, '')) LIKE CONCAT('%', :keyword, '%')
		       OR LOWER(COALESCE(f.regionDetail, '')) LIKE CONCAT('%', :keyword, '%'))
		  AND (:providerType IS NULL OR f.providerType = :providerType)
		  AND (:excludeClosed = false OR f.endDe IS NULL OR f.endDe >= CURRENT_TIMESTAMP)
		""")
	Page<Festival> searchForAdmin(
		@Param("keyword") String keyword,
		@Param("providerType") ProviderType providerType,
		@Param("includeDeleted") boolean includeDeleted,
		@Param("excludeClosed") boolean excludeClosed,
		Pageable pageable
	);
}
