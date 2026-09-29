package com.team007.room_escape.domain.inquiry.service;

import com.team007.room_escape.domain.comment.infra.repository.CommentRepository;
import com.team007.room_escape.domain.community.infra.repository.CommunityCommentRepository;
import com.team007.room_escape.domain.community.infra.repository.CommunityPostRepository;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryTargetType;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.domain.post.infra.repository.PostRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.InquiryExceptionCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 신고·제보 대상이 실제로 있는지 확인한다.
 *
 * targetId는 FK가 아닌 문자열이라 DB가 존재 여부를 보장하지 않는다.
 * 대상 종류마다 ID 타입이 다르다. 행사·댓글·커뮤니티 댓글은 Long, 후기·회원·커뮤니티 글은 UUID다.
 */
@Component
@RequiredArgsConstructor
public class InquiryTargetValidator {

	private final FestivalRepository festivalRepository;
	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final MemberRepository memberRepository;
	private final CommunityPostRepository communityPostRepository;
	private final CommunityCommentRepository communityCommentRepository;

	/** 대상이 없는 일반 문의는 통과시킨다. 종류와 ID가 짝이 맞는지는 요청 DTO에서 이미 검사했다. */
	public void validate(InquiryTargetType type, String targetId) {
		if (type == null) {
			return;
		}

		boolean exists = switch (type) {
			case FESTIVAL -> festivalRepository.existsByIdAndDeletedAtIsNull(parseLong(targetId));
			case POST -> postRepository.existsByIdAndDeletedAtIsNull(parseUuid(targetId));
			// Comment는 @SQLRestriction이 걸려 있어 삭제된 댓글은 existsById에서 자동으로 빠진다.
			case COMMENT -> commentRepository.existsById(parseLong(targetId));
			case MEMBER -> memberRepository.existsByIdAndDeletedAtIsNull(parseUuid(targetId));
			// 커뮤니티 글·댓글도 @SQLRestriction으로 삭제된 행이 빠진다.
			case COMMUNITY_POST -> communityPostRepository.existsById(parseUuid(targetId));
			case COMMUNITY_COMMENT -> communityCommentRepository.existsById(parseLong(targetId));
		};

		if (!exists) {
			throw targetNotFound();
		}
	}

	private Long parseLong(String targetId) {
		try {
			return Long.valueOf(targetId.trim());
		} catch (NumberFormatException e) {
			throw targetNotFound();
		}
	}

	private UUID parseUuid(String targetId) {
		try {
			return UUID.fromString(targetId.trim());
		} catch (IllegalArgumentException e) {
			throw targetNotFound();
		}
	}

	private BusinessException targetNotFound() {
		return new BusinessException(InquiryExceptionCode.INQUIRY_TARGET_NOT_FOUND);
	}
}
