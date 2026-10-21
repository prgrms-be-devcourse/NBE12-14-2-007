package com.team007.room_escape.domain.member.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.entity.FestivalAccuracyVote;
import com.team007.room_escape.domain.festival.infra.entity.FestivalAccuracyVoteType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalAccuracyVoteRepository;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.like.infra.entity.Like;
import com.team007.room_escape.domain.like.infra.repository.LikeRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(properties = {
	"spring.datasource.username=roomescape",
	"spring.datasource.password=roomescape",
	"spring.flyway.locations=classpath:db/migration,classpath:db/migration_v2"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({MemberTrustGradeService.class, MemberReader.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MemberTrustGradeConcurrencyTest {
	private static final String SCHEMA = "trust_grade_" + UUID.randomUUID().toString().replace("-", "");
	private static final String DATABASE_URL = System.getenv().getOrDefault("TRUST_TEST_DATABASE_URL",
		"jdbc:postgresql://localhost:55432/roomescape_policy_test");

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry properties) {
		properties.add("spring.datasource.url", () -> DATABASE_URL
			+ (DATABASE_URL.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA + ",public");
		properties.add("spring.flyway.schemas", () -> SCHEMA);
		properties.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
	}

	@Autowired MemberRepository members;
	@Autowired FestivalRepository festivals;
	@Autowired MemberTrustGradeService grades;
	@Autowired MemberReader reader;
	@Autowired PlatformTransactionManager transactionManager;
	@Autowired LikeRepository likes;
	@Autowired FestivalAccuracyVoteRepository votes;

	private TransactionTemplate transactions;
	private UUID authorId;
	private Long festivalId;

	@BeforeEach
	void setUp() {
		transactions = new TransactionTemplate(transactionManager);
		transactions.executeWithoutResult(status -> {
			Member author = members.save(Member.builder().email(UUID.randomUUID() + "@example.com")
				.password("test").nickname("작성자").role(MemberRole.ROLE_TRUSTED).build());
			authorId = author.getId();
			festivalId = festivals.save(Festival.builder().member(author).providerType(ProviderType.MEMBER)
				.title("등급 검증 행사").build()).getId();
		});
	}

	@Test
	void 재계산이_먼저_조회해도_다른_요청에서_완료한_탈퇴를_취소하지_않는다() throws Exception {
		recalculateAfter(Member::delete);

		assertThatThrownBy(() -> transactions.execute(status -> reader.getActiveMember(authorId)))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getExceptionCode()).isEqualTo(MemberExceptionCode.MEMBER_NOT_FOUND));
	}

	@Test
	void 재계산이_먼저_조회해도_다른_요청에서_완료한_제재를_해제하지_않는다() throws Exception {
		recalculateAfter(author -> author.changeRole(MemberRole.ROLE_WARNING));

		MemberRole role = transactions.execute(status -> reader.getActiveMember(authorId).getRole());
		assertThat(role).isEqualTo(MemberRole.ROLE_WARNING);
	}

	@Test
	void 프로필_수정이_나중에_저장되어도_재계산된_등급을_덮어쓰지_않는다() throws Exception {
		inOrder(author -> author.updateProfile("변경한 닉네임", null, null),
			author -> grades.refreshForFestival(festivals.findById(festivalId).orElseThrow()));

		transactions.executeWithoutResult(status -> {
			Member author = reader.getActiveMember(authorId);
			assertThat(author.getNickname()).isEqualTo("변경한 닉네임");
			assertThat(author.getRole()).isEqualTo(MemberRole.ROLE_UNVERIFIED);
		});
	}

	@Test
	void 재계산이_먼저_조회해도_다른_요청의_관리자_역할을_변경하지_않는다() throws Exception {
		recalculateAfter(author -> author.changeRole(MemberRole.ROLE_ADMIN));

		MemberRole role = transactions.execute(status -> reader.getActiveMember(authorId).getRole());
		assertThat(role).isEqualTo(MemberRole.ROLE_ADMIN);
	}

	@Test
	void 재계산이_먼저_완료되어도_오래된_회원_정보로_탈퇴할_수_있다() throws Exception {
		inOrder(Member::delete,
			author -> grades.refreshForFestival(festivals.findById(festivalId).orElseThrow()));

		assertThatThrownBy(() -> transactions.execute(status -> reader.getActiveMember(authorId)))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getExceptionCode()).isEqualTo(MemberExceptionCode.MEMBER_NOT_FOUND));
	}

	@Test
	void 재계산이_먼저_완료되어도_관리자의_제재가_유지된다() throws Exception {
		inOrder(author -> author.changeRole(MemberRole.ROLE_WARNING),
			author -> grades.refreshForFestival(festivals.findById(festivalId).orElseThrow()));

		MemberRole role = transactions.execute(status -> reader.getActiveMember(authorId).getRole());
		assertThat(role).isEqualTo(MemberRole.ROLE_WARNING);
	}

	@ParameterizedTest
	@CsvSource({
		"0, 0, ROLE_UNVERIFIED",
		"9, 9, ROLE_UNVERIFIED",
		"10, 9, ROLE_RECOGNIZED",
		"9, 10, ROLE_RECOGNIZED",
		"10, 10, ROLE_TRUSTED"
	})
	void 기존_등급_기준을_유지하고_같은_요청에서도_최신_등급을_읽는다(
		int likeCount, int accurateCount, MemberRole expected
	) {
		transactions.executeWithoutResult(status -> {
			Festival festival = festivals.findById(festivalId).orElseThrow();
			for (int index = 0; index < Math.max(likeCount, accurateCount); index++) {
				Member voter = members.save(Member.builder().email(UUID.randomUUID() + "@example.com")
					.password("test").nickname("평가자").role(MemberRole.ROLE_UNVERIFIED).build());
				if (index < likeCount) {
					likes.save(Like.builder().festival(festival).member(voter).build());
				}
				if (index < accurateCount) {
					votes.save(FestivalAccuracyVote.builder().festival(festival).member(voter)
						.voteType(FestivalAccuracyVoteType.ACCURATE).build());
				}
			}
			grades.refreshForFestival(festival);
			assertThat(reader.getActiveMember(authorId).getRole()).isEqualTo(expected);
		});
		MemberRole storedRole = transactions.execute(status -> reader.getActiveMember(authorId).getRole());
		assertThat(storedRole).isEqualTo(expected);
	}

	@Test
	void 등급_갱신이_같은_요청의_행사_변경을_분리하거나_누락시키지_않는다() {
		transactions.executeWithoutResult(status -> {
			Festival festival = festivals.findById(festivalId).orElseThrow();
			festival.delete();
			grades.refreshForFestival(festival);
			festival.updateImgUrl("https://images.example.com/after-refresh.png");
		});
		transactions.executeWithoutResult(status -> {
			Festival festival = festivals.findById(festivalId).orElseThrow();
			assertThat(festival.isDeleted()).isTrue();
			assertThat(festival.getImgUrl()).isEqualTo("https://images.example.com/after-refresh.png");
		});
	}

	@Test
	void 요청이_롤백되면_등급_갱신도_함께_롤백된다() {
		assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
			grades.refreshForFestival(festivals.findById(festivalId).orElseThrow());
			throw new IllegalStateException("후속 처리 실패");
		})).isInstanceOf(IllegalStateException.class);

		MemberRole role = transactions.execute(status -> reader.getActiveMember(authorId).getRole());
		assertThat(role).isEqualTo(MemberRole.ROLE_TRUSTED);
	}

	private void recalculateAfter(Consumer<Member> competingChange) throws Exception {
		inOrder(author -> grades.refreshForFestival(festivals.findById(festivalId).orElseThrow()), competingChange);
	}

	private void inOrder(Consumer<Member> delayedChange, Consumer<Member> competingChange) throws Exception {
		CountDownLatch loaded = new CountDownLatch(1);
		CountDownLatch changed = new CountDownLatch(1);
		try (var executor = Executors.newSingleThreadExecutor()) {
			var calculation = executor.submit(() -> transactions.executeWithoutResult(status -> {
				Member author = reader.getActiveMember(authorId); // 다른 요청 전의 회원 상태를 먼저 읽는다.
				loaded.countDown();
				await(changed);
				delayedChange.accept(author);
			}));
			try {
				await(loaded);
				transactions.executeWithoutResult(status -> competingChange.accept(reader.getActiveMember(authorId)));
			} finally {
				changed.countDown();
			}
			calculation.get(15, TimeUnit.SECONDS);
		}
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(10, TimeUnit.SECONDS)) {
				throw new AssertionError("동시 요청의 진행 신호를 받지 못했습니다.");
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new AssertionError(exception);
		}
	}
}
