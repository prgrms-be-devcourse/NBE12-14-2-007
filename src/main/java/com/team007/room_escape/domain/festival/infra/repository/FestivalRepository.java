package com.team007.room_escape.domain.festival.infra.repository;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface FestivalRepository extends JpaRepository<Festival, Long> {

    @Query(value = """
            SELECT f FROM Festival f LEFT JOIN FETCH f.member
            WHERE f.providerType = :providerType AND f.deletedAt IS NULL
              AND EXISTS (SELECT s.id FROM FestivalSubmission s WHERE s.festival = f AND s.deletedAt IS NULL)
              AND LOWER(f.title) LIKE LOWER(:pattern) ESCAPE '!'
              AND (:closed IS NULL
                OR (:closed = TRUE AND f.endDe < :now)
                OR (:closed = FALSE AND (f.endDe IS NULL OR f.endDe >= :now)))
            """, countQuery = """
            SELECT COUNT(f) FROM Festival f
            WHERE f.providerType = :providerType AND f.deletedAt IS NULL
              AND EXISTS (SELECT s.id FROM FestivalSubmission s WHERE s.festival = f AND s.deletedAt IS NULL)
              AND LOWER(f.title) LIKE LOWER(:pattern) ESCAPE '!'
              AND (:closed IS NULL
                OR (:closed = TRUE AND f.endDe < :now)
                OR (:closed = FALSE AND (f.endDe IS NULL OR f.endDe >= :now)))
            """)
    Page<Festival> findSharedSubmissions(
            @Param("providerType") ProviderType providerType,
            @Param("pattern") String pattern,
            @Param("closed") Boolean closed,
            @Param("now") LocalDateTime now,
            Pageable page);

    @Query("""
            SELECT f FROM Festival f LEFT JOIN FETCH f.member
            WHERE f.id = :festivalId AND f.deletedAt IS NULL
              AND (f.providerType = :publicType
                OR EXISTS (SELECT s.id FROM FestivalSubmission s WHERE s.festival = f AND s.deletedAt IS NULL))
            """)
    Optional<Festival> findSharedFestival(@Param("festivalId") Long festivalId,
                                          @Param("publicType") ProviderType publicType);

/** 저장 건수 조회 메서드 **/
	long countByProviderType(ProviderType providerType);

	/** 연도 범위(yearStart 이상 ~ yearEnd 미만)에 해당하는 저장 건수 조회 */
	long countByProviderTypeAndBeginDeGreaterThanEqualAndBeginDeLessThan(
		ProviderType providerType, LocalDateTime yearStart, LocalDateTime yearEnd);

	/** 지역 + 선택한 날짜에 진행 중인 행사 목록 조회 (beginDe <= date <= endDe, 삭제된 행사 제외)
	 *  endDe가 없는 행사(비어있는 API 데이터)는 종료일 미상으로 보고 계속 진행 중인 것으로 취급 */
	@Query("SELECT f FROM Festival f "
		+ "WHERE f.providerType = :providerType AND f.region = :region AND f.deletedAt IS NULL "
		+ "AND f.beginDe <= :date AND (f.endDe IS NULL OR f.endDe >= :date)")
	Page<Festival> findOngoingByProviderTypeAndRegion(
		@Param("providerType") ProviderType providerType,
		@Param("region") FestivalRegion region,
		@Param("date") LocalDateTime date,
		Pageable pageable);

	/** endDe가 지난 OPEN 행사를 CLOSED로 일괄 갱신 (반환값: 갱신된 건수)
	 *  clearAutomatically=true: 벌크 UPDATE는 영속성 컨텍스트를 거치지 않아서,
	 *  같은 트랜잭션에서 이후에 Festival을 다시 조회하면 캐시된 옛날 status를 볼 수 있음 -> 캐시 비우기 */
	@Modifying(clearAutomatically = true)
	@Query("UPDATE Festival f SET f.status = com.team007.room_escape.domain.festival.infra.entity.FestivalStatus.CLOSED "
		+ "WHERE f.status = com.team007.room_escape.domain.festival.infra.entity.FestivalStatus.OPEN AND f.endDe < :now")
	int closeExpiredFestivals(@Param("now") LocalDateTime now);

	Optional<Festival> findByIdAndProviderTypeAndDeletedAtIsNull(
			Long festivalId,
			ProviderType providerType
	);
}
