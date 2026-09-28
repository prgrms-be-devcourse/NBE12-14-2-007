package com.team007.room_escape.domain.like.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.like.infra.entity.Like;
import com.team007.room_escape.domain.like.infra.repository.LikeRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.domain.member.service.MemberTrustGradeService;
import com.team007.room_escape.domain.post.infra.repository.PostRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.LikeExceptionCode;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

	@Mock LikeRepository likeRepository;
	@Mock PostRepository postRepository;
	@Mock FestivalRepository festivalRepository;
	@Mock MemberRepository memberRepository;
	@Mock MemberTrustGradeService memberTrustGradeService;

	@InjectMocks LikeService likeService;

	@Test
	@DisplayName("본인이 제보한 행사에는 좋아요를 등록할 수 없다")
	void rejectSelfFestivalLike() {
		UUID memberId = UUID.randomUUID();
		Member member = Member.builder().id(memberId).build();
		Festival festival = Festival.builder()
			.id(1L)
			.member(member)
			.providerType(ProviderType.MEMBER)
			.build();

		when(festivalRepository.findById(1L)).thenReturn(Optional.of(festival));
		when(memberRepository.findByIdAndDeletedAtIsNull(memberId)).thenReturn(Optional.of(member));

		assertThatThrownBy(() -> likeService.createFestivalLike(1L, memberId))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				org.assertj.core.api.Assertions.assertThat(exception.getExceptionCode())
					.isEqualTo(LikeExceptionCode.SELF_FESTIVAL_LIKE_NOT_ALLOWED)
			);

		verify(likeRepository, never()).save(org.mockito.ArgumentMatchers.any(Like.class));
	}
}
