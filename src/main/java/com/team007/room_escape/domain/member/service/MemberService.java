package com.team007.room_escape.domain.member.service;

import com.team007.room_escape.domain.auth.service.EmailVerificationService.Purpose;
import com.team007.room_escape.domain.auth.service.EmailVerificationService;
import com.team007.room_escape.domain.auth.service.RefreshTokenService;
import com.team007.room_escape.domain.member.dto.MemberRequest;
import com.team007.room_escape.domain.member.dto.MemberResponse;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.mail.MailSendService;
import com.team007.room_escape.global.response.code.AuthExceptionCode;
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import com.team007.room_escape.global.storage.ImageUrlResolver;
import com.team007.room_escape.global.storage.R2StorageService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final MemberReader memberReader;
	private final ImageUrlResolver imageUrlResolver;
	private final R2StorageService r2StorageService;
	private final EmailVerificationService emailVerificationService;
	private final MailSendService mailSendService;
	private final RefreshTokenService refreshTokenService;
	private final PasswordEncoder passwordEncoder;

	/** 마이페이지 조회. 토큰 값은 발급 시점 기준이라 DB에서 최신 값을 다시 읽는다. */
	@Transactional(readOnly = true)
	public MemberResponse.MyPageInfo getMyPage(UUID memberId) {
		Member member = memberReader.getActiveMember(memberId);

		return toMyPageInfo(member);
	}

	/** 마이페이지 수정. 제재 회원은 신고 회피를 막으려고 수정할 수 없다. */
	@Transactional
	public MemberResponse.MyPageInfo updateMyPage(UUID memberId, MemberRequest.UpdateMyPage request) {
		Member member = memberReader.getUnrestrictedMember(memberId);

		validateNickname(member, request.nickname());
		r2StorageService.requireOwnedBy(request.profileImg(), member.getProfileImg(), memberId);

		String previousProfileImg = member.getProfileImg();
		member.updateProfile(request.nickname(), request.phone(), request.profileImg());

		deleteReplacedProfileImage(previousProfileImg, member.getProfileImg(), memberId);

		return toMyPageInfo(member);
	}

	/** 인증 코드를 가입 이메일로 보낸다. 메일 발송 중 커넥션을 잡지 않으려고 트랜잭션을 걸지 않는다. */
	public void sendPasswordChangeCode(UUID memberId) {
		Member member = memberReader.getUnrestrictedMember(memberId);

		String code = emailVerificationService.issueCode(memberId, Purpose.PASSWORD_CHANGE);

		mailSendService.sendPasswordChangeCode(member.getEmail(), code);
	}

	/** 비밀번호 변경 1단계. 인증 코드가 맞는지 확인한다. */
	@Transactional(readOnly = true)
	public void verifyPasswordChangeCode(UUID memberId, MemberRequest.VerifyPassword request) {
		memberReader.getUnrestrictedMember(memberId);

		emailVerificationService.verifyCode(memberId, Purpose.PASSWORD_CHANGE, request.code());
	}

	/** 비밀번호 변경 2단계. 변경 후 Refresh Token을 지워 다른 기기의 세션을 끊는다. */
	@Transactional
	public void changePassword(UUID memberId, MemberRequest.ChangePassword request) {
		Member member = memberReader.getUnrestrictedMember(memberId);

		// 인증 상태는 롤백되지 않으므로 소모하기 전에 확인한다.
		if (passwordEncoder.matches(request.newPassword(), member.getPassword())) {
			throw new BusinessException(AuthExceptionCode.SAME_AS_OLD_PASSWORD);
		}

		emailVerificationService.consumeVerified(memberId, Purpose.PASSWORD_CHANGE);

		member.changePassword(passwordEncoder.encode(request.newPassword()));

		refreshTokenService.deleteByMemberId(memberId);
	}

	/** 회원 탈퇴. 작성한 글이 참조를 잃지 않게 행은 지우지 않고 탈퇴 시각만 남긴다. */
	@Transactional
	public void withdraw(UUID memberId, MemberRequest.Withdraw request) {
		Member member = memberReader.getActiveMember(memberId);

		// 관리자는 운영 업무 인계가 안 되므로 탈퇴 불가.
		if (member.getRole().isAdmin()) {
			throw new BusinessException(MemberExceptionCode.MEMBER_ADMIN_WITHDRAW_DENIED);
		}

		// 자리를 비운 사이 남이 탈퇴시키지 못하게 비밀번호를 다시 확인한다.
		if (!passwordEncoder.matches(request.password(), member.getPassword())) {
			throw new BusinessException(MemberExceptionCode.MEMBER_PASSWORD_MISMATCH);
		}

		member.delete();

		// 리프레시 토큰이 남으면 탈퇴 후에도 재발급이 된다.
		refreshTokenService.deleteByMemberId(memberId);
	}

	/** 지금 쓰던 닉네임 그대로면 중복 검사를 건너뛴다. */
	private void validateNickname(Member member, String nickname) {
		if (nickname == null || nickname.equals(member.getNickname())) {
			return;
		}
		if (memberRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
			throw new BusinessException(MemberExceptionCode.NICKNAME_DUPLICATED);
		}
	}

	/** 프로필 이미지 key를 공개 URL로 바꿔 내려준다. */
	private MemberResponse.MyPageInfo toMyPageInfo(Member member) {
		return MemberResponse.MyPageInfo.from(member, imageUrlResolver.resolve(member.getProfileImg()));
	}

	/** 교체된 옛 이미지를 R2에서 지운다. */
	private void deleteReplacedProfileImage(String previousKey, String currentKey, UUID memberId) {
		if (previousKey == null || previousKey.equals(currentKey)) {
			return;
		}
		r2StorageService.deleteOwnedBy(previousKey, memberId);
	}
}
