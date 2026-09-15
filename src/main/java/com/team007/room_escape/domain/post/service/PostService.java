package com.team007.room_escape.domain.post.service;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.domain.post.infra.dto.PostRequest;
import com.team007.room_escape.domain.post.infra.dto.PostResponse;
import com.team007.room_escape.domain.post.infra.entity.Post;
import com.team007.room_escape.domain.post.infra.repository.PostRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import com.team007.room_escape.global.response.code.PostExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
			PostRequest.PostCreateRequest request,
			UUID memberId
	) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));

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
	public List<PostResponse.ListResponse> getPosts() {
		return postRepository.findAll().stream()
				.map(PostResponse.ListResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public PostResponse.DetailResponse findPostDetailById(UUID id) {

		Post post = postRepository.findById(id)
				.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND));

		return PostResponse.DetailResponse.from(post);
	}

	@Transactional
	public PostResponse.DetailResponse updatePost(
			UUID postId,
			PostRequest.PostUpdateRequest request,
			UUID memberId
	) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND));

		if(!post.getMember().getId().equals(memberId)) {
			throw new BusinessException(PostExceptionCode.POST_UPDATE_FORBIDDEN);

		}
		post.update(request.title(), request.content());

		return PostResponse.DetailResponse.from(post);
	}

	@Transactional
	public void deletePost(UUID postId, UUID memberId) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND));

		if(!post.getMember().getId().equals(memberId)) {
			throw new BusinessException(PostExceptionCode.POST_DELETE_FORBIDDEN);
		}

		post.delete();
	}
}
