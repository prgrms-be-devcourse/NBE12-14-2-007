package com.team007.room_escape.domain.festival.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team007.room_escape.domain.festival.dto.FestivalAccuracyVoteRequest;
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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalAccuracyVoteServiceTest {

	@Mock FestivalAccuracyVoteRepository accuracyVoteRepository;
	@Mock FestivalRepository festivalRepository;
	@Mock MemberReader memberReader;
	@Mock MemberTrustGradeService memberTrustGradeService;

	@InjectMocks FestivalAccuracyVoteService accuracyVoteService;

	@Test
	@DisplayName("본인이 제보한 행사의 정확도는 평가할 수 없다")
	void rejectSelfAccuracyVote() {
		UUID memberId = UUID.randomUUID();
		Member member = Member.builder().id(memberId).build();
		Festival festival = Festival.builder()
			.id(1L)
			.member(member)
			.providerType(ProviderType.MEMBER)
			.build();
		FestivalAccuracyVoteRequest.Upsert request =
			new FestivalAccuracyVoteRequest.Upsert(FestivalAccuracyVoteType.ACCURATE);

		when(memberReader.getActiveMember(memberId)).thenReturn(member);
		when(festivalRepository.findByIdAndProviderTypeAndDeletedAtIsNull(
			1L, ProviderType.MEMBER
		)).thenReturn(Optional.of(festival));

		assertThatThrownBy(() -> accuracyVoteService.vote(memberId, 1L, request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				org.assertj.core.api.Assertions.assertThat(exception.getExceptionCode())
					.isEqualTo(FestivalExceptionCode.SELF_ACCURACY_VOTE_NOT_ALLOWED)
			);

		verify(accuracyVoteRepository, never())
			.save(org.mockito.ArgumentMatchers.any(FestivalAccuracyVote.class));
	}
}
