package com.team007.room_escape.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

import com.team007.room_escape.domain.comment.dto.CommentRequest;
import com.team007.room_escape.domain.comment.dto.CommentResponse;
import com.team007.room_escape.domain.inquiry.service.InquiryTargetValidator;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryTargetType;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.domain.post.infra.entity.Post;
import com.team007.room_escape.domain.post.infra.repository.PostRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.CommentExceptionCode;
import com.team007.room_escape.global.response.code.InquiryExceptionCode;

import jakarta.persistence.EntityManager;

/** 후기 댓글의 정렬, 대댓글, soft delete를 실제 DB로 확인한다. */
@SpringBootTest
@Transactional
class CommentServiceTest {

	@Autowired
	private CommentService commentService;
	@Autowired
	private InquiryTargetValidator inquiryTargetValidator;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private PostRepository postRepository;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private com.team007.room_escape.domain.deletion.service.AdminDeletionFacade adminDeletions;

	private final PageRequest firstPage = PageRequest.of(0, 20);

	private Member writer;
	private Member other;
	private UUID postId;

	@BeforeEach
	void setUp() {
		writer = memberRepository.save(member());
		other = memberRepository.save(member());
		postId = postRepository.save(post(writer)).getId();
	}

	@Test
	void 일반_댓글은_오래된_순이고_대댓글은_부모_아래에_오래된_순으로_붙는다() {
		Long first = write("첫 댓글");
		Long second = write("둘째 댓글");
		Long reply1 = reply(first, "첫 답글");
		Long reply2 = reply(first, "둘째 답글");

		Page<CommentResponse.Info> page = commentService.getComments(postId, firstPage);

		assertThat(page.getContent()).extracting(CommentResponse.Info::id).containsExactly(first, second);
		assertThat(page.getContent().get(0).replies())
			.extracting(CommentResponse.Info::id).containsExactly(reply1, reply2);
		assertThat(page.getContent().get(0).replies())
			.allSatisfy(reply -> assertThat(reply.parentId()).isEqualTo(first));
		assertThat(page.getContent().get(1).replies()).isEmpty();
	}

