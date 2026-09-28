package com.team007.room_escape.domain.community.infra.repository;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CommunityPostRepositoryTest {

	@Autowired
	private CommunityPostRepository postRepository;

	@Test
	void 검색어가_없어도_목록을_조회할_수_있다() {
		assertThatCode(() -> postRepository.search(null, "", PageRequest.of(0, 10)))
			.doesNotThrowAnyException();
	}
}
