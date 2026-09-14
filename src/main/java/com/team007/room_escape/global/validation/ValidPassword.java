package com.team007.room_escape.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 비밀번호 형식 검증.
 * 8~15자, 영문·숫자·특수문자(!@#%^&*)를 모두 포함해야 한다.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Size(min = 8, max = 25, message = "비밀번호는 8~25자여야 합니다.")
@Pattern(
	regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#%^&*]).*$",
	message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다."
)
public @interface ValidPassword {

	String message() default "";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}