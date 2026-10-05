package com.team007.room_escape.domain.festival.infra.repository;

import com.team007.room_escape.domain.festival.infra.dto.FestivalVoteCount;
import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalAccuracyVote;
import com.team007.room_escape.domain.festival.infra.entity.FestivalAccuracyVoteType;
import com.team007.room_escape.domain.member.infra.entity.Member;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FestivalAccuracyVoteRepository
		extends JpaRepository<FestivalAccuracyVote, Long> {

	Optional<FestivalAccuracyVote> findByFestivalAndMember(
			Festival festival,
			Member member
	);

	Optional<FestivalAccuracyVote> findByFestivalIdAndMemberId(
			Long festivalId,
			UUID memberId
	);

	long countByFestivalAndVoteType(
			Festival festival,
			FestivalAccuracyVoteType voteType
	);

	/** 목록 한 페이지에 담긴 행사들의 투표 수를 종류별로 한 번에 센다. 투표가 없는 행사는 결과에 없다. */
	@Query("""
		SELECT new com.team007.room_escape.domain.festival.infra.dto.FestivalVoteCount(
			v.festival.id, v.voteType, COUNT(v))
		FROM FestivalAccuracyVote v
		WHERE v.festival.id IN :festivalIds
		GROUP BY v.festival.id, v.voteType
		""")
	List<FestivalVoteCount> countByFestivalIds(@Param("festivalIds") Collection<Long> festivalIds);

	/** 회원이 작성한 삭제되지 않은 행사 제보들이 받은 '정확해요' 합계 */
	@Query("""
		SELECT COUNT(v)
		FROM FestivalAccuracyVote v
		JOIN v.festival f
		WHERE f.member.id = :authorId
		  AND f.providerType = com.team007.room_escape.domain.festival.infra.entity.ProviderType.MEMBER
		  AND f.deletedAt IS NULL
		  AND v.voteType = com.team007.room_escape.domain.festival.infra.entity.FestivalAccuracyVoteType.ACCURATE
		  AND v.member.id <> f.member.id
		""")
	long countReceivedAccurateVotesByAuthorId(@Param("authorId") UUID authorId);
}
