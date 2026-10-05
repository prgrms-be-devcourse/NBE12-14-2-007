package com.team007.room_escape.domain.festival.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.team007.room_escape.domain.festival.dto.FestivalSubmissionRequest;
import com.team007.room_escape.domain.festival.infra.entity.*;
import com.team007.room_escape.domain.festival.infra.repository.*;
import com.team007.room_escape.domain.member.service.MemberReader;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import com.team007.room_escape.global.util.RichTextSanitizer;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FestivalSubmissionServiceTest {
    private final FestivalRepository festivals = mock(FestivalRepository.class);
    private final FestivalSubmissionRepository submissions = mock(FestivalSubmissionRepository.class);
    private final MemberReader members = mock(MemberReader.class);
    private final FestivalSubmissionService service = new FestivalSubmissionService(
            festivals, submissions, members, mock(RichTextSanitizer.class));
    private final UUID memberId = UUID.randomUUID();
    private final UUID submissionId = UUID.randomUUID();
    private final LocalDateTime begin = LocalDateTime.of(2026, 10, 2, 10, 30);
    private final LocalDateTime end = LocalDateTime.of(2026, 10, 3, 18, 0);

    private FestivalSubmissionRequest.Upsert request() {
        return FestivalSubmissionRequest.Upsert.builder()
                .title("행사").category("축제").region(FestivalRegion.GYEONGGI)
                .referenceUrl(" https://example.com/event/// ")
                .beginDe(begin).endDe(end).build();
    }

    @Test
    void duplicateCreateUsesDatesAndNormalizedUrlAndRejectsBeforeSaving() {
        when(festivals.existsSubmissionDuplicate(isNull(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);
        assertDuplicate(() -> service.create(memberId, request()));
        verify(festivals).existsSubmissionDuplicate(null,
                begin.toLocalDate().atStartOfDay(), begin.toLocalDate().plusDays(1).atStartOfDay(),
                end.toLocalDate().atStartOfDay(), end.toLocalDate().plusDays(1).atStartOfDay(),
                FestivalRegion.GYEONGGI.name(), "https://example.com/event");
        var order = inOrder(festivals);
        order.verify(festivals).lockSubmissionDuplicateKey(anyLong());
        order.verify(festivals).existsSubmissionDuplicate(isNull(), any(), any(), any(), any(), any(), any());
        verify(festivals, never()).saveAndFlush(any());
        verify(submissions, never()).save(any());
    }

    @Test
    void duplicateUpdateExcludesCurrentEvent() {
        Festival festival = Festival.builder().id(7L).build();
        when(submissions.findByIdAndFestival_Member_IdAndDeletedAtIsNull(submissionId, memberId))
                .thenReturn(Optional.of(FestivalSubmission.builder().festival(festival).build()));
        when(festivals.existsSubmissionDuplicate(eq(7L), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);
        assertDuplicate(() -> service.update(memberId, submissionId, request()));
        assertThat(festival.getTitle()).isNull();
    }

    @Test
    void deleteMarksBothSubmissionAndPubliclyListedEventDeleted() {
        Festival festival = Festival.builder().id(7L).providerType(ProviderType.MEMBER).build();
        FestivalSubmission submission = FestivalSubmission.builder().festival(festival).build();
        when(submissions.findByIdAndFestival_Member_IdAndDeletedAtIsNull(submissionId, memberId))
                .thenReturn(Optional.of(submission));
        service.delete(memberId, submissionId);
        assertThat(submission.isDeleted()).isTrue();
        assertThat(festival.isDeleted()).isTrue();
    }

    @Test
    void nonDuplicateCreateSavesNormalizedUrl() {
        when(festivals.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(submissions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.create(memberId, request());
        var captor = org.mockito.ArgumentCaptor.forClass(Festival.class);
        verify(festivals).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUrl()).isEqualTo("https://example.com/event");
        verify(submissions).save(any());
    }

    @Test
    void nonDuplicateUpdateStillAllowsEditingOwnEvent() {
        Festival festival = Festival.builder().id(7L).build();
        when(submissions.findByIdAndFestival_Member_IdAndDeletedAtIsNull(submissionId, memberId))
                .thenReturn(Optional.of(FestivalSubmission.builder().festival(festival).build()));
        service.update(memberId, submissionId, request());
        assertThat(festival.getTitle()).isEqualTo("행사");
        assertThat(festival.getUrl()).isEqualTo("https://example.com/event");
        verify(festivals).flush();
    }

    @Test
    void cannotDeleteAnotherMembersSubmission() {
        when(submissions.findByIdAndFestival_Member_IdAndDeletedAtIsNull(submissionId, memberId))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(memberId, submissionId))
                .isInstanceOf(BusinessException.class);
    }

    private void assertDuplicate(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getExceptionCode())
                .isEqualTo(FestivalExceptionCode.DUPLICATE_FESTIVAL);
    }
}
