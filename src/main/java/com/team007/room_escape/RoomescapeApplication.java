package com.team007.room_escape;

import com.team007.room_escape.global.config.AuthMailProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
// 메일 발송(SMTP)이 요청 스레드를 붙잡지 않도록 비동기를 켠다.
@EnableAsync
@EnableConfigurationProperties(AuthMailProperties.class)
public class RoomescapeApplication {

	public static void main(String[] args) {
		SpringApplication.run(RoomescapeApplication.class, args);
	}

}
