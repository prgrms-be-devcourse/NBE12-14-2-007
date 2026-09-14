package com.team007.room_escape.domain.post.service;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.domain.post.infra.dto.PostCreateRequest;
import com.team007.room_escape.domain.post.infra.dto.PostCreateResponse;
import com.team007.room_escape.domain.post.infra.dto.PostDetailDto;
import com.team007.room_escape.domain.post.infra.entity.Post;
import com.team007.room_escape.domain.post.infra.repository.PostRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import com.team007.room_escape.global.response.code.PostExceptionCode;
import lombok.RequiredArgsConstructor;
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
	public PostCreateResponse createPost(
			Long festivalId,
			PostCreateRequest request,
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

		Post savePost = postRepository.save(post);

		return PostCreateResponse.from(savePost);
	}


	@Transactional(readOnly = true)
	public PostDetailDto findPostDetailById(UUID id) {

		Post post = postRepository.findById(id)
				.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND));

		return PostDetailDto.from(post);
	}
}
