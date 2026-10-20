package com.team007.room_escape.domain.community.service;

import com.team007.room_escape.domain.community.dto.CommunityRequest;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionTargetType;
import com.team007.room_escape.domain.deletion.service.DeletionHistoryService;
import com.team007.room_escape.domain.community.dto.CommunityResponse;
import com.team007.room_escape.domain.community.infra.entity.CommunityComment;
import com.team007.room_escape.domain.community.infra.entity.CommunityPost;
import com.team007.room_escape.domain.community.infra.repository.CommunityCommentRepository;
import com.team007.room_escape.domain.community.infra.repository.CommunityPostRepository;
import com.team007.room_escape.domain.community.type.CommunityCategory;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.CommonExceptionCode;
import com.team007.room_escape.global.response.code.CommunityExceptionCode;
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import com.team007.room_escape.global.storage.ImageUrlResolver;
import com.team007.room_escape.global.util.RichTextSanitizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunityService {
	private static final long POST_COOLDOWN_SECONDS = 30;
	private static final long COMMENT_COOLDOWN_SECONDS = 5;
	private static final long DUPLICATE_CHECK_SECONDS = 60;

	private final CommunityPostRepository postRepository;
	private final CommunityCommentRepository commentRepository;
	private final MemberRepository memberRepository;
	private final RichTextSanitizer richTextSanitizer;
	private final ImageUrlResolver imageUrlResolver;
	private final DeletionHistoryService deletionHistoryService;

	@Transactional(readOnly = true)
	public Page<CommunityResponse.PostListItem> getPosts(
		CommunityCategory category,
		String keyword,
		Pageable pageable
	) {
		String normalizedKeyword = keyword == null ? "" : keyword.trim();
		Page<CommunityPost> posts = postRepository.search(category, normalizedKeyword, pageable);
		Map<UUID, Long> commentCounts = posts.isEmpty()
			? Map.of()
			: commentRepository.countByPostIds(
				posts.getContent().stream().map(CommunityPost::getId).toList()
			).stream().collect(Collectors.toMap(
				CommunityCommentRepository.PostCommentCount::getPostId,
				CommunityCommentRepository.PostCommentCount::getCommentCount
			));
		return posts.map(post -> CommunityResponse.PostListItem.from(
			post,
			commentCounts.getOrDefault(post.getId(), 0L),
			imageUrlResolver
		));
	}

	@Transactional
	public CommunityResponse.PostDetail getPost(UUID postId) {
		if (postRepository.increaseViewCount(postId) == 0) {
			throw new BusinessException(CommunityExceptionCode.POST_NOT_FOUND);
		}
		CommunityPost post = getPostEntity(postId);
		return CommunityResponse.PostDetail.from(post, commentRepository.countByPostIdAndDeletedAtIsNull(postId), imageUrlResolver);
	}

	@Transactional
	public CommunityResponse.PostDetail createPost(
		CommunityRequest.PostUpsert request,
		UUID memberId
	) {
		Member member = getActiveMember(memberId);
		String title = request.title().trim();
		String content = sanitizeRequiredContent(request.content());
		checkPostCreationAllowed(memberId, content);

		CommunityPost post = CommunityPost.builder()
			.member(member)
			.category(request.category())
			.title(title)
			.content(content)
			.build();
		postRepository.save(post);

		return CommunityResponse.PostDetail.from(post, 0, imageUrlResolver);
	}

	@Transactional
	public CommunityResponse.PostDetail updatePost(
		UUID postId,
		CommunityRequest.PostUpsert request,
		UUID memberId
	) {
		getActiveMember(memberId);
		CommunityPost post = getPostEntity(postId);
		checkAuthor(post.getMember().getId(), memberId, CommunityExceptionCode.POST_FORBIDDEN);

		post.update(
			request.category(),
			request.title().trim(),
			sanitizeRequiredContent(request.content())
		);
		return CommunityResponse.PostDetail.from(post, commentRepository.countByPostIdAndDeletedAtIsNull(postId), imageUrlResolver);
	}

	@Transactional
	public void deletePost(UUID postId, UUID memberId) {
		CommunityPost post = postRepository.findForDeletion(postId)
			.orElseThrow(() -> new BusinessException(CommunityExceptionCode.POST_NOT_FOUND));
		if (!post.getMember().getId().equals(memberId)) {
			throw new BusinessException(CommunityExceptionCode.POST_FORBIDDEN);
		}
		post.delete();
		deletionHistoryService.recordAuthorDeletion(DeletionTargetType.COMMUNITY_POST, postId.toString(), memberId);
	}

	/** 일반 댓글을 오래된 순으로 페이지 나누고, 각 댓글 아래에 대댓글을 붙인다. 삭제된 댓글도 자리를 남긴다. */
	@Transactional(readOnly = true)
	public Page<CommunityResponse.CommentInfo> getComments(UUID postId, Pageable pageable) {
		getPostEntity(postId);
		// 정렬은 쿼리에 고정돼 있어서 클라이언트가 보낸 sort는 버린다.
		Page<CommunityComment> roots = commentRepository.findRootsByPostId(
			postId, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
		Map<Long, List<CommunityComment>> repliesByParentId = roots.isEmpty()
			? Map.of()
			: commentRepository.findRepliesByParentIds(roots.map(CommunityComment::getId).toList()).stream()
				.collect(Collectors.groupingBy(reply -> reply.getParent().getId()));
		return roots.map(root -> CommunityResponse.CommentInfo.of(
			root,
			repliesByParentId.getOrDefault(root.getId(), List.of()),
			imageUrlResolver
		));
	}

	@Transactional
	public CommunityResponse.CommentInfo createComment(
		UUID postId,
		CommunityRequest.CommentCreate request,
		UUID memberId
	) {
		Member member = getActiveMember(memberId);
		CommunityPost post = getPostEntity(postId);
		String content = request.content().trim();
		checkCommentCreationAllowed(memberId, content);
		CommunityComment comment = CommunityComment.builder()
			.post(post)
			.member(member)
			.parent(findParent(request.parentId(), postId))
			.content(content)
			.build();
		commentRepository.save(comment);
		return CommunityResponse.CommentInfo.from(comment, imageUrlResolver);
	}

	@Transactional
	public CommunityResponse.CommentInfo updateComment(
		Long commentId,
		CommunityRequest.CommentUpsert request,
		UUID memberId
	) {
		getActiveMember(memberId);
		CommunityComment comment = getCommentEntity(commentId);
		checkAuthor(comment.getMember().getId(), memberId, CommunityExceptionCode.COMMENT_FORBIDDEN);
		comment.update(request.content().trim());
		return CommunityResponse.CommentInfo.from(comment, imageUrlResolver);
	}

	/** 행은 지우지 않고 삭제 시각만 남긴다. 대댓글은 그대로 두고, 목록에는 "삭제된 댓글입니다"로 보인다. */
	@Transactional
	public void deleteComment(Long commentId, UUID memberId) {
		CommunityComment comment = commentRepository.findForDeletion(commentId)
			.orElseThrow(() -> new BusinessException(CommunityExceptionCode.COMMENT_NOT_FOUND));
		if (!comment.getMember().getId().equals(memberId)) {
			throw new BusinessException(CommunityExceptionCode.COMMENT_FORBIDDEN);
		}
		comment.delete();
		deletionHistoryService.recordAuthorDeletion(DeletionTargetType.COMMUNITY_COMMENT, commentId.toString(), memberId);
	}

	private Member getActiveMember(UUID memberId) {
		Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));
		if (member.isRestricted()) {
			throw new BusinessException(MemberExceptionCode.MEMBER_RESTRICTED);
		}
		return member;
	}

	private CommunityPost getPostEntity(UUID postId) {
		return postRepository.findById(postId)
			.orElseThrow(() -> new BusinessException(CommunityExceptionCode.POST_NOT_FOUND));
	}

	/**
	 * 답글을 달 부모 댓글. 대댓글에 답글을 달면 그 대댓글의 부모 아래에 붙여 한 단계를 유지한다.
	 * 삭제된 댓글에는 새 답글을 달 수 없다.
	 */
	private CommunityComment findParent(Long parentId, UUID postId) {
		if (parentId == null) {
			return null;
		}
		CommunityComment parent = getCommentEntity(parentId);
		if (!parent.getPost().getId().equals(postId)) {
			throw new BusinessException(CommunityExceptionCode.COMMENT_PARENT_MISMATCH);
		}
		return parent.isReply() ? parent.getParent() : parent;
	}

	/** 삭제되지 않은 댓글. 삭제된 댓글은 수정·삭제·답글 대상이 아니다. */
	private CommunityComment getCommentEntity(Long commentId) {
		return commentRepository.findByIdAndDeletedAtIsNull(commentId)
			.orElseThrow(() -> new BusinessException(CommunityExceptionCode.COMMENT_NOT_FOUND));
	}

	private String sanitizeRequiredContent(String content) {
		String sanitized = richTextSanitizer.sanitize(content);
		if (!richTextSanitizer.hasVisibleText(sanitized)) {
			throw new BusinessException(CommonExceptionCode.INVALID_INPUT);
		}
		return sanitized;
	}

	private void checkPostCreationAllowed(UUID memberId, String content) {
		LocalDateTime now = LocalDateTime.now();
		if (postRepository.existsByMemberIdAndContentAndCreatedAtAfter(
			memberId,
			content,
			now.minusSeconds(DUPLICATE_CHECK_SECONDS)
		)) {
			throw new BusinessException(CommunityExceptionCode.POST_DUPLICATE);
		}
		if (postRepository.existsByMemberIdAndCreatedAtAfter(
			memberId,
			now.minusSeconds(POST_COOLDOWN_SECONDS)
		)) {
			throw new BusinessException(CommunityExceptionCode.POST_CREATE_TOO_SOON);
		}
	}

	private void checkCommentCreationAllowed(UUID memberId, String content) {
		LocalDateTime now = LocalDateTime.now();
		if (commentRepository.existsByMemberIdAndContentAndDeletedAtIsNullAndCreatedAtAfter(
			memberId,
			content,
			now.minusSeconds(DUPLICATE_CHECK_SECONDS)
		)) {
			throw new BusinessException(CommunityExceptionCode.COMMENT_DUPLICATE);
		}
		if (commentRepository.existsByMemberIdAndDeletedAtIsNullAndCreatedAtAfter(
			memberId,
			now.minusSeconds(COMMENT_COOLDOWN_SECONDS)
		)) {
			throw new BusinessException(CommunityExceptionCode.COMMENT_CREATE_TOO_SOON);
		}
	}

	private void checkAuthor(UUID authorId, UUID memberId, CommunityExceptionCode exceptionCode) {
		if (!authorId.equals(memberId)) {
			throw new BusinessException(exceptionCode);
		}
	}
}
