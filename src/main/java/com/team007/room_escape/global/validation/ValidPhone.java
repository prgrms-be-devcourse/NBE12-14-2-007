package com.team007.room_escape.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 회원 휴대폰 번호 형식 검증.
 *
 * 하이픈 없이 숫자만 받는다. 하이픈 유무가 섞이면 같은 번호가 다른 값이 되어
 * 중복 검사나 발송 연동에서 문제가 생기기 때문이다. 화면 표시용 하이픈은 프론트가 붙인다.
 *
 * 010-2323-2323 → 01023232323
 *
 * 빈 문자열을 허용하는 이유는 마이페이지 수정에서 "번호 지우기"를 빈 문자열로 표현하기 때문이다.
 * 빈 문자열은 저장 직전에 null 로 바꾼다.
 * null 은 @Pattern 이 원래 통과시키므로 선택 입력이 그대로 유지된다.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Pattern(
	regexp = "^$|^01[016789]\\d{7,8}$",
	message = "휴대폰 번호는 하이픈 없이 숫자만 입력해 주세요."
)
public @interface ValidPhone {

	String message() default "";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
