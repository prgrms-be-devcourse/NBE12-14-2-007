package com.team007.room_escape.domain.member.service;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalAccuracyVoteRepository;
import com.team007.room_escape.domain.like.infra.repository.LikeRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberTrustGradeService {

	private static final long GRADE_THRESHOLD = 10L;

	private final LikeRepository likeRepository;
	private final FestivalAccuracyVoteRepository accuracyVoteRepository;

	/** 행사 반응이 바뀔 때 작성자의 현재 신뢰 등급을 다시 계산한다. */
	@Transactional
	public void refreshForFestival(Festival festival) {
		Member author = festival.getMember();
		if (festival.getProviderType() != ProviderType.MEMBER || author == null) {
			return;
		}
		if (author.getRole() == MemberRole.ROLE_WARNING
			|| author.getRole() == MemberRole.ROLE_ADMIN) {
			return;
		}

		long receivedLikeCount = likeRepository
			.countReceivedFestivalLikesByAuthorId(author.getId());
		long receivedAccurateCount = accuracyVoteRepository
			.countReceivedAccurateVotesByAuthorId(author.getId());

		boolean enoughLikes = receivedLikeCount >= GRADE_THRESHOLD;
		boolean enoughAccurateVotes = receivedAccurateCount >= GRADE_THRESHOLD;

		MemberRole grade = enoughLikes && enoughAccurateVotes
			? MemberRole.ROLE_TRUSTED
			: enoughLikes || enoughAccurateVotes
				? MemberRole.ROLE_RECOGNIZED
				: MemberRole.ROLE_UNVERIFIED;

		author.applyTrustGrade(grade);
	}
}
