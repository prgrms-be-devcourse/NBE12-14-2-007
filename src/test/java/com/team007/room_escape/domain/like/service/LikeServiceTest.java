package com.team007.room_escape.domain.like.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.like.dto.LikeResponse;
import com.team007.room_escape.domain.like.infra.entity.Like;
import com.team007.room_escape.domain.like.infra.repository.LikeRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.service.MemberReader;
import com.team007.room_escape.domain.member.service.MemberTrustGradeService;
import com.team007.room_escape.domain.post.infra.entity.Post;
import com.team007.room_escape.domain.post.infra.repository.PostRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.LikeExceptionCode;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

	@Mock LikeRepository likeRepository;
	@Mock PostRepository postRepository;
	@Mock FestivalRepository festivalRepository;
	@Mock MemberReader memberReader;
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
		when(memberReader.getUnrestrictedMember(memberId)).thenReturn(member);

		assertThatThrownBy(() -> likeService.createFestivalLike(1L, memberId))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				org.assertj.core.api.Assertions.assertThat(exception.getExceptionCode())
					.isEqualTo(LikeExceptionCode.SELF_FESTIVAL_LIKE_NOT_ALLOWED)
			);

		verify(likeRepository, never()).save(org.mockito.ArgumentMatchers.any(Like.class));
	}

	@Test
	@DisplayName("후기 좋아요를 등록하면 후기의 좋아요 수를 올리고 올라간 값을 돌려준다")
	void createPostLikeIncreasesLikeCount() {
		UUID memberId = UUID.randomUUID();
		UUID postId = UUID.randomUUID();
		Member member = Member.builder().id(memberId).build();
		Post post = Post.builder().id(postId).member(member).build();

		when(memberReader.getUnrestrictedMember(memberId)).thenReturn(member);
		when(postRepository.findByIdAndDeletedAtIsNull(postId)).thenReturn(Optional.of(post));
		when(likeRepository.existsByPostIdAndMemberId(postId, memberId)).thenReturn(false);
		when(postRepository.findLikeCountById(postId)).thenReturn(Optional.of(3L));

		LikeResponse response = likeService.createPostLike(postId, memberId);

		assertThat(response.likeCount()).isEqualTo(3L);
		InOrder inOrder = inOrder(likeRepository, postRepository);
		inOrder.verify(likeRepository).save(any(Like.class));
		inOrder.verify(postRepository).increaseLikeCount(postId);
	}

	@Test
	@DisplayName("이미 좋아요한 후기면 좋아요 수를 바꾸지 않는다")
	void duplicatePostLikeDoesNotChangeLikeCount() {
		UUID memberId = UUID.randomUUID();
		UUID postId = UUID.randomUUID();
		Member member = Member.builder().id(memberId).build();

		when(memberReader.getUnrestrictedMember(memberId)).thenReturn(member);
		when(postRepository.findByIdAndDeletedAtIsNull(postId))
			.thenReturn(Optional.of(Post.builder().id(postId).member(member).build()));
		when(likeRepository.existsByPostIdAndMemberId(postId, memberId)).thenReturn(true);

		assertThatThrownBy(() -> likeService.createPostLike(postId, memberId))
			.isInstanceOf(BusinessException.class);

		verify(postRepository, never()).increaseLikeCount(any());
	}

	@Test
	@DisplayName("후기 좋아요를 취소하면 후기의 좋아요 수를 내리고 내려간 값을 돌려준다")
	void deletePostLikeDecreasesLikeCount() {
		UUID memberId = UUID.randomUUID();
		UUID postId = UUID.randomUUID();
		Like like = Like.builder().id(1L).build();

		when(postRepository.existsById(postId)).thenReturn(true);
		when(likeRepository.findByPostIdAndMemberId(postId, memberId)).thenReturn(Optional.of(like));
		when(postRepository.findLikeCountById(postId)).thenReturn(Optional.of(2L));

		LikeResponse response = likeService.deletePostLike(postId, memberId);

		assertThat(response.likeCount()).isEqualTo(2L);
		InOrder inOrder = inOrder(likeRepository, postRepository);
		inOrder.verify(likeRepository).delete(like);
		inOrder.verify(postRepository).decreaseLikeCount(postId);
	}
}
