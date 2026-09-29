package com.team007.room_escape.domain.post.service;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.like.infra.dto.PostLikeCount;
import com.team007.room_escape.domain.like.infra.repository.LikeRepository;
import com.team007.room_escape.domain.like.type.LikeSort;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.service.MemberReader;
import com.team007.room_escape.domain.post.infra.dto.PostRequest;
import com.team007.room_escape.domain.post.infra.dto.PostResponse;
import com.team007.room_escape.domain.post.infra.entity.Post;
import com.team007.room_escape.domain.post.type.PostSearchType;
import com.team007.room_escape.domain.post.infra.repository.PostRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.CommonExceptionCode;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import com.team007.room_escape.global.response.code.PostExceptionCode;
import com.team007.room_escape.global.util.RichTextSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

	private final PostRepository postRepository;
	private final FestivalRepository festivalRepository;
	private final MemberReader memberReader;
	private final RichTextSanitizer richTextSanitizer;
	private final LikeRepository likeRepository;


	@Transactional
	public PostResponse.CreateResponse createPost(
			Long festivalId,
			PostRequest request,
			UUID memberId
	) {
		Member member = memberReader.getUnrestrictedMember(memberId);

		Festival festival = festivalRepository.findById(festivalId)
				.orElseThrow(() -> new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_FOUND));
		String sanitizedContent = sanitizeRequiredContent(request.content());

		Post post = Post.builder()
				.member(member)
				.festival(festival)
				.title(request.title())
				.content(sanitizedContent)
				.thumbnail(request.thumbnail())
				.build();

		postRepository.save(post);

		return PostResponse.CreateResponse.from(post);
	}

	@Transactional(readOnly = true)
	public Page<PostResponse.ListResponse> searchPosts(
			PostSearchType type,
			String keyword,
			Pageable page
	) {
		boolean hasKeyword = type != null && keyword != null && !keyword.isBlank();

		if(LikeSort.isRequested(page)) {
			return toListResponses(postRepository.findAllOrderByLikeCount(
					false, null,
					hasKeyword, hasKeyword ? type.name() : null, hasKeyword ? keyword : null,
					LikeSort.withoutSort(page)
			));
		}
		if(!hasKeyword) {
			return toListResponses(postRepository.findAllNotDeleted(page));
		}
		return toListResponses(postRepository.searchPosts(type, keyword, page));
	}

	@Transactional(readOnly = true)
	public Page<PostResponse.AdminListResponse> searchPostsForAdmin(
			PostSearchType type,
			String keyword,
			Pageable page
	) {
		if(type == null || keyword == null || keyword.isBlank()) {
			return postRepository.findAllIncludingDeleted(page)
					.map(PostResponse.AdminListResponse::from);
		}

		return postRepository.searchPostsIncludingDeleted(type, keyword, page)
					.map(PostResponse.AdminListResponse::from);
	}

	@Transactional(readOnly = true)
	public Page<PostResponse.ListResponse> getPostsByFestival(Long festivalId, Pageable page) {
		if(LikeSort.isRequested(page)) {
			return toListResponses(postRepository.findAllOrderByLikeCount(
					true, festivalId,
					false, null, null,
					LikeSort.withoutSort(page)
			));
		}
		return toListResponses(postRepository.findAllByFestivalId(festivalId, page));
	}

	/** 페이지에 담긴 후기들의 좋아요 수를 한 번에 세서 응답에 붙인다. (후기마다 세면 N+1) */
	private Page<PostResponse.ListResponse> toListResponses(Page<Post> posts) {
		if(posts.isEmpty()) {
			return posts.map(post -> PostResponse.ListResponse.from(post, 0L));
		}

		List<UUID> postIds = posts.getContent().stream().map(Post::getId).toList();
		Map<UUID, Long> likeCounts = likeRepository.countByPostIds(postIds).stream()
				.collect(Collectors.toMap(PostLikeCount::postId, PostLikeCount::likeCount));

		return posts.map(post ->
				PostResponse.ListResponse.from(post, likeCounts.getOrDefault(post.getId(), 0L)));
	}

	@Transactional(readOnly = true)
	public PostResponse.DetailResponse getPostDetail(UUID id) {

		Post post = postRepository.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND));

		return PostResponse.DetailResponse.from(post);
	}

	@Transactional
	public PostResponse.DetailResponse updatePost(
			UUID postId,
			PostRequest request,
			UUID memberId
	) {
		memberReader.getUnrestrictedMember(memberId);

		Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
				.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND));

		if(!post.getMember().getId().equals(memberId)) {
			throw new BusinessException(PostExceptionCode.POST_FORBIDDEN);

		}
		String sanitizedContent = sanitizeRequiredContent(request.content());
		post.update(
			request.title(),
			sanitizedContent,
			request.thumbnail()
		);

		return PostResponse.DetailResponse.from(post);
	}

	@Transactional
	public void deletePost(UUID postId, UUID memberId, boolean isAdmin) {
		Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
				.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND));

		boolean isAuthor = post.getMember().getId().equals(memberId);

		if(!isAuthor && !isAdmin) {
			throw new BusinessException(PostExceptionCode.POST_FORBIDDEN);
		}

		post.delete();
	}

	private String sanitizeRequiredContent(String content) {
		String sanitizedContent = richTextSanitizer.sanitize(content);
		if (!richTextSanitizer.hasVisibleText(sanitizedContent)) {
			throw new BusinessException(CommonExceptionCode.INVALID_INPUT);
		}
		return sanitizedContent;
	}
}
