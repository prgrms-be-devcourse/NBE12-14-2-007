package com.team007.room_escape.domain.comment.service;

import org.springframework.security.access.AccessDeniedException;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import com.team007.room_escape.domain.comment.dto.CommentRequest;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionTargetType;
import com.team007.room_escape.domain.deletion.service.DeletionHistoryService;
import com.team007.room_escape.domain.comment.dto.CommentResponse;
import com.team007.room_escape.domain.comment.infra.entity.Comment;
import com.team007.room_escape.domain.comment.infra.repository.CommentRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.service.MemberReader;
import com.team007.room_escape.domain.post.infra.entity.Post;
import com.team007.room_escape.domain.post.infra.repository.PostRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.CommentExceptionCode;
import com.team007.room_escape.global.response.code.PostExceptionCode;
import com.team007.room_escape.global.storage.ImageUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CommentService {

	private final CommentRepository commentRepository;
	private final MemberReader memberReader;
	private final PostRepository postRepository;
	private final ImageUrlResolver imageUrlResolver;
	private final DeletionHistoryService deletionHistoryService;

	@Transactional(readOnly = true)
	public Page<CommentResponse.AdminInfo> getCommentsForAdmin(UUID postId, UUID adminId, Pageable page) {
		if (memberReader.getActiveMember(adminId).getRole() !=
			MemberRole.ROLE_ADMIN) {
			throw new AccessDeniedException("관리자만 삭제된 댓글을 조회할 수 있습니다.");
		}
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND));
		var roots = commentRepository.findRootsByPostId(postId, PageRequest.of(page.getPageNumber(), page.getPageSize()));
		var replies = roots.isEmpty() ? List.<Comment>of()
			: commentRepository.findRepliesByParentIds(roots.map(Comment::getId).toList());
		var comments = Stream.concat(roots.stream(), replies.stream()).toList();
		var histories = deletionHistoryService.findLatestForTargets(DeletionTargetType.COMMENT,
			comments.stream().filter(Comment::isDeleted).map(comment -> comment.getId().toString()).toList());
		var repliesByParentId = replies.stream().collect(Collectors.groupingBy(reply -> reply.getParent().getId()));
		return roots.map(root -> CommentResponse.AdminInfo.from(root, imageUrlResolver, post.isDeleted(),
			histories.get(root.getId().toString()), repliesByParentId.getOrDefault(root.getId(), List.of()).stream()
				.map(reply -> CommentResponse.AdminInfo.from(reply, imageUrlResolver, post.isDeleted(),
					histories.get(reply.getId().toString()), List.of())).toList()));
	}

	@Transactional
	public CommentResponse.Info createComment(
			UUID postId,
			CommentRequest.Create request,
			UUID memberId
	) {
		Member member = memberReader.getUnrestrictedMember(memberId);

		Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
				.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND));

		Comment comment = Comment.builder()
				.post(post)
				.member(member)
				.parent(findParent(request.parentId(), postId))
				.content(request.content())
				.build();

		commentRepository.save(comment);

		return CommentResponse.Info.from(comment, imageUrlResolver);
	}

	/** 일반 댓글을 오래된 순으로 페이지 나누고, 각 댓글 아래에 대댓글을 붙인다. 삭제된 댓글도 자리를 남긴다. */
	@Transactional(readOnly = true)
	public Page<CommentResponse.Info> getComments(UUID postId, Pageable page) {

		if(!postRepository.existsByIdAndDeletedAtIsNull(postId)) {
			throw new BusinessException(PostExceptionCode.POST_NOT_FOUND);
		}

		// 정렬은 쿼리에 고정돼 있어서 클라이언트가 보낸 sort는 버린다.
		Page<Comment> roots = commentRepository.findRootsByPostId(
				postId, PageRequest.of(page.getPageNumber(), page.getPageSize()));

		Map<Long, List<Comment>> repliesByParentId = roots.isEmpty()
				? Map.of()
				: commentRepository.findRepliesByParentIds(roots.map(Comment::getId).toList()).stream()
						.collect(Collectors.groupingBy(reply -> reply.getParent().getId()));

		return roots.map(root -> CommentResponse.Info.of(
				root,
				repliesByParentId.getOrDefault(root.getId(), List.of()),
				imageUrlResolver
		));
	}

	@Transactional
	public CommentResponse.Info updateComment(
			Long id,
			CommentRequest.Upsert request,
			UUID memberId
	) {
		memberReader.getUnrestrictedMember(memberId);

		Comment comment = getActiveComment(id);

		if(!comment.getMember().getId().equals(memberId)) {
			throw new BusinessException(CommentExceptionCode.COMMENT_FORBIDDEN);
		}

		comment.update(request.content());

		return CommentResponse.Info.from(comment, imageUrlResolver);
	}

	/** 행은 지우지 않고 삭제 시각만 남긴다. 대댓글은 그대로 두고, 목록에는 "삭제된 댓글입니다"로 보인다. */
	@Transactional
	public void deleteComment(Long id, UUID memberId) {
		Comment comment = commentRepository.findForDeletion(id)
				.orElseThrow(() -> new BusinessException(CommentExceptionCode.COMMENT_NOT_FOUND));

		boolean isAuthor = comment.getMember().getId().equals(memberId);

		if(!isAuthor) {
			throw new BusinessException(CommentExceptionCode.COMMENT_FORBIDDEN);
		}

		comment.delete();
		deletionHistoryService.recordAuthorDeletion(DeletionTargetType.COMMENT, id.toString(), memberId);
	}

	/**
	 * 답글을 달 부모 댓글. 대댓글에 답글을 달면 그 대댓글의 부모 아래에 붙여 한 단계를 유지한다.
	 * 삭제된 댓글에는 새 답글을 달 수 없다.
	 */
	private Comment findParent(Long parentId, UUID postId) {
		if (parentId == null) {
			return null;
		}

		Comment parent = getActiveComment(parentId);

		if (!parent.getPost().getId().equals(postId)) {
			throw new BusinessException(CommentExceptionCode.COMMENT_PARENT_MISMATCH);
		}

		return parent.isReply() ? parent.getParent() : parent;
	}

	private Comment getActiveComment(Long id) {
		return commentRepository.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new BusinessException(CommentExceptionCode.COMMENT_NOT_FOUND));
	}
}
