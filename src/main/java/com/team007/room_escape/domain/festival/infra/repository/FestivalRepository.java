package com.team007.room_escape.domain.festival.infra.repository;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface FestivalRepository extends JpaRepository<Festival, Long> {

    List<Festival> findAllByMember_IdAndProviderTypeOrderByWritngDeDesc(
            UUID memberId,
            ProviderType providerType
    );
}
