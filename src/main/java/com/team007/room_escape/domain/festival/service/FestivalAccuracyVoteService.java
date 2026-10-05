package com.team007.room_escape.domain.festival.service;

import com.team007.room_escape.domain.festival.dto.FestivalAccuracyVoteRequest;
import com.team007.room_escape.domain.festival.dto.FestivalAccuracyVoteResponse;
import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalAccuracyVote;
import com.team007.room_escape.domain.festival.infra.entity.FestivalAccuracyVoteType;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalAccuracyVoteRepository;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.service.MemberReader;
import com.team007.room_escape.domain.member.service.MemberTrustGradeService;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FestivalAccuracyVoteService {

    private final FestivalAccuracyVoteRepository accuracyVoteRepository;
    private final FestivalRepository festivalRepository;
    private final MemberReader memberReader;
    private final MemberTrustGradeService memberTrustGradeService;

    @Transactional
    public FestivalAccuracyVoteResponse.Info vote(
            UUID memberId,
            Long festivalId,
            FestivalAccuracyVoteRequest.Upsert request
    ) {
        Member member = memberReader.getActiveMember(memberId);
        Festival festival = findUserSubmittedFestival(festivalId);

        if (festival.getMember() != null
                && festival.getMember().getId().equals(memberId)) {
            throw new BusinessException(
                    FestivalExceptionCode.SELF_ACCURACY_VOTE_NOT_ALLOWED
            );
        }

        FestivalAccuracyVote vote = accuracyVoteRepository
                .findByFestivalAndMember(festival, member)
                .orElseGet(() -> FestivalAccuracyVote.builder()
                        .festival(festival)
                        .member(member)
                        .voteType(request.voteType())
                        .build());

        vote.changeVote(request.voteType());
        accuracyVoteRepository.save(vote);

        FestivalAccuracyVoteResponse.Info response = buildResponse(festival, vote.getVoteType());
        memberTrustGradeService.refreshForFestival(festival);
        return response;
    }

    @Transactional
    public FestivalAccuracyVoteResponse.Info cancelVote(
            UUID memberId,
            Long festivalId
    ) {
        Member member = memberReader.getActiveMember(memberId);
        Festival festival = findUserSubmittedFestival(festivalId);

        accuracyVoteRepository
                .findByFestivalAndMember(festival, member)
                .ifPresent(accuracyVoteRepository::delete);

        FestivalAccuracyVoteResponse.Info response = buildResponse(festival, null);
        memberTrustGradeService.refreshForFestival(festival);
        return response;
    }

    private FestivalAccuracyVoteResponse.Info buildResponse(
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

        return FestivalAccuracyVoteResponse.Info.from(
                accurateCount,
                inaccurateCount,
                myVote
        );
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
