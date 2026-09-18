package com.team007.room_escape.global.mail;

import com.team007.room_escape.global.config.AuthMailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 메일 발송.
 *
 * SMTP는 응답까지 수 초가 걸려서 요청 스레드에서 보내면 API가 그만큼 느려진다.
 * 그래서 @Async로 빼고, 발송 실패는 로그만 남긴다.
 * (인증 코드는 이미 DB에 저장돼 있으므로 사용자는 재발송으로 복구할 수 있다)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailSendService {

	private final JavaMailSender mailSender;
	private final AuthMailProperties properties;

	@Value("${spring.mail.username}")
	private String from;

	@Async
	public void sendPasswordChangeCode(String to, String code) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(to);
		message.setSubject("[roomescape] 비밀번호 변경 인증 코드");
		message.setText("""
			비밀번호 변경을 위한 인증 코드입니다.

			    %s

			유효시간은 %d분입니다.
			본인이 요청하지 않았다면 이 메일을 무시하고 비밀번호를 바꾸지 마세요.
			""".formatted(code, properties.expireMinutes()));

		try {
			mailSender.send(message);
			log.info("비밀번호 변경 인증 메일 발송 완료 to={}", mask(to));
		} catch (MailException e) {
			log.error("비밀번호 변경 인증 메일 발송 실패 to={}", mask(to), e);
		}
	}

	/** 로그에 이메일 전체를 남기지 않는다. */
	private String mask(String email) {
		int at = email.indexOf('@');
		if (at <= 1) {
			return "***";
		}
		return email.charAt(0) + "***" + email.substring(at);
	}
}
