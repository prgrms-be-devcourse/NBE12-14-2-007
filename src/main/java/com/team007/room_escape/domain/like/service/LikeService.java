package com.team007.room_escape.domain.like.service;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.like.infra.dto.LikeResponse;
import com.team007.room_escape.domain.like.infra.entity.Like;
import com.team007.room_escape.domain.like.infra.repository.LikeRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.domain.post.infra.entity.Post;
import com.team007.room_escape.domain.post.infra.repository.PostRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import com.team007.room_escape.global.response.code.LikeExceptionCode;
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import com.team007.room_escape.global.response.code.PostExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LikeService {

	private final LikeRepository likeRepository;
	private final PostRepository postRepository;
	private final FestivalRepository festivalRepository;
	private final MemberRepository memberRepository;

	/** 후기 */
	@Transactional
	public LikeResponse createPostLike(UUID postId, UUID memberId) {

		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND));

		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));

		if(likeRepository.existsByPostIdAndMemberId(postId, memberId)) {
			throw new BusinessException(LikeExceptionCode.LIKE_ALREADY_EXISTS);
		}

		Like like = Like.builder()
				.post(post)
				.member(member)
				.build();

		likeRepository.save(like);

		Long likeCount = likeRepository.countByPostId(postId);

		return new LikeResponse(likeCount);
	}


	@Transactional(readOnly = true)
	public Long getPostLikeCount(UUID postId) {

		if(!postRepository.existsById(postId)) {
			throw new BusinessException(PostExceptionCode.POST_NOT_FOUND);
		}

		return likeRepository.countByPostId(postId);
	}

	@Transactional
	public LikeResponse deletePostLike(UUID postId, UUID memberId) {

		if(!postRepository.existsById(postId)) {
			throw new BusinessException(PostExceptionCode.POST_NOT_FOUND);
		}

		Like like =  likeRepository.findByPostIdAndMemberId(postId, memberId)
				.orElseThrow(() -> new BusinessException(LikeExceptionCode.LIKE_NOT_FOUND));

		likeRepository.delete(like);

		Long likeCount = likeRepository.countByPostId(postId);

		return new LikeResponse(likeCount);
	}

	/** 행사 */
	@Transactional
	public LikeResponse createFestivalLike(Long festivalId, UUID memberId) {

		Festival festival = festivalRepository.findById(festivalId)
				.orElseThrow(() -> new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_FOUND));

		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));

		if(likeRepository.existsByFestivalIdAndMemberId(festivalId, memberId)) {
			throw new BusinessException(LikeExceptionCode.LIKE_ALREADY_EXISTS);
		}

		Like like = Like.builder()
				.festival(festival)
				.member(member)
				.build();

		likeRepository.save(like);

		Long likeCount = likeRepository.countByFestivalId(festivalId);

		return new LikeResponse(likeCount);
	}

	@Transactional
	public LikeResponse deleteFestivalLike(Long festivalId, UUID memberId) {

		if(!festivalRepository.existsById(festivalId)) {
			throw new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_FOUND);
		}

		Like like = likeRepository.findByFestivalIdAndMemberId(festivalId, memberId)
				.orElseThrow(() -> new BusinessException(LikeExceptionCode.LIKE_NOT_FOUND));

		likeRepository.delete(like);

		Long likeCount = likeRepository.countByFestivalId(festivalId);

		return new LikeResponse(likeCount);
	}
}
