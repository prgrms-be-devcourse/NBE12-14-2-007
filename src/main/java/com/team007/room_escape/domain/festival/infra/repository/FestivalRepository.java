package com.team007.room_escape.domain.festival.infra.repository;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestivalRepository extends JpaRepository<Festival, Long> {
/** 저장 건수 조회 메서드 **/
	long countByProviderType(ProviderType providerType);

	/** 연도 범위(yearStart 이상 ~ yearEnd 미만)에 해당하는 저장 건수 조회 */
	long countByProviderTypeAndBeginDeGreaterThanEqualAndBeginDeLessThan(
		ProviderType providerType, LocalDateTime yearStart, LocalDateTime yearEnd);
}
