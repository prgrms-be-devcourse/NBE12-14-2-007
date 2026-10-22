package com.team007.room_escape.global.response.code;

import com.team007.room_escape.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 회원 도메인 에러 코드.
 * 조회 실패 등 회원 관련 비즈니스 예외에 사용한다.
 */
@Getter
@RequiredArgsConstructor
public enum MemberExceptionCode implements ExceptionCode {

	/** 해당 id/이메일 회원이 없거나 이미 소프트 삭제된 경우 */
	MEMBER_NOT_FOUND("MEMBER000", HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
	/** 탈퇴하지 않은 계정이 같은 이메일을 이미 사용 중 */
	EMAIL_DUPLICATED("MEMBER001", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
	/** 탈퇴 계정의 보관 기간 중이라 같은 이메일로 아직 가입할 수 없는 경우 */
	EMAIL_WITHDRAWN("MEMBER010", HttpStatus.CONFLICT,
		"탈퇴한 계정에서 사용 중인 이메일입니다. 개인정보 보관 기간이 지난 뒤에 다시 가입할 수 있습니다."),
	/** 탈퇴하지 않은 계정이 같은 닉네임을 이미 사용 중 */
	NICKNAME_DUPLICATED("MEMBER002", HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
	/** ROLE_WARNING 등급이 정보 수정 등 제한된 기능을 시도한 경우 */
	MEMBER_RESTRICTED("MEMBER003", HttpStatus.FORBIDDEN,
		"제재 중인 계정이라 정보를 수정할 수 없습니다. 관리자에게 문의해 주세요."),
	/** 관리자 계정의 등급은 화면에서 바꾸지 않는다. 관리자 박탈은 DB에서 직접 처리한다 */
	MEMBER_ROLE_ADMIN_PROTECTED("MEMBER004", HttpStatus.FORBIDDEN,
		"관리자 계정의 등급은 변경할 수 없습니다."),
	/** 등급 부여로 관리자를 만들 수 있으면 권한 상승 경로가 열린다 */
	MEMBER_ROLE_ADMIN_GRANT_DENIED("MEMBER005", HttpStatus.FORBIDDEN,
		"관리자 등급은 부여할 수 없습니다."),
	/** 자기 등급을 내리면 되돌릴 권한까지 잃는다 */
	MEMBER_ROLE_SELF_CHANGE_DENIED("MEMBER006", HttpStatus.FORBIDDEN,
		"본인의 등급은 변경할 수 없습니다."),
	/** 탈퇴한 회원은 등급을 바꿔도 의미가 없다 */
	MEMBER_ALREADY_DELETED("MEMBER007", HttpStatus.CONFLICT,
		"탈퇴한 회원의 등급은 변경할 수 없습니다."),
	/** 탈퇴 등 되돌리기 어려운 작업에서 본인 확인에 실패한 경우 */
	MEMBER_PASSWORD_MISMATCH("MEMBER008", HttpStatus.UNAUTHORIZED,
		"비밀번호가 일치하지 않습니다."),
	/** 관리자가 탈퇴하면 그 계정으로 하던 운영 업무를 이어받을 수 없다 */
	MEMBER_ADMIN_WITHDRAW_DENIED("MEMBER009", HttpStatus.FORBIDDEN,
		"관리자 계정은 탈퇴할 수 없습니다. 다른 관리자에게 문의해 주세요.");

	private final String code;
	private final HttpStatus status;
	private final String message;
}
