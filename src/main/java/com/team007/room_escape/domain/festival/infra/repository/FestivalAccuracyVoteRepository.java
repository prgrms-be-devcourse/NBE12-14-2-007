package com.team007.room_escape.domain.festival.infra.repository;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalAccuracyVote;
import com.team007.room_escape.domain.festival.infra.entity.FestivalAccuracyVoteType;
import com.team007.room_escape.domain.member.infra.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestivalAccuracyVoteRepository
        extends JpaRepository<FestivalAccuracyVote, Long> {

    Optional<FestivalAccuracyVote> findByFestivalAndMember(
            Festival festival,
            Member member
    );

    long countByFestivalAndVoteType(
            Festival festival,
            FestivalAccuracyVoteType voteType
    );
}
