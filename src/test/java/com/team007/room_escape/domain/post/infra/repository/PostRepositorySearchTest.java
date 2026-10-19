package com.team007.room_escape.domain.post.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.domain.post.infra.entity.Post;
import com.team007.room_escape.domain.post.type.PostSearchType;

/** 탈퇴 회원의 원래 닉네임으로 후기를 찾을 수 없어야 한다. 화면에서 가린 이름이 검색으로 드러나면 안 된다. */
@SpringBootTest
@Transactional
class PostRepositorySearchTest {

	@Autowired
	private PostRepository postRepository;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private FestivalRepository festivalRepository;

	private final PageRequest page = PageRequest.of(0, 10);

	// 시드 데이터와 겹치지 않게 테스트마다 다른 검색어를 쓴다.
	private final String token = UUID.randomUUID().toString().substring(0, 8);

	private Post activePost;
	private Post withdrawnPost;

	@BeforeEach
	void setUp() {
		Festival festival = festivalRepository.save(Festival.builder()
			.providerType(ProviderType.PUBLIC)
			.title("행사 " + token)
			.beginDe(LocalDateTime.now())
			.endDe(LocalDateTime.now().plusDays(1))
			.build());

		Member active = memberRepository.save(member("활동" + token));
		Member withdrawn = member("탈퇴" + token);
		withdrawn.delete();
		memberRepository.save(withdrawn);

		activePost = postRepository.save(post(active, festival, "활동 후기 " + token));
		withdrawnPost = postRepository.save(post(withdrawn, festival, "탈퇴 후기 " + token));
	}

	@Test
	void 닉네임_검색은_탈퇴_회원의_후기를_찾지_않는다() {
		Page<Post> result = postRepository.searchPosts(PostSearchType.MEMBER_NICKNAME, token, page);

		assertThat(result.getContent()).extracting(Post::getId).containsExactly(activePost.getId());
		assertThat(result.getTotalElements()).isEqualTo(1);
	}

	@Test
	void 좋아요순_닉네임_검색도_탈퇴_회원의_후기를_찾지_않는다() {
		Page<Post> result = postRepository.searchPostsOrderByLikeCount(
			PostSearchType.MEMBER_NICKNAME.name(), token, page);

		assertThat(result.getContent()).extracting(Post::getId).containsExactly(activePost.getId());
		assertThat(result.getTotalElements()).isEqualTo(1);
	}

	@Test
	void 탈퇴_회원의_원래_닉네임을_그대로_넣어도_찾지_않는다() {
		Page<Post> result = postRepository.searchPosts(PostSearchType.MEMBER_NICKNAME, "탈퇴" + token, page);

		assertThat(result.getContent()).isEmpty();
		assertThat(result.getTotalElements()).isZero();
	}

	@Test
	void 제목_검색에서는_탈퇴_회원의_후기도_그대로_나온다() {
		Page<Post> latest = postRepository.searchPosts(PostSearchType.TITLE, token, page);
		Page<Post> byLike = postRepository.searchPostsOrderByLikeCount(PostSearchType.TITLE.name(), token, page);

		assertThat(latest.getContent()).extracting(Post::getId)
			.containsExactlyInAnyOrder(activePost.getId(), withdrawnPost.getId());
		assertThat(byLike.getContent()).extracting(Post::getId)
			.containsExactlyInAnyOrder(activePost.getId(), withdrawnPost.getId());
	}

	@Test
	void 관리자_닉네임_검색은_탈퇴_회원의_후기도_찾는다() {
		Page<Post> result = postRepository.searchPostsIncludingDeleted(PostSearchType.MEMBER_NICKNAME, token, page);

		assertThat(result.getContent()).extracting(Post::getId)
			.containsExactlyInAnyOrder(activePost.getId(), withdrawnPost.getId());
	}

	private Member member(String nickname) {
		return Member.builder()
			.email(nickname + "@test.com")
			.password("encoded")
			.role(MemberRole.ROLE_UNVERIFIED)
			.nickname(nickname)
			.build();
	}

	private Post post(Member member, Festival festival, String title) {
		return Post.builder()
			.member(member)
			.festival(festival)
			.title(title)
			.content("본문")
			.build();
	}
}
