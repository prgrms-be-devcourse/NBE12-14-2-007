package com.team007.room_escape.domain.festival.infra.repository;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface FestivalRepository extends JpaRepository<Festival, Long> {

/** 저장 건수 조회 메서드 **/
	long countByProviderType(ProviderType providerType);

	/** 연도 범위(yearStart 이상 ~ yearEnd 미만)에 해당하는 저장 건수 조회 */
	long countByProviderTypeAndBeginDeGreaterThanEqualAndBeginDeLessThan(
		ProviderType providerType, LocalDateTime yearStart, LocalDateTime yearEnd);

	/** endDe가 지난 OPEN 행사를 CLOSED로 일괄 갱신 (반환값: 갱신된 건수)
	 *  clearAutomatically=true: 벌크 UPDATE는 영속성 컨텍스트를 거치지 않아서,
	 *  같은 트랜잭션에서 이후에 Festival을 다시 조회하면 캐시된 옛날 status를 볼 수 있음 -> 캐시 비우기 */
	@Modifying(clearAutomatically = true)
	@Query("UPDATE Festival f SET f.status = com.team007.room_escape.domain.festival.infra.entity.FestivalStatus.CLOSED "
		+ "WHERE f.status = com.team007.room_escape.domain.festival.infra.entity.FestivalStatus.OPEN AND f.endDe < :now")
	int closeExpiredFestivals(@Param("now") LocalDateTime now);
}
