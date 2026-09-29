package com.team007.room_escape.domain.comment.service;

import com.team007.room_escape.domain.comment.infra.dto.CommentRequest;
import com.team007.room_escape.domain.comment.infra.dto.CommentResponse;
import com.team007.room_escape.domain.comment.infra.entity.Comment;
import com.team007.room_escape.domain.comment.infra.repository.CommentRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.domain.post.infra.entity.Post;
import com.team007.room_escape.domain.post.infra.repository.PostRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.CommentExceptionCode;
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import com.team007.room_escape.global.response.code.PostExceptionCode;
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
	private final MemberRepository memberRepository;
	private final PostRepository postRepository;

	@Transactional
	public CommentResponse.CommentInfo createComment(
			UUID postId,
			CommentRequest request,
			UUID memberId
	) {
		Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
				.orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));

		if(member.isRestricted()) {
			throw new BusinessException(MemberExceptionCode.MEMBER_RESTRICTED);
		}

		Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
				.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND));

		Comment comment = Comment.builder()
				.post(post)
				.member(member)
				.content(request.content())
				.build();

		commentRepository.save(comment);

		return CommentResponse.CommentInfo.from(comment);
	}

	@Transactional(readOnly = true)
	public List<CommentResponse.CommentInfo> getComments(UUID postId, Pageable page) {

		return commentRepository.findAllByPostId(postId, page).stream()
				.map(CommentResponse.CommentInfo::from)
				.toList();
	}

	@Transactional
	public CommentResponse.CommentInfo updateComment(
			Long id,
			CommentRequest request,
			UUID memberId
	) {
		Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
				.orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));

		if(member.isRestricted()) {
			throw new BusinessException(MemberExceptionCode.MEMBER_RESTRICTED);
		}

		Comment comment = commentRepository.findById(id)
				.orElseThrow(() -> new BusinessException(CommentExceptionCode.COMMENT_NOT_FOUND));

		if(!comment.getMember().getId().equals(memberId)) {
			throw new BusinessException(CommentExceptionCode.COMMENT_FORBIDDEN);
		}

		comment.update(request.content());

		return CommentResponse.CommentInfo.from(comment);
	}

	@Transactional
	public void deleteComment(Long id, UUID memberId, boolean isAdmin) {
		Comment comment = commentRepository.findById(id)
				.orElseThrow(() -> new BusinessException(CommentExceptionCode.COMMENT_NOT_FOUND));

		boolean isAuthor = comment.getMember().getId().equals(memberId);

		if(!isAuthor && !isAdmin) {
			throw new BusinessException(CommentExceptionCode.COMMENT_FORBIDDEN);
		}

		comment.delete();
	}
}
