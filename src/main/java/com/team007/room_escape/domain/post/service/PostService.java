package com.team007.room_escape.domain.post.service;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.domain.post.infra.dto.PostRequest;
import com.team007.room_escape.domain.post.infra.dto.PostResponse;
import com.team007.room_escape.domain.post.infra.entity.Post;
import com.team007.room_escape.domain.post.type.PostSearchType;
import com.team007.room_escape.domain.post.infra.repository.PostRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import com.team007.room_escape.global.response.code.PostExceptionCode;
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
	private final MemberRepository memberRepository;


	@Transactional
	public PostResponse.CreateResponse createPost(
			Long festivalId,
			PostRequest request,
			UUID memberId
	) {
		Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
				.orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));

		if(member.isRestricted()) {
			throw new BusinessException(MemberExceptionCode.MEMBER_RESTRICTED);
		}

		Festival festival = festivalRepository.findById(festivalId)
				.orElseThrow(() -> new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_FOUND));

		Post post = Post.builder()
				.member(member)
				.festival(festival)
				.title(request.title())
				.content(request.content())
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
		if(type == null || keyword == null || keyword.isBlank()) {
			return postRepository.findAllNotDeleted(page)
					.map(PostResponse.ListResponse::from);
		}
		return postRepository.searchPosts(type, keyword, page)
				.map(PostResponse.ListResponse::from);
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
		return postRepository.findAllByFestivalId(festivalId, page)
				.map(PostResponse.ListResponse::from);
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
		Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
				.orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));

		if(member.isRestricted()) {
			throw new BusinessException(MemberExceptionCode.MEMBER_RESTRICTED);
		}

		Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
				.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND));

		if(!post.getMember().getId().equals(memberId)) {
			throw new BusinessException(PostExceptionCode.POST_FORBIDDEN);

		}
		post.update(request.title(), request.content(), request.thumbnail());

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
}
