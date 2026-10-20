package com.team007.room_escape.domain.deletion;

import static org.assertj.core.api.Assertions.assertThat;
import com.team007.room_escape.domain.deletion.infra.entity.*;
import com.team007.room_escape.domain.deletion.infra.repository.DeletionHistoryRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest(properties = {"spring.datasource.username=roomescape", "spring.datasource.password=roomescape",
	"spring.flyway.locations=classpath:db/migration,classpath:db/migration_v2", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.context.annotation.Import({
	com.team007.room_escape.domain.deletion.service.AdminDeletionFacade.class,
	com.team007.room_escape.domain.inquiry.service.InquiryDeletionProcessor.class,
	com.team007.room_escape.domain.deletion.service.DeletionHistoryService.class,
	com.team007.room_escape.domain.festival.service.FestivalDeletionHandler.class,
	com.team007.room_escape.domain.post.service.PostDeletionHandler.class,
	com.team007.room_escape.domain.comment.service.CommentDeletionHandler.class,
	com.team007.room_escape.domain.community.service.CommunityPostDeletionHandler.class,
	com.team007.room_escape.domain.community.service.CommunityCommentDeletionHandler.class,
	com.team007.room_escape.domain.festival.service.FestivalDeletionService.class,
	com.team007.room_escape.domain.festival.service.AdminFestivalService.class,
	com.team007.room_escape.domain.festival.service.FestivalSubmissionService.class,
	com.team007.room_escape.domain.festival.service.FestivalDuplicateValidator.class,
	com.team007.room_escape.domain.member.service.MyContentService.class,
	com.team007.room_escape.domain.member.infra.repository.MyContentRepository.class,
	com.team007.room_escape.domain.member.service.MemberReader.class,
	com.team007.room_escape.domain.post.service.PostService.class,
	com.team007.room_escape.domain.comment.service.CommentService.class,
	com.team007.room_escape.domain.community.service.CommunityService.class,
	com.team007.room_escape.domain.member.service.MemberTrustGradeService.class
})
class DeletionHistoryRepositoryTest {
	private static final String SCHEMA = "history_" + UUID.randomUUID().toString().replace("-", "");
	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () ->
			"jdbc:postgresql://localhost:5432/roomescape_policy_test?currentSchema=" + SCHEMA + ",public");
		registry.add("spring.flyway.schemas", () -> SCHEMA);
		registry.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
	}
	@Autowired EntityManager entityManager;
	@Autowired DeletionHistoryRepository histories;
	@Autowired com.team007.room_escape.domain.deletion.service.DeletionHistoryService historyService;
	@Autowired com.team007.room_escape.domain.deletion.service.AdminDeletionFacade facade;
	@Autowired com.team007.room_escape.domain.festival.infra.repository.FestivalRepository festivals;
	@Autowired com.team007.room_escape.domain.festival.service.FestivalDeletionService festivalDeletionService;
	@Autowired com.team007.room_escape.domain.festival.service.AdminFestivalService adminFestivals;
	@Autowired com.team007.room_escape.domain.festival.service.FestivalSubmissionService submissions;
	@Autowired com.team007.room_escape.domain.member.service.MyContentService myContents;
	@Autowired com.team007.room_escape.domain.post.service.PostService postService;
	@Autowired com.team007.room_escape.domain.comment.service.CommentService commentService;
	@Autowired com.team007.room_escape.domain.community.service.CommunityService communityService;
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	com.team007.room_escape.global.util.RichTextSanitizer sanitizer;
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	com.team007.room_escape.global.storage.ImageUrlResolver imageUrlResolver;
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	com.team007.room_escape.global.storage.R2StorageService storageService;

	@Test
	void administratorCommentListKeepsOriginalAndHistoryWhilePublicListRedactsDeletedComments() {
		Member admin = Member.builder().email(UUID.randomUUID() + "@example.com").password("test")
			.nickname("관리자").role(MemberRole.ROLE_ADMIN).build();
		Member author = Member.builder().email(UUID.randomUUID() + "@example.com").password("test")
			.nickname("작성자").role(MemberRole.ROLE_UNVERIFIED).build();
		entityManager.persist(admin); entityManager.persist(author);
		var post = com.team007.room_escape.domain.post.infra.entity.Post.builder()
			.member(author).title("댓글 관리 후기").content("내용").build();
		entityManager.persist(post);
		var comment = com.team007.room_escape.domain.comment.infra.entity.Comment.builder()
			.member(author).post(post).content("삭제 후에도 관리자가 확인할 댓글").build();
		entityManager.persist(comment); entityManager.flush();
		facade.delete(admin.getId(), new com.team007.room_escape.domain.deletion.dto.AdminDeletionRequest.Create(
			DeletionTargetType.COMMENT, comment.getId().toString(), DeletionReason.SPAM, "반복 게시 확인", null));
		entityManager.flush(); entityManager.clear();
		var page = org.springframework.data.domain.PageRequest.of(0, 20);
		var removed = commentService.getCommentsForAdmin(post.getId(), admin.getId(), page).getContent().getFirst();
		assertThat(removed.content()).isEqualTo("삭제 후에도 관리자가 확인할 댓글");
		assertThat(removed.deletedAt()).isNotNull();
		assertThat(removed.deletion().actorType()).isEqualTo(DeletionActorType.ADMIN);
		assertThat(removed.deletion().reasonDetail()).isEqualTo("반복 게시 확인");
		assertThat(removed.parentDeleted()).isFalse();
		var publicComment = commentService.getComments(post.getId(), page).getContent().getFirst();
		assertThat(publicComment.deleted()).isTrue();
		assertThat(publicComment.content()).isEqualTo("삭제된 댓글입니다");
		assertThat(publicComment.member()).isNull();
		facade.restore(admin.getId(), DeletionTargetType.COMMENT, comment.getId().toString());
		entityManager.flush(); entityManager.clear();
		assertThat(commentService.getCommentsForAdmin(post.getId(), admin.getId(), page).getContent().getFirst().deletedAt()).isNull();
		assertThat(commentService.getComments(post.getId(), page)).hasSize(1);
	}

	@Test
	void administratorCommentListRetainsAuthorDeletionAndDeletedParentButRejectsOrdinaryMembers() {
		Member admin = Member.builder().email(UUID.randomUUID() + "@example.com").password("test")
			.nickname("관리자").role(MemberRole.ROLE_ADMIN).build();
		Member author = Member.builder().email(UUID.randomUUID() + "@example.com").password("test")
			.nickname("작성자").role(MemberRole.ROLE_UNVERIFIED).build();
		entityManager.persist(admin); entityManager.persist(author);
		var post = com.team007.room_escape.domain.post.infra.entity.Post.builder()
			.member(author).title("삭제된 부모 후기").content("내용").build();
		entityManager.persist(post);
		var comment = com.team007.room_escape.domain.comment.infra.entity.Comment.builder()
			.member(author).post(post).content("작성자가 삭제한 댓글").build();
		entityManager.persist(comment); entityManager.flush();
		commentService.deleteComment(comment.getId(), author.getId());
		post.delete(); entityManager.flush(); entityManager.clear();
		var page = org.springframework.data.domain.PageRequest.of(0, 20);
		var removed = commentService.getCommentsForAdmin(post.getId(), admin.getId(), page).getContent().getFirst();
		assertThat(removed.parentDeleted()).isTrue();
		assertThat(removed.deletion().actorType()).isEqualTo(DeletionActorType.AUTHOR);
		assertThat(commentService.getCommentsForAdmin(post.getId(), admin.getId(), org.springframework.data.domain.PageRequest.of(1, 20))).isEmpty();
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> commentService.getCommentsForAdmin(post.getId(), author.getId(), page))
			.isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
	}

	@Test
	void adminReplyRestorationPreservesThreadAndIndependentDeletionAfterParentPostRestoration() {
		Member admin = Member.builder().email(UUID.randomUUID() + "@example.com").password("test")
			.nickname("관리자").role(MemberRole.ROLE_ADMIN).build();
		Member author = Member.builder().email(UUID.randomUUID() + "@example.com").password("test")
			.nickname("작성자").role(MemberRole.ROLE_UNVERIFIED).build();
		entityManager.persist(admin); entityManager.persist(author);
		var post = com.team007.room_escape.domain.post.infra.entity.Post.builder()
			.member(author).title("대댓글 복구 검증").content("본문").build();
		entityManager.persist(post);
		var root = com.team007.room_escape.domain.comment.infra.entity.Comment.builder()
			.member(author).post(post).content("부모 댓글").build();
		entityManager.persist(root);
		var reply = com.team007.room_escape.domain.comment.infra.entity.Comment.builder()
			.member(author).post(post).parent(root).content("관리자가 삭제할 답글").build();
		entityManager.persist(reply); entityManager.flush();
		facade.delete(admin.getId(), new com.team007.room_escape.domain.deletion.dto.AdminDeletionRequest.Create(
			DeletionTargetType.COMMENT, reply.getId().toString(), DeletionReason.SPAM, "답글 삭제 사유", null));
		facade.delete(admin.getId(), new com.team007.room_escape.domain.deletion.dto.AdminDeletionRequest.Create(
			DeletionTargetType.POST, post.getId().toString(), DeletionReason.SPAM, null, null));
		facade.restore(admin.getId(), DeletionTargetType.POST, post.getId().toString());
		entityManager.flush(); entityManager.clear();
		var page = org.springframework.data.domain.PageRequest.of(0, 1);
		var adminPage = commentService.getCommentsForAdmin(post.getId(), admin.getId(), page);
		assertThat(adminPage.getTotalElements()).isEqualTo(1);
		var replyInfo = adminPage.getContent().getFirst().replies().getFirst();
		assertThat(replyInfo.parentId()).isEqualTo(root.getId());
		assertThat(replyInfo.deletedAt()).isNotNull();
		assertThat(replyInfo.content()).isEqualTo("관리자가 삭제할 답글");
		assertThat(replyInfo.deletion().reasonDetail()).isEqualTo("답글 삭제 사유");
		var publicReply = commentService.getComments(post.getId(), page).getContent().getFirst().replies().getFirst();
		assertThat(publicReply.content()).isEqualTo("삭제된 댓글입니다");
		assertThat(publicReply.member()).isNull();
		facade.restore(admin.getId(), DeletionTargetType.COMMENT, reply.getId().toString());
		entityManager.flush(); entityManager.clear();
		assertThat(commentService.getComments(post.getId(), page).getContent().getFirst().replies().getFirst().content())
			.isEqualTo("관리자가 삭제할 답글");
	}

	@Test
	void myContentCombinesTypesAndPreservesDeletionWithoutExposingOtherMembers() {
		Member author = Member.builder().email(UUID.randomUUID() + "@example.com").password("test").nickname("작성자").role(MemberRole.ROLE_ADMIN).build();
		Member other = Member.builder().email(UUID.randomUUID() + "@example.com").password("test").nickname("타인").role(MemberRole.ROLE_UNVERIFIED).build();
		entityManager.persist(author); entityManager.persist(other);
		var post = com.team007.room_escape.domain.post.infra.entity.Post.builder().member(author).title("내 후기").content("내용").build();
		var community = com.team007.room_escape.domain.community.infra.entity.CommunityPost.builder().member(author)
			.category(com.team007.room_escape.domain.community.type.CommunityCategory.FREE).title("내 글").content("내용").build();
		entityManager.persist(post); entityManager.persist(community);
		entityManager.persist(com.team007.room_escape.domain.post.infra.entity.Post.builder().member(other).title("타인의 후기").content("내용").build());
		var comment = com.team007.room_escape.domain.comment.infra.entity.Comment.builder().member(author).post(post).content("내 댓글").build();
		var communityComment = com.team007.room_escape.domain.community.infra.entity.CommunityComment.builder().member(author).post(community).content("내 커뮤니티 댓글").build();
		entityManager.persist(comment); entityManager.persist(communityComment);
		var festival = festivals.saveAndFlush(com.team007.room_escape.domain.festival.infra.entity.Festival.builder().member(author)
			.providerType(com.team007.room_escape.domain.festival.infra.entity.ProviderType.MEMBER).title("내 제보").build());
		entityManager.persist(com.team007.room_escape.domain.festival.infra.entity.FestivalSubmission.builder().festival(festival).build());
		entityManager.flush();
		facade.delete(author.getId(), new com.team007.room_escape.domain.deletion.dto.AdminDeletionRequest.Create(DeletionTargetType.POST,
			post.getId().toString(), DeletionReason.SPAM, "마이페이지 사유", null));
		entityManager.flush(); entityManager.clear();
		var posts = myContents.findMine(author.getId(), false, org.springframework.data.domain.PageRequest.of(0, 20));
		assertThat(posts.getTotalElements()).isEqualTo(3);
		assertThat(posts.getContent()).extracting(com.team007.room_escape.domain.member.dto.MyContentResponse.Item::type)
			.containsExactlyInAnyOrder(DeletionTargetType.FESTIVAL, DeletionTargetType.POST, DeletionTargetType.COMMUNITY_POST);
		var removed = posts.getContent().stream().filter(item -> item.type() == DeletionTargetType.POST).findFirst().orElseThrow();
		assertThat(removed.deletedAt()).isNotNull(); assertThat(removed.originalPath()).isNull();
		assertThat(removed.deletion().reasonDetail()).isEqualTo("마이페이지 사유");
		var comments = myContents.findMine(author.getId(), true, org.springframework.data.domain.PageRequest.of(0, 20));
		assertThat(comments.getTotalElements()).isEqualTo(2);
		var orphan = comments.getContent().stream().filter(item -> item.type() == DeletionTargetType.COMMENT).findFirst().orElseThrow();
		assertThat(orphan.parentDeleted()).isTrue(); assertThat(orphan.originalPath()).isNull();
		assertThat(myContents.findMine(author.getId(), false, org.springframework.data.domain.PageRequest.of(1, 2)).getContent()).hasSize(1);
	}

	@Test
	void ownSubmissionListAndDetailReadTypedDeletionReasonFromHistory() {
		Member author = Member.builder().email(UUID.randomUUID() + "@example.com")
			.password("test").nickname("작성자").role(MemberRole.ROLE_UNVERIFIED).build();
		Member admin = Member.builder().email(UUID.randomUUID() + "@example.com")
			.password("test").nickname("관리자").role(MemberRole.ROLE_ADMIN).build();
		entityManager.persist(author);
		entityManager.persist(admin);
		var festival = festivals.saveAndFlush(com.team007.room_escape.domain.festival.infra.entity.Festival.builder()
			.member(author).providerType(com.team007.room_escape.domain.festival.infra.entity.ProviderType.MEMBER).title("제보 이력 조회").build());
		var submission = com.team007.room_escape.domain.festival.infra.entity.FestivalSubmission.builder().festival(festival).build();
		entityManager.persist(submission);
		entityManager.flush();
		facade.delete(admin.getId(), new com.team007.room_escape.domain.deletion.dto.AdminDeletionRequest.Create(
			DeletionTargetType.FESTIVAL, festival.getId().toString(), DeletionReason.SPAM, "이력 상세 설명", null));
		entityManager.flush();
		entityManager.clear();
		assertThat(submissions.findAllByMemberId(author.getId()).getFirst().deletion().reason())
			.contains("도배·스팸", "이력 상세 설명");
		assertThat(submissions.findById(author.getId(), submission.getId()).submission().festival().deletion().reason())
			.contains("도배·스팸", "이력 상세 설명");
	}

	@Test
	void batchHistoryLookupSeparatesTypesAndReturnsLatestActionPerTarget() {
		Member actor = Member.builder().email(UUID.randomUUID() + "@example.com")
			.password("test").nickname("관리자").role(MemberRole.ROLE_ADMIN).build();
		entityManager.persist(actor);
		histories.save(DeletionHistory.record(DeletionTargetType.FESTIVAL, "12", DeletionAction.DELETE,
			actor.getId(), DeletionActorType.ADMIN, DeletionReason.SPAM, null));
		histories.save(DeletionHistory.record(DeletionTargetType.FESTIVAL, "12", DeletionAction.RESTORE,
			actor.getId(), DeletionActorType.ADMIN, DeletionReason.ADMIN_RESTORE, null));
		histories.save(DeletionHistory.record(DeletionTargetType.COMMENT, "12", DeletionAction.DELETE,
			actor.getId(), DeletionActorType.ADMIN, DeletionReason.ABUSIVE_CONTENT, null));
		histories.save(DeletionHistory.record(DeletionTargetType.FESTIVAL, "13", DeletionAction.DELETE,
			actor.getId(), DeletionActorType.AUTHOR, DeletionReason.AUTHOR_REQUEST, null));
		entityManager.flush();
		entityManager.clear();
		var latest = historyService.findLatestForTargets(DeletionTargetType.FESTIVAL, java.util.List.of("12", "13", "14"));
		assertThat(latest).hasSize(2);
		assertThat(latest.get("12").getAction()).isEqualTo(DeletionAction.RESTORE);
		assertThat(latest.get("13").getActorType()).isEqualTo(DeletionActorType.AUTHOR);
		assertThat(historyService.findLatestForTargets(DeletionTargetType.FESTIVAL, java.util.List.of())).isEmpty();
	}

	@Test
	void administratorFestivalListUsesHistoryInsteadOfLegacyDeletionFields() {
		Member admin = Member.builder().email(UUID.randomUUID() + "@example.com")
			.password("test").nickname("관리자").role(MemberRole.ROLE_ADMIN).build();
		entityManager.persist(admin);
		String prefix = "이력전환" + UUID.randomUUID();
		var legacy = festivals.saveAndFlush(com.team007.room_escape.domain.festival.infra.entity.Festival.builder()
			.providerType(com.team007.room_escape.domain.festival.infra.entity.ProviderType.PUBLIC).title(prefix + "과거").build());
		legacy.delete();
		var current = festivals.saveAndFlush(com.team007.room_escape.domain.festival.infra.entity.Festival.builder()
			.providerType(com.team007.room_escape.domain.festival.infra.entity.ProviderType.PUBLIC).title(prefix + "현재").build());
		facade.delete(admin.getId(), new com.team007.room_escape.domain.deletion.dto.AdminDeletionRequest.Create(
			DeletionTargetType.FESTIVAL, current.getId().toString(), DeletionReason.SPAM, "이력의 상세 사유", null));
		// 기존 컬럼의 사유가 아닌 이력의 분류와 상세가 함께 내려와야 한다.
		var page = adminFestivals.search(new com.team007.room_escape.domain.festival.dto.AdminFestivalRequest.Search(
			prefix, null, false, true), org.springframework.data.domain.PageRequest.of(0, 20));
		assertThat(page.getContent()).hasSize(2);
		assertThat(page.getContent().stream().filter(item -> item.festivalId().equals(legacy.getId())).findFirst().orElseThrow().deletion()).isNull();
		var item = page.getContent().stream().filter(value -> value.festivalId().equals(current.getId())).findFirst().orElseThrow();
		assertThat(item.deletion().type()).isEqualTo(DeletionActorType.ADMIN);
		assertThat(item.deletion().reason()).contains("도배·스팸", "이력의 상세 사유");
	}

	@Test
	void administratorReadsLatestHistoryAndCannotMistakeRestorationForDeletion() {
		Member admin = Member.builder().email(UUID.randomUUID() + "@example.com")
			.password("test").nickname("관리자").role(MemberRole.ROLE_ADMIN).build();
		entityManager.persist(admin);
		var festival = festivals.saveAndFlush(com.team007.room_escape.domain.festival.infra.entity.Festival.builder()
			.providerType(com.team007.room_escape.domain.festival.infra.entity.ProviderType.PUBLIC).title("이력 조회").build());
		String id = festival.getId().toString();
		assertThat(facade.latest(admin.getId(), DeletionTargetType.FESTIVAL, id)).isNull();
		facade.delete(admin.getId(), new com.team007.room_escape.domain.deletion.dto.AdminDeletionRequest.Create(
			DeletionTargetType.FESTIVAL, id, DeletionReason.SPAM, "도배 확인", null));
		var deleted = facade.latest(admin.getId(), DeletionTargetType.FESTIVAL, id);
		assertThat(deleted).isNotNull();
		assertThat(deleted.action()).isEqualTo(DeletionAction.DELETE);
		assertThat(deleted.actorType()).isEqualTo(DeletionActorType.ADMIN);
		assertThat(deleted.reasonLabel()).isEqualTo("도배·스팸");
		assertThat(deleted.reasonDetail()).isEqualTo("도배 확인");
		facade.restore(admin.getId(), DeletionTargetType.FESTIVAL, id);
		assertThat(facade.latest(admin.getId(), DeletionTargetType.FESTIVAL, id).action()).isEqualTo(DeletionAction.RESTORE);
	}

	@Test
	void nonAdministratorCannotReadDeletionHistory() {
		Member member = Member.builder().email(UUID.randomUUID() + "@example.com")
			.password("test").nickname("일반회원").role(MemberRole.ROLE_UNVERIFIED).build();
		entityManager.persist(member);
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> facade.latest(member.getId(), DeletionTargetType.POST,
			UUID.randomUUID().toString())).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
	}

	@Test
	void deletionCompletesMatchingPendingReportWithReason() {
		Member admin = Member.builder().email(UUID.randomUUID() + "@example.com")
			.password("test").nickname("관리자").role(MemberRole.ROLE_ADMIN).build();
		entityManager.persist(admin);
		var festival = festivals.saveAndFlush(com.team007.room_escape.domain.festival.infra.entity.Festival.builder()
			.providerType(com.team007.room_escape.domain.festival.infra.entity.ProviderType.PUBLIC).title("신고 대상").build());
		var report = com.team007.room_escape.domain.inquiry.infra.entity.Inquiry.builder()
			.member(admin).category(com.team007.room_escape.domain.inquiry.infra.entity.InquiryCategory.REPORT)
			.targetType(com.team007.room_escape.domain.inquiry.infra.entity.InquiryTargetType.FESTIVAL)
			.targetId(festival.getId().toString()).title("신고").content("잘못된 정보")
			.status(com.team007.room_escape.domain.inquiry.infra.entity.InquiryStatus.PENDING).build();
		entityManager.persist(report);
		entityManager.flush();
		facade.delete(admin.getId(), new com.team007.room_escape.domain.deletion.dto.AdminDeletionRequest.Create(
			DeletionTargetType.FESTIVAL, festival.getId().toString(), DeletionReason.INCORRECT_INFORMATION,
			"행사 장소가 다릅니다", report.getId()));
		entityManager.flush();
		entityManager.clear();
		var reloaded = entityManager.find(com.team007.room_escape.domain.inquiry.infra.entity.Inquiry.class, report.getId());
		assertThat(reloaded.getStatus()).isEqualTo(com.team007.room_escape.domain.inquiry.infra.entity.InquiryStatus.ANSWERED);
		assertThat(reloaded.getAnswer()).contains("삭제", "잘못된 정보", "행사 장소가 다릅니다");
		assertThat(festivals.findByIdAndDeletedAtIsNull(festival.getId())).isEmpty();
		assertThat(histories.findAllByTargetTypeAndTargetIdOrderByIdDesc(DeletionTargetType.FESTIVAL,
			festival.getId().toString())).hasSize(1);
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = {"OTHER_TARGET", "OTHER_TYPE", "ANSWERED", "ANSWER_ONLY", "DELETED", "QUESTION", "TIP", "MISSING"})
	void invalidReportRollsBackDeletionAndHistory(String scenario) {
		Member admin = Member.builder().email(UUID.randomUUID() + "@example.com")
			.password("test").nickname("관리자").role(MemberRole.ROLE_ADMIN).build();
		entityManager.persist(admin);
		var festival = festivals.saveAndFlush(com.team007.room_escape.domain.festival.infra.entity.Festival.builder()
			.providerType(com.team007.room_escape.domain.festival.infra.entity.ProviderType.PUBLIC).title("취소 검증").build());
		var report = com.team007.room_escape.domain.inquiry.infra.entity.Inquiry.builder()
			.member(admin).category(switch (scenario) {
				case "QUESTION" -> com.team007.room_escape.domain.inquiry.infra.entity.InquiryCategory.QUESTION;
				case "TIP" -> com.team007.room_escape.domain.inquiry.infra.entity.InquiryCategory.TIP;
				default -> com.team007.room_escape.domain.inquiry.infra.entity.InquiryCategory.REPORT;
			})
			.targetType("OTHER_TYPE".equals(scenario)
				? com.team007.room_escape.domain.inquiry.infra.entity.InquiryTargetType.COMMENT
				: com.team007.room_escape.domain.inquiry.infra.entity.InquiryTargetType.FESTIVAL)
			.targetId("OTHER_TARGET".equals(scenario) ? "999999999" : festival.getId().toString())
			.title("연결 불가 신고").content("내용")
			.status("ANSWERED".equals(scenario)
				? com.team007.room_escape.domain.inquiry.infra.entity.InquiryStatus.ANSWERED
				: com.team007.room_escape.domain.inquiry.infra.entity.InquiryStatus.PENDING)
			.answer("ANSWER_ONLY".equals(scenario) ? "기존 답변" : null).build();
		if ("DELETED".equals(scenario)) {
			report.delete();
		}
		entityManager.persist(report);
		entityManager.flush();
		UUID adminId = admin.getId();
		Long festivalId = festival.getId();
		UUID reportId = report.getId();
		String originalAnswer = report.getAnswer();
		var originalStatus = report.getStatus();
		// 준비 데이터만 커밋하고, 삭제 요청 실패 후 별도 트랜잭션에서 DB를 다시 확인한다.
		org.springframework.test.context.transaction.TestTransaction.flagForCommit();
		org.springframework.test.context.transaction.TestTransaction.end();
		org.springframework.test.context.transaction.TestTransaction.start();
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> facade.delete(adminId,
			new com.team007.room_escape.domain.deletion.dto.AdminDeletionRequest.Create(
				DeletionTargetType.FESTIVAL, festivalId.toString(), DeletionReason.SPAM, null,
				"MISSING".equals(scenario) ? UUID.randomUUID() : reportId)))
			.isInstanceOf(com.team007.room_escape.global.exception.BusinessException.class);
		org.springframework.test.context.transaction.TestTransaction.end();
		org.springframework.test.context.transaction.TestTransaction.start();
		assertThat(festivals.findByIdAndDeletedAtIsNull(festivalId)).isPresent();
		assertThat(histories.findAllByTargetTypeAndTargetIdOrderByIdDesc(DeletionTargetType.FESTIVAL,
			festivalId.toString())).isEmpty();
		var unchanged = entityManager.find(com.team007.room_escape.domain.inquiry.infra.entity.Inquiry.class, reportId);
		assertThat(unchanged.getAnswer()).isEqualTo(originalAnswer);
		assertThat(unchanged.getStatus()).isEqualTo(originalStatus);
	}

	@Test
	void restoringAdministratorDeletedFestivalKeepsOriginalReason() {
		Member admin = Member.builder().email(UUID.randomUUID() + "@example.com")
			.password("test").nickname("관리자").role(MemberRole.ROLE_ADMIN).build();
		entityManager.persist(admin);
		var festival = festivals.saveAndFlush(com.team007.room_escape.domain.festival.infra.entity.Festival.builder()
			.providerType(com.team007.room_escape.domain.festival.infra.entity.ProviderType.PUBLIC).title("복구 대상").build());
		String id = festival.getId().toString();
		facade.delete(admin.getId(), new com.team007.room_escape.domain.deletion.dto.AdminDeletionRequest.Create(
			DeletionTargetType.FESTIVAL, id, DeletionReason.SPAM, "도배 확인", null));
		facade.restore(admin.getId(), DeletionTargetType.FESTIVAL, id);
		entityManager.flush();
		entityManager.clear();
		assertThat(festivals.findByIdAndDeletedAtIsNull(festival.getId())).isPresent();
		var records = histories.findAllByTargetTypeAndTargetIdOrderByIdDesc(DeletionTargetType.FESTIVAL, id);
		assertThat(records).extracting(DeletionHistory::getAction)
			.containsExactly(DeletionAction.RESTORE, DeletionAction.DELETE);
		assertThat(records.get(1).getReasonDetail()).isEqualTo("도배 확인");
	}

	@Test
	void authorDeletedFestivalCannotBeRestored() {
		assertFestivalRestoreRejected(true, true);
	}

	@Test
	void legacyDeletionWithoutHistoryCannotBeRestored() {
		assertFestivalRestoreRejected(false, true);
	}

	@Test
	void nonAdministratorCannotRestoreFestival() {
		assertFestivalRestoreRejected(false, false);
	}

	private void assertFestivalRestoreRejected(boolean authorDeletion, boolean administrator) {
		Member actor = Member.builder().email(UUID.randomUUID() + "@example.com")
			.password("test").nickname("처리자")
			.role(administrator ? MemberRole.ROLE_ADMIN : MemberRole.ROLE_UNVERIFIED).build();
		entityManager.persist(actor);
		var festival = festivals.saveAndFlush(com.team007.room_escape.domain.festival.infra.entity.Festival.builder()
			.providerType(com.team007.room_escape.domain.festival.infra.entity.ProviderType.PUBLIC).title("복구 거절").build());
		festival.delete();
		String id = festival.getId().toString();
		if (authorDeletion) {
			histories.save(DeletionHistory.record(DeletionTargetType.FESTIVAL, id, DeletionAction.DELETE,
				actor.getId(), DeletionActorType.AUTHOR, DeletionReason.AUTHOR_REQUEST, null));
		}
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> facade.restore(actor.getId(), DeletionTargetType.FESTIVAL, id))
			.isInstanceOf(administrator ? com.team007.room_escape.global.exception.BusinessException.class
				: org.springframework.security.access.AccessDeniedException.class);
		assertThat(festival.isDeleted()).isTrue();
		assertThat(histories.findAllByTargetTypeAndTargetIdOrderByIdDesc(DeletionTargetType.FESTIVAL, id))
			.noneMatch(record -> record.getAction() == DeletionAction.RESTORE);
	}

	@Test
	void authorFestivalDeletionRetainsAuthorHistory() {
		Member author = Member.builder().email(UUID.randomUUID() + "@example.com")
			.password("test").nickname("작성자").role(MemberRole.ROLE_UNVERIFIED).build();
		entityManager.persist(author);
		var festival = festivals.saveAndFlush(com.team007.room_escape.domain.festival.infra.entity.Festival.builder()
			.member(author).providerType(com.team007.room_escape.domain.festival.infra.entity.ProviderType.MEMBER)
			.title("작성자 삭제 대상").build());
		festivalDeletionService.deleteByAuthor(festival, author.getId());
		entityManager.flush();
		entityManager.clear();
		assertThat(festivals.findByIdAndDeletedAtIsNull(festival.getId())).isEmpty();
		assertThat(histories.findAllByTargetTypeAndTargetIdOrderByIdDesc(DeletionTargetType.FESTIVAL, festival.getId().toString()))
			.singleElement().satisfies(record -> {
				assertThat(record.getActorId()).isEqualTo(author.getId());
				assertThat(record.getActorType()).isEqualTo(DeletionActorType.AUTHOR);
				assertThat(record.getReason()).isEqualTo(DeletionReason.AUTHOR_REQUEST);
			});
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.EnumSource(value = DeletionTargetType.class,
		names = {"POST", "COMMENT", "COMMUNITY_POST", "COMMUNITY_COMMENT"})
	void facadeDeletesEachContentAndRecordsAdministratorReason(DeletionTargetType type) {
		deleteContentAndVerifyHistory(type, false);
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.EnumSource(value = DeletionTargetType.class,
		names = {"POST", "COMMENT", "COMMUNITY_POST", "COMMUNITY_COMMENT"})
	void authorDeletesContentAndRetainsAuthorHistory(DeletionTargetType type) {
		deleteContentAndVerifyHistory(type, true);
	}

	private void deleteContentAndVerifyHistory(DeletionTargetType type, boolean authorDeletion) {
		deleteContentAndVerifyHistory(type, authorDeletion, false);
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.EnumSource(value = DeletionTargetType.class,
		names = {"POST", "COMMENT", "COMMUNITY_POST", "COMMUNITY_COMMENT"})
	void administratorCannotDeleteOthersThroughAuthorApi(DeletionTargetType type) {
		deleteContentAndVerifyHistory(type, true, true);
	}

	private void deleteContentAndVerifyHistory(DeletionTargetType type, boolean authorDeletion, boolean unauthorized) {
		deleteContentAndVerifyHistory(type, authorDeletion, unauthorized, false);
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.EnumSource(value = DeletionTargetType.class,
		names = {"POST", "COMMENT", "COMMUNITY_POST", "COMMUNITY_COMMENT"})
	void administratorRestoresContentWithoutLosingDeletionReason(DeletionTargetType type) {
		deleteContentAndVerifyHistory(type, false, false, true);
	}

	private void deleteContentAndVerifyHistory(DeletionTargetType type, boolean authorDeletion, boolean unauthorized, boolean restore) {
		deleteContentAndVerifyHistory(type, authorDeletion, unauthorized, restore, false);
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.EnumSource(value = DeletionTargetType.class,
		names = {"POST", "COMMENT", "COMMUNITY_POST", "COMMUNITY_COMMENT"})
	void authorDeletedContentCannotBeRestored(DeletionTargetType type) {
		deleteContentAndVerifyHistory(type, true, false, true);
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.EnumSource(value = DeletionTargetType.class,
		names = {"COMMENT", "COMMUNITY_COMMENT"})
	void commentCannotBeRestoredWhileParentIsDeleted(DeletionTargetType type) {
		deleteContentAndVerifyHistory(type, false, false, true, true);
	}

	private void deleteContentAndVerifyHistory(DeletionTargetType type, boolean authorDeletion, boolean unauthorized,
		boolean restore, boolean deletedParent) {
		Member author = Member.builder().email(UUID.randomUUID() + "@example.com")
			.password("test").nickname("작성자").role(MemberRole.ROLE_UNVERIFIED).build();
		Member admin = Member.builder().email(UUID.randomUUID() + "@example.com")
			.password("test").nickname("관리자").role(MemberRole.ROLE_ADMIN).build();
		entityManager.persist(author);
		entityManager.persist(admin);
		var post = com.team007.room_escape.domain.post.infra.entity.Post.builder()
			.member(author).title("후기").content("본문").build();
		var communityPost = com.team007.room_escape.domain.community.infra.entity.CommunityPost.builder()
			.member(author).title("커뮤니티").content("본문")
			.category(com.team007.room_escape.domain.community.type.CommunityCategory.FREE).build();
		entityManager.persist(post);
		entityManager.persist(communityPost);
		var comment = com.team007.room_escape.domain.comment.infra.entity.Comment.builder()
			.post(post).member(author).content("후기 댓글").build();
		var communityComment = com.team007.room_escape.domain.community.infra.entity.CommunityComment.builder()
			.post(communityPost).member(author).content("커뮤니티 댓글").build();
		entityManager.persist(comment);
		entityManager.persist(communityComment);
		entityManager.flush();
		String targetId = switch (type) {
			case POST -> post.getId().toString();
			case COMMENT -> comment.getId().toString();
			case COMMUNITY_POST -> communityPost.getId().toString();
			case COMMUNITY_COMMENT -> communityComment.getId().toString();
			default -> throw new IllegalArgumentException();
		};
		Runnable authorDelete = () -> {
			UUID actorId = unauthorized ? admin.getId() : author.getId();
			switch (type) {
				case POST -> postService.deletePost(post.getId(), actorId);
				case COMMENT -> commentService.deleteComment(comment.getId(), actorId);
				case COMMUNITY_POST -> communityService.deletePost(communityPost.getId(), actorId);
				case COMMUNITY_COMMENT -> communityService.deleteComment(communityComment.getId(), actorId);
				default -> throw new IllegalArgumentException();
			}
		};
		if (unauthorized) {
			org.assertj.core.api.Assertions.assertThatThrownBy(authorDelete::run)
				.isInstanceOf(com.team007.room_escape.global.exception.BusinessException.class);
			assertThat(histories.findAllByTargetTypeAndTargetIdOrderByIdDesc(type, targetId)).isEmpty();
			assertThat(post.isDeleted()).isFalse();
			assertThat(comment.isDeleted()).isFalse();
			assertThat(communityPost.isDeleted()).isFalse();
			assertThat(communityComment.isDeleted()).isFalse();
			return;
		}
		if (restore && !authorDeletion && (type == DeletionTargetType.POST || type == DeletionTargetType.COMMUNITY_POST)) {
			facade.delete(admin.getId(), new com.team007.room_escape.domain.deletion.dto.AdminDeletionRequest.Create(
				type == DeletionTargetType.POST ? DeletionTargetType.COMMENT : DeletionTargetType.COMMUNITY_COMMENT,
				type == DeletionTargetType.POST ? comment.getId().toString() : communityComment.getId().toString(),
				DeletionReason.SPAM, null, null));
		}
		if (authorDeletion) {
			authorDelete.run();
		} else {
			facade.delete(admin.getId(), new com.team007.room_escape.domain.deletion.dto.AdminDeletionRequest.Create(
				type, targetId, DeletionReason.ABUSIVE_CONTENT, "욕설 포함", null));
		}
		entityManager.flush();
		entityManager.clear();
		String table = switch (type) {
			case POST -> "post";
			case COMMENT -> "comment";
			case COMMUNITY_POST -> "community_post";
			case COMMUNITY_COMMENT -> "community_comment";
			default -> throw new IllegalArgumentException();
		};
		Object deletedAt = entityManager.createNativeQuery("select deleted_at from " + table + " where cast(id as text) = :id")
			.setParameter("id", targetId).getSingleResult();
		assertThat(deletedAt).isNotNull();
		assertThat(histories.findAllByTargetTypeAndTargetIdOrderByIdDesc(type, targetId))
			.singleElement().satisfies(record -> {
				assertThat(record.getActorId()).isEqualTo(authorDeletion ? author.getId() : admin.getId());
				assertThat(record.getActorType()).isEqualTo(authorDeletion ? DeletionActorType.AUTHOR : DeletionActorType.ADMIN);
				assertThat(record.getReason()).isEqualTo(authorDeletion ? DeletionReason.AUTHOR_REQUEST : DeletionReason.ABUSIVE_CONTENT);
			});
		if (restore) {
			if (deletedParent) {
				facade.delete(admin.getId(), new com.team007.room_escape.domain.deletion.dto.AdminDeletionRequest.Create(
					type == DeletionTargetType.COMMENT ? DeletionTargetType.POST : DeletionTargetType.COMMUNITY_POST,
					type == DeletionTargetType.COMMENT ? post.getId().toString() : communityPost.getId().toString(),
					DeletionReason.SPAM, null, null));
				entityManager.flush();
				entityManager.clear();
			}
			if (authorDeletion || deletedParent) {
				org.assertj.core.api.Assertions.assertThatThrownBy(() -> facade.restore(admin.getId(), type, targetId))
					.isInstanceOf(com.team007.room_escape.global.exception.BusinessException.class);
				assertThat(histories.findAllByTargetTypeAndTargetIdOrderByIdDesc(type, targetId))
					.noneMatch(record -> record.getAction() == DeletionAction.RESTORE);
				return;
			}
			facade.restore(admin.getId(), type, targetId);
			entityManager.flush();
			entityManager.clear();
			assertThat(entityManager.createNativeQuery("select deleted_at from " + table + " where cast(id as text) = :id")
				.setParameter("id", targetId).getSingleResult()).isNull();
			var records = histories.findAllByTargetTypeAndTargetIdOrderByIdDesc(type, targetId);
			assertThat(records).extracting(DeletionHistory::getAction)
				.containsExactly(DeletionAction.RESTORE, DeletionAction.DELETE);
			assertThat(records.get(1).getReasonDetail()).isEqualTo("욕설 포함");
			if (type == DeletionTargetType.POST || type == DeletionTargetType.COMMUNITY_POST) {
				String childTable = type == DeletionTargetType.POST ? "comment" : "community_comment";
				Long childId = type == DeletionTargetType.POST ? comment.getId() : communityComment.getId();
				assertThat(entityManager.createNativeQuery("select deleted_at from " + childTable + " where id = :id")
					.setParameter("id", childId).getSingleResult()).isNotNull();
			}
		}
	}

	@Test
	void facadeDeletesFestivalAndRetainsTypedReasonAfterReload() {
		Member admin = Member.builder().email(UUID.randomUUID() + "@example.com")
			.password("test").nickname("관리자").role(MemberRole.ROLE_ADMIN).build();
		entityManager.persist(admin);
		var festival = festivals.saveAndFlush(com.team007.room_escape.domain.festival.infra.entity.Festival.builder()
			.providerType(com.team007.room_escape.domain.festival.infra.entity.ProviderType.PUBLIC).title("삭제 대상").build());
		facade.delete(admin.getId(), new com.team007.room_escape.domain.deletion.dto.AdminDeletionRequest.Create(
			DeletionTargetType.FESTIVAL, festival.getId().toString(), DeletionReason.SPAM, "도배", null));
		entityManager.flush();
		entityManager.clear();
		assertThat(festivals.findByIdAndDeletedAtIsNull(festival.getId())).isEmpty();
		assertThat(histories.findFirstByTargetTypeAndTargetIdOrderByIdDesc(DeletionTargetType.FESTIVAL, festival.getId().toString()))
			.get().extracting(DeletionHistory::getReason).isEqualTo(DeletionReason.SPAM);
	}

	@Test
	void deletionAndRestorationRemainRetrievableAsSeparateRecords() {
		Member admin = Member.builder().email(UUID.randomUUID() + "@example.com")
			.password("test").nickname("관리자").role(MemberRole.ROLE_ADMIN).build();
		entityManager.persist(admin);
		var deleted = histories.save(DeletionHistory.record(DeletionTargetType.FESTIVAL, "12",
			DeletionAction.DELETE, admin.getId(), DeletionActorType.ADMIN, DeletionReason.SPAM, "도배 삭제"));
		var restored = histories.save(DeletionHistory.record(DeletionTargetType.FESTIVAL, "12",
			DeletionAction.RESTORE, admin.getId(), DeletionActorType.ADMIN, DeletionReason.ADMIN_RESTORE, null));
		entityManager.flush();
		entityManager.clear();
		var records = histories.findAllByTargetTypeAndTargetIdOrderByIdDesc(DeletionTargetType.FESTIVAL, "12");
		assertThat(records).extracting(DeletionHistory::getAction)
			.containsExactly(DeletionAction.RESTORE, DeletionAction.DELETE);
		assertThat(records.get(1).getReasonDetail()).isEqualTo("도배 삭제");
		assertThat(histories.findFirstByTargetTypeAndTargetIdOrderByIdDesc(DeletionTargetType.FESTIVAL, "12"))
			.get().extracting(DeletionHistory::getId).isEqualTo(restored.getId());
		assertThat(deleted.getId()).isNotEqualTo(restored.getId());
	}
}
