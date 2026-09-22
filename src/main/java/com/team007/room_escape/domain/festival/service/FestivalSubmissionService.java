package com.team007.room_escape.domain.festival.service;

import com.team007.room_escape.domain.festival.dto.FestivalSubmissionRequest.CreateOrUpdateFestivalSubmissionRequest;
import com.team007.room_escape.domain.festival.dto.FestivalSubmissionResponse.CreateFestivalSubmissionResponse;
import com.team007.room_escape.domain.festival.dto.FestivalSubmissionResponse.FindAllFestivalSubmissionResponse;
import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalSubmission;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalSubmissionRepository;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import com.team007.room_escape.domain.festival.dto.FestivalSubmissionResponse.FindFestivalSubmissionResponse;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
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
    public CreateFestivalSubmissionResponse create(UUID memberId, CreateOrUpdateFestivalSubmissionRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));

        Festival festival = Festival.builder()
                .member(member)
                .providerType(ProviderType.MEMBER)
                .instNm(request.instNm())
                .title(request.title())
                .category(request.category())
                .content(request.festivalContent())
                .url(request.referenceUrl())
                .region(request.region())
                .regionDetail(request.regionDetail())
                .imgUrl(request.imgUrl())
                .beginDe(request.beginDe())
                .endDe(request.endDe())
                .eventTmInfo(request.eventTmInfo())
                .partcptExpnInfo(request.partcptExpnInfo())
                .telnoInfo(request.telnoInfo())
                .hostInstNm(request.hostInstNm())
                .writngDe(LocalDateTime.now())
                .status(FestivalStatus.from(request.endDe()))
                .build();

        Festival savedFestival = festivalRepository.save(festival);

        FestivalSubmission festivalSubmission = FestivalSubmission.builder()
                .festival(savedFestival)
                .build();

        FestivalSubmission savedFestivalSubmission =
                festivalSubmissionRepository.save(festivalSubmission);

        return CreateFestivalSubmissionResponse.from(savedFestival, savedFestivalSubmission);
    }

    @Transactional(readOnly = true)
    public List<FindAllFestivalSubmissionResponse> findAllByMemberId(UUID memberId) {
        return festivalSubmissionRepository
                .findAllByFestival_Member_IdAndFestival_ProviderTypeAndDeletedAtIsNullOrderByCreatedAtDesc
                        (memberId, ProviderType.MEMBER)
                .stream()
                .map(FindAllFestivalSubmissionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FindFestivalSubmissionResponse findById(
            UUID memberId,
            UUID submissionId
    ) {
        FestivalSubmission festivalSubmission =
                festivalSubmissionRepository
                        .findByIdAndFestival_Member_IdAndDeletedAtIsNull(submissionId, memberId)
                        .orElseThrow(() -> new BusinessException(
                                FestivalExceptionCode.FESTIVAL_SUBMISSION_NOT_FOUND
                        ));

        return FindFestivalSubmissionResponse.from(festivalSubmission);
    }

    @Transactional
    public FindFestivalSubmissionResponse update(
            UUID memberId,
            UUID submissionId,
            CreateOrUpdateFestivalSubmissionRequest request
    ) {
        FestivalSubmission submission = festivalSubmissionRepository
                .findByIdAndFestival_Member_IdAndDeletedAtIsNull(submissionId, memberId)
                .orElseThrow(() -> new BusinessException(
                        FestivalExceptionCode.FESTIVAL_SUBMISSION_NOT_FOUND
                ));

        submission.getFestival().updateDetails(
                request.instNm(),
                request.title(),
                request.category(),
                request.festivalContent(),
                request.referenceUrl(),
                request.region(),
                request.regionDetail(),
                request.imgUrl(),
                request.beginDe(),
                request.endDe(),
                request.eventTmInfo(),
                request.partcptExpnInfo(),
                request.telnoInfo(),
                request.hostInstNm()
        );
        return FindFestivalSubmissionResponse.from(submission);
    }

    @Transactional
    public void delete(UUID memberId, UUID submissionId) {
        FestivalSubmission submission = festivalSubmissionRepository
                .findByIdAndFestival_Member_IdAndDeletedAtIsNull(submissionId, memberId)
                .orElseThrow(() -> new BusinessException(
                        FestivalExceptionCode.FESTIVAL_SUBMISSION_NOT_FOUND
                ));

        submission.delete();
    }
}
