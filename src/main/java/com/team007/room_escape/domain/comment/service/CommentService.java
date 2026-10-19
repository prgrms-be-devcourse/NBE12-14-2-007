package com.team007.room_escape.domain.comment.service;

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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

	private final CommentRepository commentRepository;
	private final MemberReader memberReader;
	private final PostRepository postRepository;
	private final ImageUrlResolver imageUrlResolver;
	private final DeletionHistoryService deletionHistoryService;

	@Transactional(readOnly = true)
	public List<CommentResponse.AdminInfo> getCommentsForAdmin(UUID postId, UUID adminId, Pageable page) {
		if (memberReader.getActiveMember(adminId).getRole() !=
			com.team007.room_escape.domain.member.infra.entity.MemberRole.ROLE_ADMIN) {
			throw new org.springframework.security.access.AccessDeniedException("관리자만 삭제된 댓글을 조회할 수 있습니다.");
		}
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND));
		var comments = commentRepository.findAllForAdmin(postId, page.getPageSize(), page.getOffset());
		var histories = deletionHistoryService.findLatestForTargets(DeletionTargetType.COMMENT,
			comments.stream().filter(Comment::isDeleted).map(comment -> comment.getId().toString()).toList());
		return comments.stream().map(comment -> CommentResponse.AdminInfo.from(comment, imageUrlResolver,
			post.isDeleted(), histories.get(comment.getId().toString()))).toList();
	}

	@Transactional
	public CommentResponse.Info createComment(
			UUID postId,
			CommentRequest.Upsert request,
			UUID memberId
	) {
		Member member = memberReader.getUnrestrictedMember(memberId);

		Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
				.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND));

		Comment comment = Comment.builder()
				.post(post)
				.member(member)
				.content(request.content())
				.build();

		commentRepository.save(comment);

		return CommentResponse.Info.from(comment, imageUrlResolver);
	}

	@Transactional(readOnly = true)
	public List<CommentResponse.Info> getComments(UUID postId, Pageable page) {

		if(!postRepository.existsByIdAndDeletedAtIsNull(postId)) {
			throw new BusinessException(PostExceptionCode.POST_NOT_FOUND);
		}

		return commentRepository.findAllByPostId(postId, page).stream()
				.map(comment -> CommentResponse.Info.from(comment, imageUrlResolver))
				.toList();
	}

	@Transactional
	public CommentResponse.Info updateComment(
			Long id,
			CommentRequest.Upsert request,
			UUID memberId
	) {
		memberReader.getUnrestrictedMember(memberId);

		Comment comment = commentRepository.findById(id)
				.orElseThrow(() -> new BusinessException(CommentExceptionCode.COMMENT_NOT_FOUND));

		if(!comment.getMember().getId().equals(memberId)) {
			throw new BusinessException(CommentExceptionCode.COMMENT_FORBIDDEN);
		}

		comment.update(request.content());

		return CommentResponse.Info.from(comment, imageUrlResolver);
	}

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
}
