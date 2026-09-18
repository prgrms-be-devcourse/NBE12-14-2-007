package com.team007.room_escape.domain.member.service;

import com.team007.room_escape.domain.member.dto.MemberResponse;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import com.team007.room_escape.global.storage.ImageUrlResolver;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final ImageUrlResolver imageUrlResolver;

	/**
	 * 마이페이지 정보를 조회한다.
	 *
	 * 토큰은 발급 시점의 값만 담고 있어 그대로 믿을 수 없다.
	 * 토큰이 아직 유효해도 그 사이 탈퇴했거나 정보가 바뀌었을 수 있으므로
	 * DB에서 "살아있는 회원"인지 다시 확인하고 최신 값을 읽는다.
	 */
	@Transactional(readOnly = true)
	public MemberResponse.MyPageInfo getMyPage(UUID memberId) {
		Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));

		// profileImg는 R2 key로 저장되므로 응답에서는 공개 URL로 바꿔 내려준다.
		return MemberResponse.MyPageInfo.from(member, imageUrlResolver.resolve(member.getProfileImg()));
	}
}
