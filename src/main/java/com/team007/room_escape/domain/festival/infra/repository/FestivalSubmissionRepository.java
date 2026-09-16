package com.team007.room_escape.domain.festival.infra.repository;

import com.team007.room_escape.domain.festival.infra.entity.FestivalSubmission;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestivalSubmissionRepository extends JpaRepository<FestivalSubmission, UUID> {

    List<FestivalSubmission>
    findAllByFestival_Member_IdAndFestival_ProviderTypeOrderByFestival_WritngDeDesc(
            UUID memberId,
            ProviderType providerType
    );
}
