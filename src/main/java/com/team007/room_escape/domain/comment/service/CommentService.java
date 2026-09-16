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
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import com.team007.room_escape.global.response.code.PostExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

	private final CommentRepository commentRepository;
	private final MemberRepository memberRepository;
	private final PostRepository postRepository;

	@Transactional
	public CommentResponse.CommentCreateResponse createComment(
			UUID postId,
			CommentRequest.CommentCreateRequest request,
			UUID memberId
	) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));

		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND));

		Comment comment = Comment.builder()
				.post(post)
				.member(member)
				.content(request.content())
				.build();

		commentRepository.save(comment);

		return CommentResponse.CommentCreateResponse.from(comment);
	}
}