	@Test
	void 클라이언트가_최신순을_요청해도_오래된_순으로_내려준다() {
		Long first = write("첫 댓글");
		Long second = write("둘째 댓글");

		Page<CommentResponse.Info> page = commentService.getComments(
			postId, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

		assertThat(page.getContent()).extracting(CommentResponse.Info::id).containsExactly(first, second);
	}

	@Test
	void 페이지는_일반_댓글_기준으로_나눈다() {
		for (int i = 0; i < 21; i++) {
			Long root = write("댓글 " + i);
			reply(root, "답글 " + i);
		}

		Page<CommentResponse.Info> page = commentService.getComments(postId, firstPage);

		assertThat(page.getTotalElements()).isEqualTo(21);
		assertThat(page.getTotalPages()).isEqualTo(2);
		assertThat(page.getContent()).hasSize(20)
			.allSatisfy(root -> assertThat(root.replies()).hasSize(1));
	}

	@Test
	void 삭제해도_행은_남고_삭제_시각만_기록된다() {
		Long id = write("지울 댓글");

		commentService.deleteComment(id, writer.getId());
		entityManager.flush();

		var row = jdbcTemplate.queryForMap("SELECT content, deleted_at FROM \"comment\" WHERE id = ?", id);
		assertThat(row.get("content")).isEqualTo("지울 댓글");
		assertThat(row.get("deleted_at")).isNotNull();
	}

	@Test
	void 대댓글이_없는_댓글도_삭제하면_문구로_자리가_남는다() {
		Long id = write("지울 댓글");

		commentService.deleteComment(id, writer.getId());
		CommentResponse.Info deleted = commentService.getComments(postId, firstPage).getContent().get(0);

		assertThat(deleted.id()).isEqualTo(id);
		assertThat(deleted.deleted()).isTrue();
		assertThat(deleted.content()).isEqualTo(CommentResponse.DELETED_CONTENT);
		assertThat(deleted.member()).isNull();
	}

	@Test
	void 부모_댓글을_지워도_대댓글은_그대로_남는다() {
		Long root = write("부모 댓글");
		Long reply = reply(root, "남아야 하는 답글");

		commentService.deleteComment(root, writer.getId());
		CommentResponse.Info deletedRoot = commentService.getComments(postId, firstPage).getContent().get(0);

		assertThat(deletedRoot.deleted()).isTrue();
		assertThat(deletedRoot.content()).isEqualTo(CommentResponse.DELETED_CONTENT);
		assertThat(deletedRoot.replies()).singleElement().satisfies(r -> {
			assertThat(r.id()).isEqualTo(reply);
			assertThat(r.deleted()).isFalse();
			assertThat(r.content()).isEqualTo("남아야 하는 답글");
			assertThat(r.member().nickname()).isEqualTo(other.getNickname());
		});
	}

	@Test
	void 삭제된_대댓글도_문구로_자리가_남는다() {
		Long root = write("부모 댓글");
		Long reply = reply(root, "지울 답글");

		commentService.deleteComment(reply, other.getId());
		CommentResponse.Info rootInfo = commentService.getComments(postId, firstPage).getContent().get(0);

		assertThat(rootInfo.deleted()).isFalse();
		assertThat(rootInfo.replies()).singleElement().satisfies(r -> {
			assertThat(r.deleted()).isTrue();
			assertThat(r.content()).isEqualTo(CommentResponse.DELETED_CONTENT);
			assertThat(r.member()).isNull();
		});
	}

	@Test
	void 삭제된_댓글은_수정할_수_없다() {
		Long id = write("지울 댓글");
		commentService.deleteComment(id, writer.getId());

		assertThatThrownBy(() -> commentService.updateComment(id, new CommentRequest.Upsert("되살리기"), writer.getId()))
			.isInstanceOfSatisfying(BusinessException.class, e ->
				assertThat(e.getExceptionCode()).isEqualTo(CommentExceptionCode.COMMENT_NOT_FOUND));
	}

	@Test
	void 이미_삭제된_댓글은_다시_삭제할_수_없다() {
		Long id = write("지울 댓글");
		commentService.deleteComment(id, writer.getId());

		assertThatThrownBy(() -> commentService.deleteComment(id, writer.getId()))
			.isInstanceOfSatisfying(BusinessException.class, e ->
				assertThat(e.getExceptionCode()).isEqualTo(CommentExceptionCode.COMMENT_NOT_FOUND));
	}

	@Test
	void 삭제된_댓글에는_답글을_달_수_없다() {
		Long id = write("지울 댓글");
		commentService.deleteComment(id, writer.getId());

		assertThatThrownBy(() -> reply(id, "늦은 답글"))
			.isInstanceOfSatisfying(BusinessException.class, e ->
				assertThat(e.getExceptionCode()).isEqualTo(CommentExceptionCode.COMMENT_NOT_FOUND));
	}

	@Test
	void 삭제된_댓글은_신고할_수_없다() {
		Long id = write("지울 댓글");
		commentService.deleteComment(id, writer.getId());

		assertThatThrownBy(() -> inquiryTargetValidator.validate(InquiryTargetType.COMMENT, String.valueOf(id)))
			.isInstanceOfSatisfying(BusinessException.class, e ->
				assertThat(e.getExceptionCode()).isEqualTo(InquiryExceptionCode.INQUIRY_TARGET_NOT_FOUND));
	}

	@Test
	void 대댓글에_단_답글은_그_대댓글의_부모_아래에_붙는다() {
		Long root = write("부모 댓글");
		Long reply = reply(root, "답글");

		CommentResponse.Info replyOfReply = commentService.createComment(
			postId, new CommentRequest.Create("답글의 답글", reply), writer.getId());

		assertThat(replyOfReply.parentId()).isEqualTo(root);
		assertThat(commentService.getComments(postId, firstPage).getContent().get(0).replies())
			.extracting(CommentResponse.Info::id).containsExactly(reply, replyOfReply.id());
	}

	@Test
	void 다른_후기의_댓글에는_답글을_달_수_없다() {
		UUID otherPostId = postRepository.save(post(writer)).getId();
		Long otherPostComment = commentService.createComment(
			otherPostId, new CommentRequest.Create("다른 후기 댓글", null), writer.getId()).id();

		assertThatThrownBy(() -> reply(otherPostComment, "엉뚱한 답글"))
			.isInstanceOfSatisfying(BusinessException.class, e ->
				assertThat(e.getExceptionCode()).isEqualTo(CommentExceptionCode.COMMENT_PARENT_MISMATCH));
	}

	@Test
	void 남의_댓글은_삭제할_수_없고_관리자는_삭제할_수_있다() {
		Long id = write("남의 댓글");

		assertThatThrownBy(() -> commentService.deleteComment(id, other.getId()))
			.isInstanceOfSatisfying(BusinessException.class, e ->
				assertThat(e.getExceptionCode()).isEqualTo(CommentExceptionCode.COMMENT_FORBIDDEN));

		Member admin = memberRepository.save(Member.builder().email(UUID.randomUUID() + "@test.com")
			.password("encoded").nickname("관리자").role(MemberRole.ROLE_ADMIN).build());
		adminDeletions.delete(admin.getId(), new com.team007.room_escape.domain.deletion.dto.AdminDeletionRequest.Create(
			com.team007.room_escape.domain.deletion.infra.entity.DeletionTargetType.COMMENT, id.toString(),
			com.team007.room_escape.domain.deletion.infra.entity.DeletionReason.SPAM, null, null));
		assertThat(commentService.getComments(postId, firstPage).getContent().get(0).deleted()).isTrue();
	}

	private Long write(String content) {
		return commentService.createComment(postId, new CommentRequest.Create(content, null), writer.getId()).id();
	}

	private Long reply(Long parentId, String content) {
		return commentService.createComment(postId, new CommentRequest.Create(content, parentId), other.getId()).id();
	}

	private Member member() {
		String nickname = "댓글" + UUID.randomUUID().toString().substring(0, 8);
		return Member.builder()
			.email(nickname + "@test.com")
			.password("encoded")
			.role(MemberRole.ROLE_UNVERIFIED)
			.nickname(nickname)
			.build();
	}

	private Post post(Member member) {
		return Post.builder()
			.member(member)
			.title("댓글 테스트 후기")
			.content("본문")
			.build();
	}
}
