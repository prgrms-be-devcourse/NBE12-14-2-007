package com.team007.room_escape.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.like.infra.dto.PostLikeCount;
import com.team007.room_escape.domain.like.infra.repository.LikeRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.domain.post.infra.dto.PostResponse;
import com.team007.room_escape.domain.post.infra.entity.Post;
import com.team007.room_escape.domain.post.infra.repository.PostRepository;
import com.team007.room_escape.domain.post.type.PostSearchType;
import com.team007.room_escape.global.storage.ImageUrlResolver;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 후기 목록의 정렬 분기와 좋아요 수 조립을 검증한다.
 * 실제 정렬 결과는 LikeSortRepositoryTest 에서 DB로 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

	private static final Pageable LIKE_SORT = PageRequest.of(2, 6, Sort.by(Sort.Direction.DESC, "likeCount"));
	private static final Pageable UNSORTED = PageRequest.of(2, 6);

	@Mock
	PostRepository postRepository;

	@Mock
	FestivalRepository festivalRepository;

	@Mock
	MemberRepository memberRepository;

	@Mock
	LikeRepository likeRepository;

	@Mock
	ImageUrlResolver imageUrlResolver;

	@InjectMocks
	PostService postService;

	@Test
	@DisplayName("좋아요순 검색이면 정렬을 뗀 페이지로 전용 쿼리를 쓰고 검색 조건을 넘긴다")
	void searchUsesLikeSortQuery() {
		Post post = post();
		when(postRepository.findAllOrderByLikeCount(false, null, true, "MEMBER_NICKNAME", "닉", UNSORTED))
				.thenReturn(new PageImpl<>(List.of(post), UNSORTED, 13));
		when(likeRepository.countByPostIds(List.of(post.getId())))
				.thenReturn(List.of(new PostLikeCount(post.getId(), 4L)));

		Page<PostResponse.ListResponse> result =
				postService.searchPosts(PostSearchType.MEMBER_NICKNAME, "닉", LIKE_SORT);

		assertThat(result.getContent()).extracting(PostResponse.ListResponse::likeCount).containsExactly(4L);
		assertThat(result.getTotalElements()).isEqualTo(13);
		verify(postRepository, never()).searchPosts(any(), any(), any());
		verify(postRepository, never()).findAllNotDeleted(any());
	}

	@Test
	@DisplayName("검색어가 비어 있으면 좋아요순 전체 목록으로 조회한다")
	void blankKeywordFallsBackToAllPosts() {
		when(postRepository.findAllOrderByLikeCount(false, null, false, null, null, UNSORTED))
				.thenReturn(Page.empty(UNSORTED));

		Page<PostResponse.ListResponse> result =
				postService.searchPosts(PostSearchType.TITLE, "  ", LIKE_SORT);

		assertThat(result.getContent()).isEmpty();
		// 빈 페이지면 좋아요를 셀 필요가 없다
		verifyNoInteractions(likeRepository);
	}

	@Test
	@DisplayName("행사별 좋아요순이면 행사 조건으로 전용 쿼리를 쓴다")
	void festivalPostsUseLikeSortQuery() {
		Post post = post();
		when(postRepository.findAllOrderByLikeCount(true, 3L, false, null, null, UNSORTED))
				.thenReturn(new PageImpl<>(List.of(post), UNSORTED, 1));

		Page<PostResponse.ListResponse> result = postService.getPostsByFestival(3L, LIKE_SORT);

		// 좋아요가 하나도 없으면 집계 결과에 없으므로 0으로 채운다
		assertThat(result.getContent()).extracting(PostResponse.ListResponse::likeCount).containsExactly(0L);
		verify(postRepository, never()).findAllByFestivalId(anyLong(), any());
	}

	@Test
	@DisplayName("최신순은 기존 쿼리를 그대로 쓰고, 좋아요 수만 붙인다")
	void latestSortKeepsExistingQuery() {
		Pageable latest = PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "createdAt"));
		Post first = post();
		Post second = post();
		when(postRepository.findAllNotDeleted(latest))
				.thenReturn(new PageImpl<>(List.of(first, second), latest, 2));
		when(likeRepository.countByPostIds(List.of(first.getId(), second.getId())))
				.thenReturn(List.of(new PostLikeCount(second.getId(), 2L)));

		Page<PostResponse.ListResponse> result = postService.searchPosts(null, null, latest);

		assertThat(result.getContent())
				.extracting(PostResponse.ListResponse::id, PostResponse.ListResponse::likeCount)
				.containsExactly(tuple(first.getId(), 0L), tuple(second.getId(), 2L));
		verify(postRepository, never()).findAllOrderByLikeCount(
				anyBoolean(), any(), anyBoolean(), any(), any(), any());
	}

	private Post post() {
		Member member = Member.builder()
				.id(UUID.randomUUID())
				.email("a@test.local")
				.password("x")
				.role(MemberRole.ROLE_UNVERIFIED)
				.nickname("닉네임")
				.build();
		Festival festival = Festival.builder().id(3L).providerType(ProviderType.PUBLIC).title("행사").build();
		return Post.builder()
				.id(UUID.randomUUID())
				.member(member)
				.festival(festival)
				.title("후기")
				.content("내용")
				.build();
	}
}
