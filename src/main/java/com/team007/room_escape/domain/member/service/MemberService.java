package com.team007.room_escape.domain.member.service;

import com.team007.room_escape.domain.member.dto.MemberRequest;
import com.team007.room_escape.domain.member.dto.MemberResponse;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import com.team007.room_escape.global.storage.ImageUrlResolver;
import com.team007.room_escape.global.storage.R2StorageService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final ImageUrlResolver imageUrlResolver;
	/** 프로필 이미지를 교체할 때 옛 파일을 지우려면 저장소를 직접 다뤄야 한다. */
	private final R2StorageService r2StorageService;

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

	/**
	 * 마이페이지 정보를 수정한다. 보낸 필드만 반영한다.
	 *
	 * ROLE_WARNING(제재) 등급은 수정할 수 없다.
	 * 닉네임·프로필 이미지를 바꿔 신고 이력을 회피하는 걸 막기 위함이다.
	 */
	@Transactional
	public MemberResponse.MyPageInfo updateMyPage(UUID memberId, MemberRequest.UpdateMyPage request) {
		Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));

		if (member.isRestricted()) {
			throw new BusinessException(MemberExceptionCode.MEMBER_RESTRICTED);
		}

		validateNickname(member, request.nickname());

		String previousProfileImg = member.getProfileImg();
		member.updateProfile(request.nickname(), request.phone(), request.profileImg());

		deleteReplacedProfileImage(previousProfileImg, member.getProfileImg());

		return MemberResponse.MyPageInfo.from(member, imageUrlResolver.resolve(member.getProfileImg()));
	}

	/** 지금 쓰던 닉네임 그대로면 중복 검사를 건너뛴다. 본인 닉네임에 걸리면 안 되기 때문이다. */
	private void validateNickname(Member member, String nickname) {
		if (nickname == null || nickname.equals(member.getNickname())) {
			return;
		}
		if (memberRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
			throw new BusinessException(MemberExceptionCode.NICKNAME_DUPLICATED);
		}
	}

	/** 교체된 옛 이미지는 R2에서 지운다. 안 지우면 쓰지 않는 파일이 계속 쌓인다. */
	private void deleteReplacedProfileImage(String previousKey, String currentKey) {
		if (previousKey == null || previousKey.equals(currentKey)) {
			return;
		}
		r2StorageService.delete(previousKey);
	}
}
