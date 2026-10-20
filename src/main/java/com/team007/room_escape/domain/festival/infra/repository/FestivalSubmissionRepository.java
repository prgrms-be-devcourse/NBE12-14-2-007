package com.team007.room_escape.domain.festival.infra.repository;

import com.team007.room_escape.domain.festival.infra.entity.FestivalSubmission;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface FestivalSubmissionRepository extends JpaRepository<FestivalSubmission, UUID> {

	@EntityGraph(attributePaths = "festival")
	List<FestivalSubmission>
	findAllByFestival_Member_IdAndFestival_ProviderTypeOrderByCreatedAtDesc(
			UUID memberId,
			ProviderType providerType
	);

	@EntityGraph(attributePaths = "festival")
	Optional<FestivalSubmission>
	findByIdAndFestival_Member_Id(
			UUID submissionId,
			UUID memberId
	);
}
