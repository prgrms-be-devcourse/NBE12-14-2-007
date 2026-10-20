package com.team007.room_escape.domain.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.team007.room_escape.domain.community.dto.CommunityRequest;
import com.team007.room_escape.domain.community.dto.CommunityResponse;
import com.team007.room_escape.domain.community.infra.entity.CommunityPost;
import com.team007.room_escape.domain.community.infra.repository.CommunityPostRepository;
import com.team007.room_escape.domain.community.type.CommunityCategory;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryTargetType;
import com.team007.room_escape.domain.inquiry.service.InquiryTargetValidator;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.CommunityExceptionCode;
import com.team007.room_escape.global.response.code.InquiryExceptionCode;

import jakarta.persistence.EntityManager;

/**
 * 커뮤니티 댓글의 정렬, 대댓글, soft delete를 실제 DB로 확인한다.
 * 같은 회원은 5초 안에 댓글을 다시 쓸 수 없어서 댓글마다 새 회원으로 쓴다.
 */
@SpringBootTest
@Transactional
class CommunityCommentTest {

	@Autowired
	private CommunityService communityService;
	@Autowired
	private InquiryTargetValidator inquiryTargetValidator;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private CommunityPostRepository postRepository;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private com.team007.room_escape.domain.deletion.service.AdminDeletionFacade adminDeletions;

	private final PageRequest firstPage = PageRequest.of(0, 20);

	private UUID postId;

	@BeforeEach
	void setUp() {
		postId = postRepository.save(post(newMember())).getId();
	}

	@Test
	void 일반_댓글은_오래된_순이고_대댓글은_부모_아래에_오래된_순으로_붙는다() {
		Long first = write("첫 댓글").id();
		Long second = write("둘째 댓글").id();
		Long reply1 = reply(first, "첫 답글").id();
		Long reply2 = reply(first, "둘째 답글").id();

		Page<CommunityResponse.CommentInfo> page = communityService.getComments(postId, firstPage);

		assertThat(page.getContent()).extracting(CommunityResponse.CommentInfo::id).containsExactly(first, second);
		assertThat(page.getContent().get(0).replies())
			.extracting(CommunityResponse.CommentInfo::id).containsExactly(reply1, reply2);
		assertThat(page.getContent().get(0).replies())
			.allSatisfy(reply -> assertThat(reply.parentId()).isEqualTo(first));
		assertThat(page.getContent().get(1).replies()).isEmpty();
	}

