package com.team007.room_escape.domain.post.service;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.like.infra.repository.LikeRepository;
import com.team007.room_escape.domain.like.type.LikeSort;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.service.MemberReader;
import com.team007.room_escape.domain.post.dto.PostRequest;
import com.team007.room_escape.domain.post.dto.PostResponse;
import com.team007.room_escape.domain.post.infra.entity.Post;
import com.team007.room_escape.domain.post.type.PostSearchType;
import com.team007.room_escape.domain.post.infra.repository.PostRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.CommonExceptionCode;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import com.team007.room_escape.global.response.code.PostExceptionCode;
import com.team007.room_escape.global.storage.ImageUrlResolver;
import com.team007.room_escape.global.storage.R2StorageService;
import com.team007.room_escape.global.util.RichTextSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostService {

	private final PostRepository postRepository;
	private final FestivalRepository festivalRepository;
	private final MemberReader memberReader;
	private final RichTextSanitizer richTextSanitizer;
	private final LikeRepository likeRepository;
	private final ImageUrlResolver imageUrlResolver;
	private final R2StorageService r2StorageService;


	@Transactional
	public PostResponse.CreateResponse createPost(
			Long festivalId,
			PostRequest request,
			UUID memberId
	) {
		Member member = memberReader.getUnrestrictedMember(memberId);

		Festival festival = festivalRepository.findByIdAndDeletedAtIsNull(festivalId)
				.orElseThrow(() -> new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_FOUND));
		String sanitizedContent = sanitizeRequiredContent(request.content());
		String thumbnail = imageUrlResolver.toKey(request.thumbnail());
		r2StorageService.requireOwnedBy(thumbnail, null, memberId);

		Post post = Post.builder()
				.member(member)
				.festival(festival)
				.title(request.title())
				.content(sanitizedContent)
				.thumbnail(thumbnail)
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
			Pageable unsorted = LikeSort.withoutSort(page);
			return toListResponses(hasKeyword
					? postRepository.searchPostsOrderByLikeCount(type.name(), keyword, unsorted)
					: postRepository.findAllOrderByLikeCount(unsorted));
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
					.map(post -> PostResponse.AdminListResponse.from(post, imageUrlResolver));
		}

		return postRepository.searchPostsIncludingDeleted(type, keyword, page)
					.map(post -> PostResponse.AdminListResponse.from(post, imageUrlResolver));
	}

	@Transactional(readOnly = true)
	public Page<PostResponse.ListResponse> getPostsByFestival(Long festivalId, Pageable page) {
		if(LikeSort.isRequested(page)) {
			return toListResponses(postRepository.findAllByFestivalIdOrderByLikeCount(
					festivalId, LikeSort.withoutSort(page)
			));
		}
		return toListResponses(postRepository.findAllByFestivalId(festivalId, page));
	}

	private Page<PostResponse.ListResponse> toListResponses(Page<Post> posts) {
		return posts.map(post -> PostResponse.ListResponse.from(post, post.getLikeCount(), imageUrlResolver));
	}

	@Transactional(readOnly = true)
	public PostResponse.DetailResponse getPostDetail(UUID id, UUID memberId) {

		Post post = postRepository.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND));

		// 좋아요 버튼 상태용. 비회원은 항상 false.
		boolean likedByMe = memberId != null
				&& likeRepository.existsByPostIdAndMemberId(id, memberId);

		return PostResponse.DetailResponse.from(post, likedByMe, imageUrlResolver);
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
		// 화면은 썸네일을 안 바꿔도 URL을 다시 보내서 양쪽을 key로 맞춰 비교한다.
		String previousThumbnail = imageUrlResolver.toKey(post.getThumbnail());
		String thumbnail = imageUrlResolver.toKey(request.thumbnail());
		r2StorageService.requireOwnedBy(thumbnail, previousThumbnail, memberId);

		post.update(
			request.title(),
			sanitizedContent,
			thumbnail
		);
		deleteReplacedThumbnail(previousThumbnail, thumbnail, memberId);

		return PostResponse.DetailResponse.from(
				post,
				likeRepository.existsByPostIdAndMemberId(postId, memberId),
				imageUrlResolver
		);
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

	/** 교체된 옛 썸네일을 R2에서 지운다. */
	private void deleteReplacedThumbnail(String previousKey, String currentKey, UUID memberId) {
		if (previousKey == null || previousKey.equals(currentKey)) {
			return;
		}
		r2StorageService.deleteOwnedBy(previousKey, memberId);
	}

	private String sanitizeRequiredContent(String content) {
		String sanitizedContent = richTextSanitizer.sanitize(content);
		if (!richTextSanitizer.hasVisibleText(sanitizedContent)) {
			throw new BusinessException(CommonExceptionCode.INVALID_INPUT);
		}
		return sanitizedContent;
	}
}
