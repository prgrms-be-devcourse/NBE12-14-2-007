package com.team007.room_escape.domain.festival.service;

import com.team007.room_escape.domain.festival.dto.FestivalSubmissionRequest.CreateOrUpdateFestivalSubmissionRequest;
import com.team007.room_escape.domain.festival.dto.FestivalSubmissionResponse.CreateFestivalSubmissionResponse;
import com.team007.room_escape.domain.festival.dto.FestivalSubmissionResponse.FindAllFestivalSubmissionResponse;
import com.team007.room_escape.domain.festival.dto.FestivalSubmissionResponse.FindFestivalSubmissionResponse;
import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.infra.entity.FestivalSubmission;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalSubmissionRepository;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.service.MemberReader;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import com.team007.room_escape.global.util.RichTextSanitizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FestivalSubmissionService {

    private static final String DUPLICATE_CONSTRAINT_NAME =
            "uk_member_festival_schedule_region_url";

    private final FestivalRepository festivalRepository;
    private final FestivalSubmissionRepository festivalSubmissionRepository;
    private final MemberReader memberReader;
    private final RichTextSanitizer richTextSanitizer;

    @Transactional
    public CreateFestivalSubmissionResponse create(UUID memberId, CreateOrUpdateFestivalSubmissionRequest request) {
        Member member = memberReader.getUnrestrictedMember(memberId);

        String normalizedReferenceUrl = normalizeReferenceUrl(request.referenceUrl());

        validateNotDuplicate(
                null,
                request.beginDe(),
                request.endDe(),
                request.region(),
                normalizedReferenceUrl
        );

        Festival festival = Festival.builder()
                .member(member)
                .providerType(ProviderType.MEMBER)
                .instNm(request.instNm())
                .title(request.title())
                .category(request.category())
                .content(richTextSanitizer.sanitize(request.festivalContent()))
                .url(normalizedReferenceUrl)
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

        Festival savedFestival = saveFestival(festival);

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
        memberReader.getUnrestrictedMember(memberId);

        FestivalSubmission submission = festivalSubmissionRepository
                .findByIdAndFestival_Member_IdAndDeletedAtIsNull(submissionId, memberId)
                .orElseThrow(() -> new BusinessException(
                        FestivalExceptionCode.FESTIVAL_SUBMISSION_NOT_FOUND
                ));

        Festival festival = submission.getFestival();
        String normalizedReferenceUrl = normalizeReferenceUrl(request.referenceUrl());

        validateNotDuplicate(
                festival.getId(),
                request.beginDe(),
                request.endDe(),
                request.region(),
                normalizedReferenceUrl
        );

        festival.updateDetails(
                request.instNm(),
                request.title(),
                request.category(),
                richTextSanitizer.sanitize(request.festivalContent()),
                normalizedReferenceUrl,
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
        flushFestivalChanges();

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
        submission.getFestival().delete();
    }

    private void validateNotDuplicate(
            Long currentFestivalId,
            LocalDateTime beginDe,
            LocalDateTime endDe,
            FestivalRegion region,
            String referenceUrl
    ) {
        // 날짜 기준으로 비교하고, 같은 조건의 동시 요청은 잠금으로 순서대로 처리한다.
        long lockKey = java.util.Objects.hash(beginDe.toLocalDate(), endDe.toLocalDate(), region.name(), referenceUrl);
        festivalRepository.lockSubmissionDuplicateKey(lockKey);
        boolean duplicated = festivalRepository.existsSubmissionDuplicate(
                currentFestivalId,
                beginDe.toLocalDate().atStartOfDay(),
                beginDe.toLocalDate().plusDays(1).atStartOfDay(),
                endDe.toLocalDate().atStartOfDay(),
                endDe.toLocalDate().plusDays(1).atStartOfDay(),
                region.name(), referenceUrl);

        if (duplicated) {
            throw new BusinessException(FestivalExceptionCode.DUPLICATE_FESTIVAL);
        }
    }

    private String normalizeReferenceUrl(String referenceUrl) {
        return referenceUrl
                .trim()
                .replaceFirst("/+$", "");
    }

    private Festival saveFestival(Festival festival) {
        try {
            return festivalRepository.saveAndFlush(festival);
        } catch (DataIntegrityViolationException exception) {
            throw translateDuplicateConstraint(exception);
        }
    }

    private void flushFestivalChanges() {
        try {
            festivalRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw translateDuplicateConstraint(exception);
        }
    }

    private RuntimeException translateDuplicateConstraint(DataIntegrityViolationException exception) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintException
                    && DUPLICATE_CONSTRAINT_NAME.equals(constraintException.getConstraintName())) {
                return new BusinessException(FestivalExceptionCode.DUPLICATE_FESTIVAL);
            }
            cause = cause.getCause();
        }

        return exception;
    }
}
