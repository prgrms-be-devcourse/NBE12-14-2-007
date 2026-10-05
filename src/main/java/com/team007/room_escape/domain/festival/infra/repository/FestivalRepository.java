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

	/** 저장 건수 조회 */
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

	/** 기본 정렬 검색. 곧 시작하는 행사 먼저, 이미 시작한 행사는 최근 시작순으로 뒤에 둔다. */
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

	/** 좋아요순 검색. 좋아요를 행사별로 먼저 집계한 뒤 붙인다. */
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

	/** 종료일이 지났는데 아직 OPEN인 행사 조회 */
	List<Festival> findByStatusAndEndDeBefore(FestivalStatus status, LocalDateTime now);

	/** 아직 R2로 이관되지 않은(외부 URL을 쓰는) 행사 이미지 조회 */
	@Query("SELECT f FROM Festival f WHERE f.imgUrl IS NOT NULL AND f.imgUrl NOT LIKE CONCAT(:r2PublicUrl, '%')")
	Page<Festival> findLegacyImages(@Param("r2PublicUrl") String r2PublicUrl, Pageable pageable);

	/** 종료일이 지난 OPEN 행사를 CLOSED로 일괄 갱신한다. 벌크 UPDATE라 영속성 컨텍스트를 비운다. */
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

	/** 관리자 행사 검색. keyword는 null 대신 빈 문자열을 받는다. */
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
