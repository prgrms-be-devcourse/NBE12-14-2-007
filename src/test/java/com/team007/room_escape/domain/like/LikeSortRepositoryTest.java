package com.team007.room_escape.domain.like;

import static org.assertj.core.api.Assertions.assertThat;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.like.infra.dto.FestivalLikeCount;
import com.team007.room_escape.domain.like.infra.dto.PostLikeCount;
import com.team007.room_escape.domain.like.infra.entity.Like;
import com.team007.room_escape.domain.like.infra.repository.LikeRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import com.team007.room_escape.domain.post.infra.entity.Post;
import com.team007.room_escape.domain.post.infra.repository.PostRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 좋아요순 정렬 쿼리를 실제 PostgreSQL 에서 검증한다.
 * 집계 서브쿼리 JOIN, 동점 처리, 페이징은 DB 가 해야 의미가 있어서 목(mock)으로는 확인할 수 없다.
 *
 * 로컬 DB(.env)에 붙고, 테스트마다 트랜잭션이 롤백돼 데이터가 남지 않는다.
 * 로컬 DB에 다른 데이터가 있어도 섞이지 않게, 테스트마다 고유 토큰을 제목에 넣고 그 토큰으로 검색한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LikeSortRepositoryTest {

	private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 1, 1, 12, 0);

	@Autowired
	EntityManager em;

	@Autowired
	PostRepository postRepository;

	@Autowired
	FestivalRepository festivalRepository;

	@Autowired
	LikeRepository likeRepository;

	private String token;
	private List<Member> likers;

	@BeforeEach
	void setUp() {
		token = "likesort" + UUID.randomUUID().toString().substring(0, 8);
		likers = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			likers.add(persist(Member.builder()
					.email(token + i + "@test.local")
					.password("x")
					.role(MemberRole.ROLE_UNVERIFIED)
					.nickname(token + "-member" + i)
					.build()));
		}
	}

	@Nested
	@DisplayName("후기 좋아요순")
	class PostLikeSort {

		private Festival festival;

		@BeforeEach
		void setUp() {
			festival = festival("후기용 행사", BASE_TIME, FestivalRegion.values()[0]);
		}

		@Test
		@DisplayName("좋아요가 많은 순으로 정렬되고, 좋아요가 없는 후기는 뒤에 온다")
		void ordersByLikeCountDesc() {
			Post three = post("three");
			Post none = post("none");
			Post one = post("one");
			likePost(three, 3);
			likePost(one, 1);
			flushAndClear();

			Page<Post> page = searchByToken(PageRequest.of(0, 10));

			assertThat(titles(page)).containsExactly(title("three"), title("one"), title("none"));
		}

		@Test
		@DisplayName("좋아요 수가 같으면 최신 작성순, 작성 시각도 같으면 id 역순으로 고정된다")
		void breaksTiesByCreatedAtThenId() {
			Post older = post("older");
			Post newer = post("newer");
			Post sameTimeFirst = post("same-first");
			Post sameTimeSecond = post("same-second");
			for (Post post : List.of(older, newer, sameTimeFirst, sameTimeSecond)) {
				likePost(post, 2);
			}
			setCreatedAt(older, BASE_TIME);
			setCreatedAt(newer, BASE_TIME.plusHours(2));
			setCreatedAt(sameTimeFirst, BASE_TIME.plusHours(1));
			setCreatedAt(sameTimeSecond, BASE_TIME.plusHours(1));
			flushAndClear();

			Page<Post> page = searchByToken(PageRequest.of(0, 10));

			// UUID v7 은 나중에 만든 쪽이 더 크다 → id 역순이면 same-second 가 먼저
			assertThat(titles(page)).containsExactly(
					title("newer"), title("same-second"), title("same-first"), title("older"));
		}

		@Test
		@DisplayName("삭제된 후기는 좋아요가 많아도 목록과 전체 개수에서 빠진다")
		void excludesDeletedPosts() {
			Post deleted = post("deleted");
			Post alive = post("alive");
			likePost(deleted, 5);
			likePost(alive, 1);
			deleted.delete();
			flushAndClear();

			Page<Post> page = searchByToken(PageRequest.of(0, 10));

			assertThat(titles(page)).containsExactly(title("alive"));
			assertThat(page.getTotalElements()).isEqualTo(1);
		}

		@Test
		@DisplayName("페이지를 넘겨도 중복·누락 없이 전체 순서가 이어진다")
		void keepsOrderAcrossPageBoundaries() {
			// 동점(2개)이 페이지 경계에 걸치도록 구성
			likePost(post("p4"), 4);
			likePost(post("p2-a"), 2);
			likePost(post("p2-b"), 2);
			likePost(post("p1"), 1);
			post("p0");
			flushAndClear();

			List<String> expected = titles(searchByToken(PageRequest.of(0, 10)));
			List<String> paged = new ArrayList<>();
			Page<Post> page = searchByToken(PageRequest.of(0, 2));
			paged.addAll(titles(page));
			paged.addAll(titles(searchByToken(PageRequest.of(1, 2))));
			paged.addAll(titles(searchByToken(PageRequest.of(2, 2))));

			assertThat(page.getTotalElements()).isEqualTo(5);
			assertThat(page.getTotalPages()).isEqualTo(3);
			assertThat(paged).containsExactlyElementsOf(expected).doesNotHaveDuplicates();
			assertThat(expected.getFirst()).isEqualTo(title("p4"));
			assertThat(expected.getLast()).isEqualTo(title("p0"));
		}

		@Test
		@DisplayName("행사별 목록은 그 행사의 후기만 좋아요순으로 보여준다")
		void filtersByFestival() {
			Festival other = festival("다른 행사", BASE_TIME, FestivalRegion.values()[0]);
			Post mine = post("mine");
			Post others = persist(Post.builder()
					.member(likers.getFirst()).festival(other)
					.title(title("others")).content("c").build());
			likePost(mine, 1);
			likePost(others, 3);
			flushAndClear();

			Page<Post> page = postRepository.findAllOrderByLikeCount(
					true, festival.getId(), false, null, null, PageRequest.of(0, 10));

			assertThat(titles(page)).containsExactly(title("mine"));
			assertThat(page.getTotalElements()).isEqualTo(1);
		}

		@Test
		@DisplayName("행사에 누른 좋아요는 후기 좋아요 수에 섞이지 않는다")
		void ignoresFestivalLikes() {
			Post post = post("post");
			likePost(post, 1);
			likeFestival(festival, 4);
			flushAndClear();

			Map<UUID, Long> counts = likeRepository.countByPostIds(List.of(post.getId())).stream()
					.collect(Collectors.toMap(PostLikeCount::postId, PostLikeCount::likeCount));

			assertThat(counts).containsExactly(Map.entry(post.getId(), 1L));
		}

		private Page<Post> searchByToken(PageRequest pageable) {
			return postRepository.findAllOrderByLikeCount(false, null, true, "TITLE", token, pageable);
		}

		private Post post(String name) {
			return persist(Post.builder()
					.member(likers.getFirst())
					.festival(festival)
					.title(title(name))
					.content("content")
					.build());
		}
	}

	@Nested
	@DisplayName("행사 좋아요순")
	class FestivalLikeSort {

		@Test
		@DisplayName("좋아요 많은 순, 같으면 곧 시작하는 순, 그것도 같으면 id 역순이다")
		void ordersByLikeCountThenBeginDeThenId() {
			Festival many = festival("many", BASE_TIME.plusDays(9), FestivalRegion.values()[0]);
			Festival tieLater = festival("tie-later", BASE_TIME.plusDays(5), FestivalRegion.values()[0]);
			Festival tieSoonFirst = festival("tie-soon-first", BASE_TIME.plusDays(1), FestivalRegion.values()[0]);
			Festival tieSoonSecond = festival("tie-soon-second", BASE_TIME.plusDays(1), FestivalRegion.values()[0]);
			Festival none = festival("none", BASE_TIME, FestivalRegion.values()[0]);
			likeFestival(many, 3);
			for (Festival festival : List.of(tieLater, tieSoonFirst, tieSoonSecond)) {
				likeFestival(festival, 2);
			}
			flushAndClear();

			Page<Festival> page = searchFestivalsByToken(false, null, PageRequest.of(0, 10));

			assertThat(festivalTitles(page)).containsExactly(
					title("many"), title("tie-soon-second"), title("tie-soon-first"),
					title("tie-later"), title("none"));
			assertThat(none.getId()).isNotNull();
		}

		@Test
		@DisplayName("검색 조건은 그대로 적용되고 삭제된 행사는 빠진다")
		void appliesFiltersAndExcludesDeleted() {
			FestivalRegion region = FestivalRegion.values()[0];
			FestivalRegion otherRegion = FestivalRegion.values()[1];
			Festival match = festival("match", BASE_TIME, region);
			Festival otherPlace = festival("other-place", BASE_TIME, otherRegion);
			Festival deleted = festival("deleted", BASE_TIME, region);
			likeFestival(match, 1);
			likeFestival(otherPlace, 3);
			likeFestival(deleted, 4);
			deleted.delete();
			flushAndClear();

			Page<Festival> page = searchFestivalsByToken(true, region, PageRequest.of(0, 10));

			assertThat(festivalTitles(page)).containsExactly(title("match"));
			assertThat(page.getTotalElements()).isEqualTo(1);
		}

		@Test
		@DisplayName("후기에 누른 좋아요는 행사 좋아요 수에 섞이지 않는다")
		void ignoresPostLikes() {
			Festival festival = festival("festival", BASE_TIME, FestivalRegion.values()[0]);
			Post post = persist(Post.builder()
					.member(likers.getFirst()).festival(festival)
					.title(title("post")).content("c").build());
			likeFestival(festival, 1);
			likePost(post, 4);
			flushAndClear();

			Map<Long, Long> counts = likeRepository.countByFestivalIds(List.of(festival.getId())).stream()
					.collect(Collectors.toMap(FestivalLikeCount::festivalId, FestivalLikeCount::likeCount));

			assertThat(counts).containsExactly(Map.entry(festival.getId(), 1L));
		}

		private Page<Festival> searchFestivalsByToken(
				boolean hasRegion, FestivalRegion region, PageRequest pageable) {
			// 서비스는 검색어를 소문자로 바꿔서 넘긴다
			return festivalRepository.searchFestivalsOrderByLikeCount(
					true, token.toLowerCase(),
					hasRegion, region,
					false, null,
					false, null,
					false, null, null,
					false,
					pageable);
		}
	}

	private Festival festival(String name, LocalDateTime beginDe, FestivalRegion region) {
		return persist(Festival.builder()
				.providerType(ProviderType.PUBLIC)
				.title(title(name))
				.beginDe(beginDe)
				.endDe(beginDe.plusDays(3))
				.region(region)
				.build());
	}

	private void likePost(Post post, int count) {
		for (int i = 0; i < count; i++) {
			persist(Like.builder().post(post).member(likers.get(i)).build());
		}
	}

	private void likeFestival(Festival festival, int count) {
		for (int i = 0; i < count; i++) {
			persist(Like.builder().festival(festival).member(likers.get(i)).build());
		}
	}

	private void setCreatedAt(Post post, LocalDateTime createdAt) {
		em.flush();
		em.createNativeQuery("UPDATE post SET created_at = :createdAt WHERE id = :id")
				.setParameter("createdAt", createdAt)
				.setParameter("id", post.getId())
				.executeUpdate();
	}

	private String title(String name) {
		return token + "-" + name;
	}

	private List<String> titles(Page<Post> page) {
		return page.getContent().stream().map(Post::getTitle).toList();
	}

	private List<String> festivalTitles(Page<Festival> page) {
		return page.getContent().stream().map(Festival::getTitle).toList();
	}

	private <T> T persist(T entity) {
		em.persist(entity);
		return entity;
	}

	private void flushAndClear() {
		em.flush();
		em.clear();
	}
}
