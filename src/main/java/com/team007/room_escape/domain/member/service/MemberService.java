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
	/** 프로필 이미지를 교체할 때 옛 파일을 지우려면 저장소를 직접 다뤄야 한다. */
	private final R2StorageService r2StorageService;
	private final EmailVerificationService emailVerificationService;
	private final MailSendService mailSendService;
	private final RefreshTokenService refreshTokenService;
	private final PasswordEncoder passwordEncoder;

	/**
	 * 마이페이지 정보를 조회한다.
	 *
	 * 토큰은 발급 시점의 값만 담고 있어 그대로 믿을 수 없다.
	 * 토큰이 아직 유효해도 그 사이 탈퇴했거나 정보가 바뀌었을 수 있으므로
	 * DB에서 "살아있는 회원"인지 다시 확인하고 최신 값을 읽는다.
	 */
	@Transactional(readOnly = true)
	public MemberResponse.MyPageInfo getMyPage(UUID memberId) {
		Member member = memberReader.getActiveMember(memberId);

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
		Member member = memberReader.getUnrestrictedMember(memberId);

		validateNickname(member, request.nickname());
		r2StorageService.requireOwnedBy(request.profileImg(), member.getProfileImg(), memberId);

		String previousProfileImg = member.getProfileImg();
		member.updateProfile(request.nickname(), request.phone(), request.profileImg());

		deleteReplacedProfileImage(previousProfileImg, member.getProfileImg(), memberId);

		return MemberResponse.MyPageInfo.from(member, imageUrlResolver.resolve(member.getProfileImg()));
	}

	/**
	 * 비밀번호 변경용 인증 코드를 본인 가입 이메일로 보낸다.
	 *
	 * 받는 주소는 요청으로 받지 않고 DB의 가입 이메일을 쓴다.
	 * 주소를 입력받으면 남의 계정 코드를 자기 메일로 빼돌릴 수 있기 때문이다.
	 */
	@Transactional
	public void sendPasswordChangeCode(UUID memberId) {
		Member member = memberReader.getUnrestrictedMember(memberId);

		String code = emailVerificationService.issueCode(memberId, Purpose.PASSWORD_CHANGE);

		mailSendService.sendPasswordChangeCode(member.getEmail(), code);
	}

	/**
	 * 1단계. 메일로 받은 인증 코드가 맞는지만 확인한다.
	 * 통과하면 잠시 인증 상태가 유지되고, 그 사이에 새 비밀번호를 설정하면 된다.
	 */
	@Transactional(readOnly = true)
	public void verifyPasswordChangeCode(UUID memberId, MemberRequest.VerifyPassword request) {
		memberReader.getUnrestrictedMember(memberId);

		emailVerificationService.verifyCode(memberId, Purpose.PASSWORD_CHANGE, request.code());
	}

	/**
	 * 2단계. 인증을 통과한 상태에서 새 비밀번호를 설정한다.
	 * 변경에 성공하면 Refresh Token을 지워 다른 기기의 세션을 끊는다.
	 */
	@Transactional
	public void changePassword(UUID memberId, MemberRequest.ChangePassword request) {
		Member member = memberReader.getUnrestrictedMember(memberId);

		// 인증 상태를 소모하기 전에 확인한다.
		// 인증 상태는 캐시에 있어 트랜잭션 롤백으로 되살아나지 않으므로,
		// 여기서 실패하면 인증만 날아가고 사용자는 코드를 다시 받아야 한다.
		if (passwordEncoder.matches(request.newPassword(), member.getPassword())) {
			throw new BusinessException(AuthExceptionCode.SAME_AS_OLD_PASSWORD);
		}

		emailVerificationService.consumeVerified(memberId, Purpose.PASSWORD_CHANGE);

		member.changePassword(passwordEncoder.encode(request.newPassword()));

		// 비밀번호가 바뀌었으면 기존 세션은 더 이상 유효하지 않아야 한다.
		refreshTokenService.deleteByMemberId(memberId);
	}

	/**
	 * 회원 탈퇴. 행을 지우지 않고 탈퇴 시각만 남긴다.
	 *
	 * 작성한 후기·댓글·문의는 그대로 둔다. 회원 행을 지우면 그것들이 참조를 잃고,
	 * 남이 쓴 글의 흐름까지 끊긴다. 화면에서는 "탈퇴한 사용자"로 보여준다.
	 *
	 * 이메일 유니크 인덱스(uk_member_email)가 WHERE deleted_at IS NULL 로 걸려 있어
	 * 탈퇴 후 같은 이메일로 다시 가입할 수 있다.
	 *
	 * TODO 개인정보 보관 기간이 정해지면 일정 기간 뒤 이메일·전화번호를 파기할 것.
	 *      지금은 탈퇴해도 회원 행에 그대로 남아 있다.
	 */
	@Transactional
	public void withdraw(UUID memberId, MemberRequest.Withdraw request) {
		Member member = memberReader.getActiveMember(memberId);

		// 관리자가 스스로 나가면 그 계정으로 하던 운영 업무를 이어받을 수 없다.
		if (member.getRole().isAdmin()) {
			throw new BusinessException(MemberExceptionCode.MEMBER_ADMIN_WITHDRAW_DENIED);
		}

		// 로그인 상태만으로는 부족하다. 자리를 비운 사이 남이 눌러도 탈퇴가 되면 안 된다.
		if (!passwordEncoder.matches(request.password(), member.getPassword())) {
			throw new BusinessException(MemberExceptionCode.MEMBER_PASSWORD_MISMATCH);
		}

		member.delete();

		// 탈퇴했는데 리프레시 토큰이 남아 있으면 그 쿠키로 계속 재발급이 된다.
		refreshTokenService.deleteByMemberId(memberId);
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
	private void deleteReplacedProfileImage(String previousKey, String currentKey, UUID memberId) {
		if (previousKey == null || previousKey.equals(currentKey)) {
			return;
		}
		r2StorageService.deleteOwnedBy(previousKey, memberId);
	}
}
