package com.team007.room_escape.domain.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team007.room_escape.domain.community.dto.CommunityRequest;
import com.team007.room_escape.domain.community.infra.entity.CommunityPost;
import com.team007.room_escape.domain.community.infra.repository.CommunityCommentRepository;
import com.team007.room_escape.domain.community.infra.repository.CommunityPostRepository;
import com.team007.room_escape.domain.community.type.CommunityCategory;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.CommunityExceptionCode;
import com.team007.room_escape.global.util.RichTextSanitizer;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

	@Mock CommunityPostRepository postRepository;
	@Mock CommunityCommentRepository commentRepository;
	@Mock MemberRepository memberRepository;
	@Mock RichTextSanitizer richTextSanitizer;
	@InjectMocks CommunityService communityService;

	@Test
	void 저장하기_전에_에디터_HTML을_정제한다() {
		UUID memberId = UUID.randomUUID();
		when(memberRepository.findByIdAndDeletedAtIsNull(memberId))
			.thenReturn(Optional.of(member(memberId)));
		when(richTextSanitizer.sanitize(any())).thenReturn("<p>안전한 내용</p>");
		when(richTextSanitizer.hasVisibleText(any())).thenReturn(true);
		CommunityRequest.PostUpsert request = new CommunityRequest.PostUpsert(
			CommunityCategory.FREE,
			"자유 이야기",
			"<script>위험</script><p>안전한 내용</p>"
		);

		communityService.createPost(request, memberId);

		ArgumentCaptor<CommunityPost> captor = ArgumentCaptor.forClass(CommunityPost.class);
		verify(postRepository).save(captor.capture());
		assertThat(captor.getValue().getContent()).isEqualTo("<p>안전한 내용</p>");
	}

	@Test
	void 작성자가_아니면_글을_수정할_수_없다() {
		UUID authorId = UUID.randomUUID();
		UUID otherId = UUID.randomUUID();
		when(memberRepository.findByIdAndDeletedAtIsNull(otherId))
			.thenReturn(Optional.of(member(otherId)));
		when(postRepository.findById(any()))
			.thenReturn(Optional.of(CommunityPost.builder().member(member(authorId)).build()));
		CommunityRequest.PostUpsert request = new CommunityRequest.PostUpsert(
			CommunityCategory.FREE,
			"수정할 제목",
			"<p>수정할 내용</p>"
		);

		assertThatThrownBy(() ->
			communityService.updatePost(UUID.randomUUID(), request, otherId))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getExceptionCode())
					.isEqualTo(CommunityExceptionCode.POST_FORBIDDEN));
	}

	@Test
	void 게시글은_30초_안에_연속으로_등록할_수_없다() {
		UUID memberId = UUID.randomUUID();
		when(memberRepository.findByIdAndDeletedAtIsNull(memberId))
			.thenReturn(Optional.of(member(memberId)));
		when(richTextSanitizer.sanitize(any())).thenReturn("<p>새 내용</p>");
		when(richTextSanitizer.hasVisibleText(any())).thenReturn(true);
		when(postRepository.existsByMemberIdAndCreatedAtAfter(any(), any()))
			.thenReturn(true);

		assertThatThrownBy(() -> communityService.createPost(
			new CommunityRequest.PostUpsert(CommunityCategory.FREE, "새 제목", "<p>새 내용</p>"),
			memberId
		)).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.getExceptionCode())
				.isEqualTo(CommunityExceptionCode.POST_CREATE_TOO_SOON));
	}

	@Test
	void 같은_게시글은_1분_안에_다시_등록할_수_없다() {
		UUID memberId = UUID.randomUUID();
		when(memberRepository.findByIdAndDeletedAtIsNull(memberId))
			.thenReturn(Optional.of(member(memberId)));
		when(richTextSanitizer.sanitize(any())).thenReturn("<p>같은 내용</p>");
		when(richTextSanitizer.hasVisibleText(any())).thenReturn(true);
		when(postRepository.existsByMemberIdAndContentAndCreatedAtAfter(
			any(), any(), any()
		)).thenReturn(true);

		assertThatThrownBy(() -> communityService.createPost(
			new CommunityRequest.PostUpsert(CommunityCategory.FREE, "같은 제목", "<p>같은 내용</p>"),
			memberId
		)).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.getExceptionCode())
				.isEqualTo(CommunityExceptionCode.POST_DUPLICATE));
	}

	@Test
	void 댓글은_5초_안에_연속으로_등록할_수_없다() {
		UUID memberId = UUID.randomUUID();
		UUID postId = UUID.randomUUID();
		when(memberRepository.findByIdAndDeletedAtIsNull(memberId))
			.thenReturn(Optional.of(member(memberId)));
		when(postRepository.findById(postId))
			.thenReturn(Optional.of(CommunityPost.builder().member(member(UUID.randomUUID())).build()));
		when(commentRepository.existsByMemberIdAndCreatedAtAfter(any(), any()))
			.thenReturn(true);

		assertThatThrownBy(() -> communityService.createComment(
			postId,
			new CommunityRequest.CommentUpsert("새 댓글"),
			memberId
		)).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.getExceptionCode())
				.isEqualTo(CommunityExceptionCode.COMMENT_CREATE_TOO_SOON));
	}

	@Test
	void 같은_댓글은_1분_안에_다시_등록할_수_없다() {
		UUID memberId = UUID.randomUUID();
		UUID postId = UUID.randomUUID();
		when(memberRepository.findByIdAndDeletedAtIsNull(memberId))
			.thenReturn(Optional.of(member(memberId)));
		when(postRepository.findById(postId))
			.thenReturn(Optional.of(CommunityPost.builder().member(member(UUID.randomUUID())).build()));
		when(commentRepository.existsByMemberIdAndContentAndCreatedAtAfter(
			any(), any(), any()
		)).thenReturn(true);

		assertThatThrownBy(() -> communityService.createComment(
			postId,
			new CommunityRequest.CommentUpsert("같은 댓글"),
			memberId
		)).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.getExceptionCode())
				.isEqualTo(CommunityExceptionCode.COMMENT_DUPLICATE));
	}

	private Member member(UUID id) {
		return Member.builder()
			.id(id)
			.role(MemberRole.ROLE_UNVERIFIED)
			.nickname("이웃")
			.build();
	}
}
