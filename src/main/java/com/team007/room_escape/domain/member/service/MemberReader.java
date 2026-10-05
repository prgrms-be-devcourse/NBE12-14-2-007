package com.team007.room_escape.domain.member.service;

import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 로그인한 회원을 DB에서 다시 읽는다. 토큰이 유효해도 그 사이 탈퇴·제재됐을 수 있다. */
@Component
@RequiredArgsConstructor
public class MemberReader {

	private final MemberRepository memberRepository;

	/** 탈퇴하지 않은 회원. 조회처럼 제재 회원도 할 수 있는 기능에 쓴다. */
	public Member getActiveMember(UUID memberId) {
		return memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));
	}

	/** 탈퇴하지 않았고 제재(ROLE_WARNING)도 아닌 회원. 글쓰기·수정 같은 기능에 쓴다. */
	public Member getUnrestrictedMember(UUID memberId) {
		Member member = getActiveMember(memberId);
		if (member.isRestricted()) {
			throw new BusinessException(MemberExceptionCode.MEMBER_RESTRICTED);
		}
		return member;
	}
}
