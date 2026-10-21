package com.team007.room_escape.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalAccuracyVoteRepository;
import com.team007.room_escape.domain.like.infra.repository.LikeRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberTrustGradeServiceTest {

	@Mock
	private LikeRepository likeRepository;

	@Mock
	private FestivalAccuracyVoteRepository accuracyVoteRepository;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private EntityManager entityManager;

	@InjectMocks
	private MemberTrustGradeService memberTrustGradeService;

	@Test
	@DisplayName("좋아요 또는 정확해요 중 하나가 10개 이상이면 탈출 메이커 등급이 된다")
	void becomeRecognizedWhenEitherConditionIsMet() {
		Member author = author(MemberRole.ROLE_UNVERIFIED);
		Festival festival = festivalBy(author);
		when(likeRepository.countReceivedFestivalLikesByAuthorId(author.getId()))
			.thenReturn(10L);
		when(accuracyVoteRepository.countReceivedAccurateVotesByAuthorId(author.getId()))
			.thenReturn(9L);

		memberTrustGradeService.refreshForFestival(festival);

		verify(memberRepository).updateTrustGrade(eq(author.getId()), eq(MemberRole.ROLE_RECOGNIZED), any(LocalDateTime.class));
	}

	@Test
	@DisplayName("좋아요와 정확해요가 모두 10개 이상이면 신뢰 제보자가 된다")
	void becomeTrustedWhenBothConditionsAreMet() {
		Member author = author(MemberRole.ROLE_UNVERIFIED);
		Festival festival = festivalBy(author);
		when(likeRepository.countReceivedFestivalLikesByAuthorId(author.getId()))
			.thenReturn(10L);
		when(accuracyVoteRepository.countReceivedAccurateVotesByAuthorId(author.getId()))
			.thenReturn(10L);

		memberTrustGradeService.refreshForFestival(festival);

		verify(memberRepository).updateTrustGrade(eq(author.getId()), eq(MemberRole.ROLE_TRUSTED), any(LocalDateTime.class));
	}

	@Test
	@DisplayName("제재 회원과 관리자는 자동 등급 계산으로 변경하지 않는다")
	void preserveWarningAndAdmin() {
		Member warning = author(MemberRole.ROLE_WARNING);
		Member admin = author(MemberRole.ROLE_ADMIN);
		memberTrustGradeService.refreshForFestival(festivalBy(warning));
		memberTrustGradeService.refreshForFestival(festivalBy(admin));

		assertThat(warning.getRole()).isEqualTo(MemberRole.ROLE_WARNING);
		assertThat(admin.getRole()).isEqualTo(MemberRole.ROLE_ADMIN);
		verifyNoInteractions(likeRepository, accuracyVoteRepository, memberRepository, entityManager);
	}

	private Member author(MemberRole role) {
		return Member.builder()
			.id(UUID.randomUUID())
			.role(role)
			.build();
	}

	private Festival festivalBy(Member author) {
		return Festival.builder()
			.member(author)
			.providerType(ProviderType.MEMBER)
			.build();
	}
}
