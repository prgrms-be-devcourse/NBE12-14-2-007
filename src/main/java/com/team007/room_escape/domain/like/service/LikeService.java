package com.team007.room_escape.domain.like.service;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
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
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import com.team007.room_escape.global.response.code.LikeExceptionCode;
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
	private final MemberReader memberReader;
	private final MemberTrustGradeService memberTrustGradeService;

	/** 후기 */
	@Transactional
	public LikeResponse createPostLike(UUID postId, UUID memberId) {

		Member member = memberReader.getUnrestrictedMember(memberId);

		Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
				.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND));

		if(likeRepository.existsByPostIdAndMemberId(postId, memberId)) {
			throw new BusinessException(LikeExceptionCode.LIKE_ALREADY_EXISTS);
		}

		Like like = Like.builder()
				.post(post)
				.member(member)
				.build();

		likeRepository.save(like);
		postRepository.increaseLikeCount(postId);

		return new LikeResponse(getPostLikeCount(postId));
	}


	@Transactional(readOnly = true)
	public Long getPostLikeCount(UUID postId) {

		return postRepository.findLikeCountById(postId)
				.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND));
	}

	@Transactional
	public LikeResponse deletePostLike(UUID postId, UUID memberId) {

		if(!postRepository.existsById(postId)) {
			throw new BusinessException(PostExceptionCode.POST_NOT_FOUND);
		}

		Like like =  likeRepository.findByPostIdAndMemberId(postId, memberId)
				.orElseThrow(() -> new BusinessException(LikeExceptionCode.LIKE_NOT_FOUND));

		likeRepository.delete(like);
		postRepository.decreaseLikeCount(postId);

		return new LikeResponse(getPostLikeCount(postId));
	}

	/** 행사 */
	@Transactional
	public LikeResponse createFestivalLike(Long festivalId, UUID memberId) {

		Member member = memberReader.getUnrestrictedMember(memberId);

		Festival festival = festivalRepository.findById(festivalId)
				.orElseThrow(() -> new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_FOUND));

		if (festival.getMember() != null && festival.getMember().getId().equals(memberId)) {
			throw new BusinessException(LikeExceptionCode.SELF_FESTIVAL_LIKE_NOT_ALLOWED);
		}

		if(likeRepository.existsByFestivalIdAndMemberId(festivalId, memberId)) {
			throw new BusinessException(LikeExceptionCode.LIKE_ALREADY_EXISTS);
		}

		Like like = Like.builder()
				.festival(festival)
				.member(member)
				.build();

		likeRepository.save(like);
		likeRepository.flush();
		memberTrustGradeService.refreshForFestival(festival);

		Long likeCount = likeRepository.countByFestivalId(festivalId);

		return new LikeResponse(likeCount);
	}

	@Transactional(readOnly = true)
	public Long getFestivalLikeCount(Long festivalId) {

		if(!festivalRepository.existsById(festivalId)) {
			throw new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_FOUND);
		}

		return likeRepository.countByFestivalId(festivalId);
	}

	@Transactional
	public LikeResponse deleteFestivalLike(Long festivalId, UUID memberId) {

		if(!festivalRepository.existsById(festivalId)) {
			throw new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_FOUND);
		}

		Like like = likeRepository.findByFestivalIdAndMemberId(festivalId, memberId)
				.orElseThrow(() -> new BusinessException(LikeExceptionCode.LIKE_NOT_FOUND));

		Festival festival = like.getFestival();
		likeRepository.delete(like);
		likeRepository.flush();
		memberTrustGradeService.refreshForFestival(festival);

		Long likeCount = likeRepository.countByFestivalId(festivalId);

		return new LikeResponse(likeCount);
	}
}