	@Test
	void 클라이언트가_최신순을_요청해도_오래된_순으로_내려준다() {
		Long first = write("첫 댓글").id();
		Long second = write("둘째 댓글").id();

		Page<CommunityResponse.CommentInfo> page = communityService.getComments(
			postId, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

		assertThat(page.getContent()).extracting(CommunityResponse.CommentInfo::id).containsExactly(first, second);
	}

	@Test
	void 페이지는_일반_댓글_기준으로_나눈다() {
		for (int i = 0; i < 21; i++) {
			Long root = write("댓글 " + i).id();
			reply(root, "답글 " + i);
		}

		Page<CommunityResponse.CommentInfo> page = communityService.getComments(postId, firstPage);

		assertThat(page.getTotalElements()).isEqualTo(21);
		assertThat(page.getTotalPages()).isEqualTo(2);
		assertThat(page.getContent()).hasSize(20)
			.allSatisfy(root -> assertThat(root.replies()).hasSize(1));
	}

	@Test
	void 삭제해도_행은_남고_삭제_시각만_기록된다() {
		Written comment = write("지울 댓글");

		communityService.deleteComment(comment.id(), comment.writer().getId());
		entityManager.flush();

		Map<String, Object> row = jdbcTemplate.queryForMap(
			"SELECT content, deleted_at FROM community_comment WHERE id = ?", comment.id());
		assertThat(row.get("content")).isEqualTo("지울 댓글");
		assertThat(row.get("deleted_at")).isNotNull();
	}

	@Test
	void 대댓글이_없는_댓글도_삭제하면_문구로_자리가_남는다() {
		Written comment = write("지울 댓글");

		communityService.deleteComment(comment.id(), comment.writer().getId());
		CommunityResponse.CommentInfo deleted = communityService.getComments(postId, firstPage).getContent().get(0);

		assertThat(deleted.id()).isEqualTo(comment.id());
		assertThat(deleted.deleted()).isTrue();
		assertThat(deleted.content()).isEqualTo(CommunityResponse.DELETED_COMMENT_CONTENT);
		assertThat(deleted.member()).isNull();
	}

	@Test
	void 부모_댓글을_지워도_대댓글은_그대로_남는다() {
		Written root = write("부모 댓글");
		Written reply = reply(root.id(), "남아야 하는 답글");

		communityService.deleteComment(root.id(), root.writer().getId());
		CommunityResponse.CommentInfo deletedRoot = communityService.getComments(postId, firstPage).getContent().get(0);

		assertThat(deletedRoot.deleted()).isTrue();
		assertThat(deletedRoot.content()).isEqualTo(CommunityResponse.DELETED_COMMENT_CONTENT);
		assertThat(deletedRoot.replies()).singleElement().satisfies(r -> {
			assertThat(r.id()).isEqualTo(reply.id());
			assertThat(r.deleted()).isFalse();
			assertThat(r.content()).isEqualTo("남아야 하는 답글");
			assertThat(r.member().nickname()).isEqualTo(reply.writer().getNickname());
		});
	}

	@Test
	void 삭제된_대댓글도_문구로_자리가_남는다() {
		Written root = write("부모 댓글");
		Written reply = reply(root.id(), "지울 답글");

		communityService.deleteComment(reply.id(), reply.writer().getId());
		CommunityResponse.CommentInfo rootInfo = communityService.getComments(postId, firstPage).getContent().get(0);

		assertThat(rootInfo.deleted()).isFalse();
		assertThat(rootInfo.replies()).singleElement().satisfies(r -> {
			assertThat(r.deleted()).isTrue();
			assertThat(r.content()).isEqualTo(CommunityResponse.DELETED_COMMENT_CONTENT);
			assertThat(r.member()).isNull();
		});
	}

	@Test
	void 댓글_수는_대댓글을_포함하고_삭제된_댓글은_뺀다() {
		Written root = write("부모 댓글");
		reply(root.id(), "답글 1");
		reply(root.id(), "답글 2");
		write("다른 댓글");

		communityService.deleteComment(root.id(), root.writer().getId());

		assertThat(communityService.getPost(postId).commentCount()).isEqualTo(3);
		assertThat(communityService.getPosts(null, "", PageRequest.of(0, 50)).getContent())
			.filteredOn(item -> item.id().equals(postId))
			.singleElement()
			.satisfies(item -> assertThat(item.commentCount()).isEqualTo(3));
	}

	@Test
	void 삭제된_댓글은_수정할_수_없다() {
		Written comment = write("지울 댓글");
		communityService.deleteComment(comment.id(), comment.writer().getId());

		assertThatThrownBy(() -> communityService.updateComment(
			comment.id(), new CommunityRequest.CommentUpsert("되살리기"), comment.writer().getId()))
			.isInstanceOfSatisfying(BusinessException.class, e ->
				assertThat(e.getExceptionCode()).isEqualTo(CommunityExceptionCode.COMMENT_NOT_FOUND));
	}

	@Test
	void 이미_삭제된_댓글은_다시_삭제할_수_없다() {
		Written comment = write("지울 댓글");
		communityService.deleteComment(comment.id(), comment.writer().getId());

		assertThatThrownBy(() -> communityService.deleteComment(comment.id(), comment.writer().getId()))
			.isInstanceOfSatisfying(BusinessException.class, e ->
				assertThat(e.getExceptionCode()).isEqualTo(CommunityExceptionCode.COMMENT_NOT_FOUND));
	}

	@Test
	void 삭제된_댓글에는_답글을_달_수_없다() {
		Written comment = write("지울 댓글");
		communityService.deleteComment(comment.id(), comment.writer().getId());

		assertThatThrownBy(() -> reply(comment.id(), "늦은 답글"))
			.isInstanceOfSatisfying(BusinessException.class, e ->
				assertThat(e.getExceptionCode()).isEqualTo(CommunityExceptionCode.COMMENT_NOT_FOUND));
	}

	@Test
	void 삭제된_댓글은_신고할_수_없다() {
		Written comment = write("지울 댓글");
		communityService.deleteComment(comment.id(), comment.writer().getId());

		assertThatThrownBy(() -> inquiryTargetValidator.validate(
			InquiryTargetType.COMMUNITY_COMMENT, String.valueOf(comment.id())))
			.isInstanceOfSatisfying(BusinessException.class, e ->
				assertThat(e.getExceptionCode()).isEqualTo(InquiryExceptionCode.INQUIRY_TARGET_NOT_FOUND));
	}

	@Test
	void 삭제한_댓글은_도배_확인에서_빠져_바로_다시_쓸_수_있다() {
		Written comment = write("같은 내용");
		communityService.deleteComment(comment.id(), comment.writer().getId());

		CommunityResponse.CommentInfo again = communityService.createComment(
			postId, new CommunityRequest.CommentCreate("같은 내용", null), comment.writer().getId());

		assertThat(again.deleted()).isFalse();
	}

	@Test
	void 대댓글에_단_답글은_그_대댓글의_부모_아래에_붙는다() {
		Long root = write("부모 댓글").id();
		Long reply = reply(root, "답글").id();

		Long replyOfReply = reply(reply, "답글의 답글").id();

		assertThat(communityService.getComments(postId, firstPage).getContent().get(0).replies())
			.extracting(CommunityResponse.CommentInfo::id).containsExactly(reply, replyOfReply);
		assertThat(communityService.getComments(postId, firstPage).getContent().get(0).replies())
			.allSatisfy(r -> assertThat(r.parentId()).isEqualTo(root));
	}

	@Test
	void 다른_글의_댓글에는_답글을_달_수_없다() {
		UUID otherPostId = postRepository.save(post(newMember())).getId();
		Long otherPostComment = communityService.createComment(
			otherPostId, new CommunityRequest.CommentCreate("다른 글 댓글", null), newMember().getId()).id();

		assertThatThrownBy(() -> reply(otherPostComment, "엉뚱한 답글"))
			.isInstanceOfSatisfying(BusinessException.class, e ->
				assertThat(e.getExceptionCode()).isEqualTo(CommunityExceptionCode.COMMENT_PARENT_MISMATCH));
	}

	@Test
	void 남의_댓글은_삭제할_수_없고_관리자는_삭제할_수_있다() {
		Written comment = write("남의 댓글");
		UUID otherId = newMember().getId();

		assertThatThrownBy(() -> communityService.deleteComment(comment.id(), otherId))
			.isInstanceOfSatisfying(BusinessException.class, e ->
				assertThat(e.getExceptionCode()).isEqualTo(CommunityExceptionCode.COMMENT_FORBIDDEN));

		Member admin = memberRepository.save(Member.builder().email(UUID.randomUUID() + "@test.com")
			.password("encoded").nickname("관리자").role(MemberRole.ROLE_ADMIN).build());
		adminDeletions.delete(admin.getId(), new com.team007.room_escape.domain.deletion.dto.AdminDeletionRequest.Create(
			com.team007.room_escape.domain.deletion.infra.entity.DeletionTargetType.COMMUNITY_COMMENT, comment.id().toString(),
			com.team007.room_escape.domain.deletion.infra.entity.DeletionReason.SPAM, null, null));
		assertThat(communityService.getComments(postId, firstPage).getContent().get(0).deleted()).isTrue();
	}

	private record Written(Long id, Member writer) {
	}

	private Written write(String content) {
		Member writer = newMember();
		Long id = communityService.createComment(
			postId, new CommunityRequest.CommentCreate(content, null), writer.getId()).id();
		return new Written(id, writer);
	}

	private Written reply(Long parentId, String content) {
		Member writer = newMember();
		Long id = communityService.createComment(
			postId, new CommunityRequest.CommentCreate(content, parentId), writer.getId()).id();
		return new Written(id, writer);
	}

	private Member newMember() {
		String nickname = "커뮤" + UUID.randomUUID().toString().substring(0, 8);
		return memberRepository.save(Member.builder()
			.email(nickname + "@test.com")
			.password("encoded")
			.role(MemberRole.ROLE_UNVERIFIED)
			.nickname(nickname)
			.build());
	}

	private CommunityPost post(Member member) {
		return CommunityPost.builder()
			.member(member)
			.category(CommunityCategory.FREE)
			.title("댓글 테스트 글")
			.content("<p>본문</p>")
			.build();
	}
}
