package com.team007.room_escape.domain.festival.infra.repository;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FestivalRepository extends JpaRepository<Festival, Long> {

/** 저장 건수 조회 메서드 **/
	long countByProviderType(ProviderType providerType);

	/** 연도 범위(yearStart 이상 ~ yearEnd 미만)에 해당하는 저장 건수 조회 */
	long countByProviderTypeAndBeginDeGreaterThanEqualAndBeginDeLessThan(
		ProviderType providerType, LocalDateTime yearStart, LocalDateTime yearEnd);

	// TODO: PostgreSQL의 null 파라미터 타입 오류로 사용 중인 hasXxx 플래그를 QueryDSL 도입 시 제거
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

	/** 종료일이 지났는데 아직 OPEN인 행사 조회 (CLOSED로 갱신하기 전에, 어떤 행사가 바뀌는지 응답에 담으려고) */
	List<Festival> findByStatusAndEndDeBefore(FestivalStatus status, LocalDateTime now);

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

	Optional<Festival> findByIdAndDeletedAtIsNull(Long festivalId);

	/** 신규 등록용 중복 확인 */
	boolean existsByBeginDeAndEndDeAndRegionAndUrlAndDeletedAtIsNull(
			LocalDateTime beginDe,
			LocalDateTime endDe,
			FestivalRegion region,
			String url
	);
	/** 행사 수정일 경우, 본인 행사 제외하고 중복 확인 */
	boolean existsByIdNotAndBeginDeAndEndDeAndRegionAndUrlAndDeletedAtIsNull(
			Long festivalId,
			LocalDateTime beginDe,
			LocalDateTime endDe,
			FestivalRegion region,
			String url
	);
}
