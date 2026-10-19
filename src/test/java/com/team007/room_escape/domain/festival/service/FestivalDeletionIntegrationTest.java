package com.team007.room_escape.domain.festival.service;

import static org.assertj.core.api.Assertions.*;
import com.team007.room_escape.domain.festival.infra.entity.*;
import com.team007.room_escape.domain.festival.infra.repository.*;
import com.team007.room_escape.domain.member.infra.entity.*;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.domain.member.service.*;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.util.RichTextSanitizer;
import com.team007.room_escape.domain.festival.dto.FestivalAccuracyVoteRequest;
import com.team007.room_escape.domain.like.infra.entity.Like;
import com.team007.room_escape.domain.like.infra.repository.LikeRepository;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import jakarta.persistence.EntityManager;
import com.team007.room_escape.global.config.R2Properties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest(properties = {
	"spring.datasource.username=roomescape",
	"spring.datasource.password=roomescape",
	"spring.flyway.locations=classpath:db/migration"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({FestivalDeletionService.class, FestivalSubmissionService.class, AdminFestivalService.class,
	MemberReader.class, MemberTrustGradeService.class, RichTextSanitizer.class, FestivalAccuracyVoteService.class})
class FestivalDeletionIntegrationTest {
	private static final String TEST_SCHEMA = "festival_deletion_" + UUID.randomUUID().toString().replace("-", "");

	@DynamicPropertySource
	static void isolatedDatabase(DynamicPropertyRegistry properties) {
		properties.add("spring.datasource.url", () ->
			"jdbc:postgresql://localhost:5432/roomescape?currentSchema=" + TEST_SCHEMA + ",public");
		properties.add("spring.flyway.schemas", () -> TEST_SCHEMA);
		properties.add("spring.jpa.properties.hibernate.default_schema", () -> TEST_SCHEMA);
	}
	@TestConfiguration
	static class StorageConfiguration {
		@Bean
		R2Properties r2Properties() {
			return new R2Properties("test", "test", "test", "test", "https://images.example.com");
		}
	}
	@Autowired FestivalSubmissionService submissions;
	@Autowired AdminFestivalService admin;
	@Autowired FestivalRepository festivals;
	@Autowired FestivalSubmissionRepository histories;
	@Autowired MemberRepository members;
	@Autowired EntityManager em;
	@Autowired FestivalAccuracyVoteService accuracy;
	@Autowired LikeRepository likes;
	@Autowired FestivalAccuracyVoteRepository votes;

	private FestivalSubmission history() {
		Member author = members.save(Member.builder().email(UUID.randomUUID() + "@example.com")
			.password("test").nickname("작성자").role(MemberRole.ROLE_UNVERIFIED).build());
		Festival festival = festivals.save(Festival.builder().member(author).title("행사")
			.providerType(ProviderType.MEMBER).url("https://example.com/" + UUID.randomUUID())
			.region(FestivalRegion.SEOUL).beginDe(LocalDateTime.of(2026, 10, 20, 10, 0))
			.endDe(LocalDateTime.of(2026, 10, 21, 18, 0)).build());
		return histories.saveAndFlush(FestivalSubmission.builder().festival(festival).build());
	}

	@Test
	void authorDeletionSurvivesReloadAndRemainsInOwnHistoryButNotPublicLookup() {
		FestivalSubmission history = history();
		UUID owner = history.getFestival().getMember().getId();
		UUID id = history.getId();
		Long eventId = history.getFestival().getId();
		submissions.delete(owner, id);
		em.clear();
		assertThat(festivals.findByIdAndDeletedAtIsNull(eventId)).isEmpty();
		var item = submissions.findAllByMemberId(owner).getFirst();
		assertThat(item.deletion().type()).isEqualTo(FestivalDeletionType.AUTHOR);
		assertThat(submissions.findById(owner, id).submission().festival().deletion().reason())
			.isEqualTo("작성자 삭제");
		assertThatThrownBy(() -> admin.restore(eventId)).isInstanceOf(BusinessException.class);
		assertThatThrownBy(() -> submissions.findById(UUID.randomUUID(), id)).isInstanceOf(BusinessException.class);
	}

	@Test
	void administratorCanRestoreMemberSubmittedEventAndGradeIsRecalculated() {
		FestivalSubmission history = history();
		Member author = history.getFestival().getMember();
		for (int i = 0; i < 10; i++) {
			Member voter = members.save(Member.builder().email(UUID.randomUUID() + "@example.com")
				.password("test").nickname("평가자").role(MemberRole.ROLE_UNVERIFIED).build());
			likes.save(Like.builder().member(voter).festival(history.getFestival()).build());
			votes.save(FestivalAccuracyVote.builder().member(voter).festival(history.getFestival())
				.voteType(FestivalAccuracyVoteType.ACCURATE).build());
		}
		em.flush();
		UUID operator = UUID.randomUUID();
		Long id = history.getFestival().getId();
		admin.delete(id, operator, "  행사 취소 확인  ");
		assertThat(author.getRole()).isEqualTo(MemberRole.ROLE_UNVERIFIED);
		em.clear();
		assertThat(festivals.findById(id).orElseThrow().getDeletedBy()).isEqualTo(operator);
		admin.restore(id);
		em.flush();
		em.clear();
		assertThat(festivals.findByIdAndDeletedAtIsNull(id)).isPresent();
		assertThat(members.findById(author.getId()).orElseThrow().getRole()).isEqualTo(MemberRole.ROLE_TRUSTED);
	}

	@Test
	void adminDeletionRequiresReasonWithoutChangingEvent() {
		FestivalSubmission history = history();
		assertThatThrownBy(() -> admin.delete(history.getFestival().getId(), UUID.randomUUID(), " "))
			.isInstanceOf(BusinessException.class);
		assertThat(history.getFestival().isDeleted()).isFalse();
	}

	@Test
	void restoreIsRejectedWhenActiveDuplicateExists() {
		FestivalSubmission history = history();
		Festival event = history.getFestival();
		admin.delete(event.getId(), UUID.randomUUID(), "오등록");
		festivals.saveAndFlush(Festival.builder().providerType(ProviderType.MEMBER)
			.url(event.getUrl()).region(event.getRegion())
			.beginDe(event.getBeginDe().plusHours(1)).endDe(event.getEndDe().plusHours(1)).build());
		assertThatThrownBy(() -> admin.restore(event.getId())).isInstanceOf(BusinessException.class);
		assertThat(event.isDeleted()).isTrue();
	}

	@Test
	void deletedEventRejectsAccuracyVoteAndCancellation() {
		FestivalSubmission history = history();
		UUID owner = history.getFestival().getMember().getId();
		Long id = history.getFestival().getId();
		submissions.delete(owner, history.getId());
		assertThatThrownBy(() -> accuracy.vote(owner, id,
			new FestivalAccuracyVoteRequest.Upsert(FestivalAccuracyVoteType.ACCURATE)))
			.isInstanceOfSatisfying(BusinessException.class,
				ex -> assertThat(ex.getExceptionCode()).isEqualTo(FestivalExceptionCode.FESTIVAL_NOT_FOUND));
		assertThatThrownBy(() -> accuracy.cancelVote(owner, id)).isInstanceOf(BusinessException.class);
	}

	@Test
	void publicEventDeletedByAdminCanBeRestored() {
		Festival event = history().getFestival();
		// 공공행사는 작성자가 없어도 삭제·복구할 수 있다.
		Festival publicEvent = festivals.saveAndFlush(Festival.builder().providerType(ProviderType.PUBLIC)
			.beginDe(event.getBeginDe()).endDe(event.getEndDe()).build());
		admin.delete(publicEvent.getId(), UUID.randomUUID(), "중복 수집 확인");
		admin.restore(publicEvent.getId());
		assertThat(publicEvent.isDeleted()).isFalse();
	}

	@Test
	void repeatedDeleteDoesNotOverwriteOriginalReason() {
		Festival event = history().getFestival();
		UUID operator = UUID.randomUUID();
		admin.delete(event.getId(), operator, "최초 사유");
		assertThatThrownBy(() -> admin.delete(event.getId(), operator, "다른 사유"))
			.isInstanceOf(BusinessException.class);
		assertThat(event.getDeletionReason()).isEqualTo("최초 사유");
	}
}
