package com.team007.room_escape.domain.festival.service;

import com.team007.room_escape.domain.festival.dto.FestivalAccuracyVoteRequest.CreateOrUpdateFestivalAccuracyVoteRequest;
import com.team007.room_escape.domain.festival.dto.FestivalAccuracyVoteResponse.AccuracyVoteResponse;
import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalAccuracyVote;
import com.team007.room_escape.domain.festival.infra.entity.FestivalAccuracyVoteType;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalAccuracyVoteRepository;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FestivalAccuracyVoteService {

    private final FestivalAccuracyVoteRepository accuracyVoteRepository;
    private final FestivalRepository festivalRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public AccuracyVoteResponse vote(
            UUID memberId,
            Long festivalId,
            CreateOrUpdateFestivalAccuracyVoteRequest request
    ) {
        Member member = findActiveMember(memberId);
        Festival festival = findUserSubmittedFestival(festivalId);

        FestivalAccuracyVote vote = accuracyVoteRepository
                .findByFestivalAndMember(festival, member)
                .orElseGet(() -> FestivalAccuracyVote.builder()
                        .festival(festival)
                        .member(member)
                        .voteType(request.voteType())
                        .build());

        vote.changeVote(request.voteType());
        accuracyVoteRepository.save(vote);

        return buildResponse(festival, vote.getVoteType());
    }

    @Transactional
    public AccuracyVoteResponse cancelVote(
            UUID memberId,
            Long festivalId
    ) {
        Member member = findActiveMember(memberId);
        Festival festival = findUserSubmittedFestival(festivalId);

        accuracyVoteRepository
                .findByFestivalAndMember(festival, member)
                .ifPresent(accuracyVoteRepository::delete);

        return buildResponse(festival, null);
    }

    private AccuracyVoteResponse buildResponse(
            Festival festival,
            FestivalAccuracyVoteType myVote
    ) {
        accuracyVoteRepository.flush();

        long accurateCount = accuracyVoteRepository
                .countByFestivalAndVoteType(
                        festival,
                        FestivalAccuracyVoteType.ACCURATE
                );

        long inaccurateCount = accuracyVoteRepository
                .countByFestivalAndVoteType(
                        festival,
                        FestivalAccuracyVoteType.INACCURATE
                );

        return AccuracyVoteResponse.of(
                accurateCount,
                inaccurateCount,
                myVote
        );
    }

    private Member findActiveMember(UUID memberId) {
        return memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new BusinessException(
                        MemberExceptionCode.MEMBER_NOT_FOUND
                ));
    }

    private Festival findUserSubmittedFestival(Long festivalId) {
        return festivalRepository
                .findByIdAndProviderTypeAndDeletedAtIsNull(
                        festivalId,
                        ProviderType.MEMBER
                )
                .orElseThrow(() -> new BusinessException(
                        FestivalExceptionCode.FESTIVAL_NOT_FOUND
                ));
    }
}
