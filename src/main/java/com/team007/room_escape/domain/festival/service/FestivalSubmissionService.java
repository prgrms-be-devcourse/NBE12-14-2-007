package com.team007.room_escape.domain.festival.service;

import com.team007.room_escape.domain.festival.dto.FestivalSubmissionRequest.CreateFestivalSubmissionRequest;
import com.team007.room_escape.domain.festival.dto.FestivalSubmissionResponse.CreateFestivalSubmissionResponse;
import com.team007.room_escape.domain.festival.dto.FestivalSubmissionResponse.FindAllFestivalSubmissionResponse;
import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalApplyStatus;
import com.team007.room_escape.domain.festival.infra.entity.FestivalSubmission;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalSubmissionRepository;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FestivalSubmissionService {

    private final FestivalRepository festivalRepository;
    private final FestivalSubmissionRepository festivalSubmissionRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public CreateFestivalSubmissionResponse create(UUID memberId, CreateFestivalSubmissionRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));

        Festival festival = Festival.builder()
                .member(member)
                .providerType(ProviderType.MEMBER)
                .instNm(request.instNm())
                .title(request.title())
                .manager(request.manager())
                .content(request.festivalContent())
                .url(request.url())
                .imgUrl(request.imgUrl())
                .beginDe(request.beginDe())
                .endDe(request.endDe())
                .eventTmInfo(request.eventTmInfo())
                .partcptExpnInfo(request.partcptExpnInfo())
                .telnoInfo(request.telnoInfo())
                .hostInstNm(request.hostInstNm())
                .hmpgUrl(request.hmpgUrl())
                .writngDe(LocalDateTime.now())
                .applyStatus(FestivalApplyStatus.PENDING)
                .build();

        Festival savedFestival = festivalRepository.save(festival);

        FestivalSubmission festivalSubmission = FestivalSubmission.builder()
                .festival(savedFestival)
                .category(request.category())
                .content(request.submissionContent())
                .build();

        FestivalSubmission savedFestivalSubmission =
                festivalSubmissionRepository.save(festivalSubmission);

        return CreateFestivalSubmissionResponse.from(
                savedFestival,
                savedFestivalSubmission
        );
    }

    @Transactional(readOnly = true)
    public List<FindAllFestivalSubmissionResponse> findAllByMemberId(UUID memberId) {
        return festivalSubmissionRepository
                .findAllByFestival_Member_IdAndFestival_ProviderTypeOrderByFestival_WritngDeDesc
                        (memberId, ProviderType.MEMBER)
                .stream()
                .map(FindAllFestivalSubmissionResponse::from)
                .toList();
    }
}
