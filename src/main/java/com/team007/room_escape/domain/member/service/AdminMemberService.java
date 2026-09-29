package com.team007.room_escape.domain.member.service;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team007.room_escape.domain.member.dto.AdminMemberRequest;
import com.team007.room_escape.domain.member.dto.MemberResponse;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.CommonExceptionCode;
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import com.team007.room_escape.global.storage.ImageUrlResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminMemberService {


	private final MemberRepository memberRepository;
	private final ImageUrlResolver imageUrlResolver;

	/** 관리자 회원 검색. 조건을 비우면 전체를 조회한다. */
	@Transactional(readOnly = true)
	public Page<MemberResponse.AdminInfo> search(AdminMemberRequest.Search request, Pageable pageable) {

		return memberRepository.search(
			request.keywordOrEmpty(),
			request.role(),
			request.includeDeletedOrFalse(),
			pageable
		).map(this::toAdminInfo);
	}

	/** 관리자 회원 단건 조회. 신고된 회원이 탈퇴했어도 확인할 수 있게 탈퇴 회원도 찾는다. */
	@Transactional(readOnly = true)
	public MemberResponse.AdminInfo get(UUID memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));

		return toAdminInfo(member);
	}

	/**
	 * 회원 등급을 변경한다.
	 *
	 * 화면에서도 관리자 계정을 막고 있지만 API를 직접 호출하면 그만이므로
	 * 아래 네 가지는 반드시 서버에서 막는다.
	 *
	 * @param actorId 등급을 바꾸는 관리자 본인의 id
	 */
	@Transactional
	public MemberResponse.AdminInfo changeRole(UUID actorId, UUID targetId, AdminMemberRequest.ChangeRole request) {
		// 1. 자기 등급을 내리면 되돌릴 권한까지 함께 잃어 복구할 수 없다.
		if (actorId.equals(targetId)) {
			throw new BusinessException(MemberExceptionCode.MEMBER_ROLE_SELF_CHANGE_DENIED);
		}

		// 2. 등급 부여로 관리자를 만들 수 있으면 권한 상승 경로가 열린다.
		//  TODO : 나중에 최고관리자 역할을 또 만들어서 관리자 만들수 있게 할수도있음
		if (request.role() == MemberRole.ROLE_ADMIN) {
			throw new BusinessException(MemberExceptionCode.MEMBER_ROLE_ADMIN_GRANT_DENIED);
		}

		// 탈퇴 회원도 찾아야 상태를 구분해서 알려줄 수 있다.
		Member target = memberRepository.findById(targetId)
			.orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));

		// 3. 관리자끼리 서로 등급을 박탈하는 상황을 막는다. 관리자 해제는 DB에서 직접 한다.
		if (target.getRole() == MemberRole.ROLE_ADMIN) {
			throw new BusinessException(MemberExceptionCode.MEMBER_ROLE_ADMIN_PROTECTED);
		}

		// 4. 탈퇴한 회원은 로그인 자체가 안 되므로 등급을 바꿔도 의미가 없다.
		if (target.isDeleted()) {
			throw new BusinessException(MemberExceptionCode.MEMBER_ALREADY_DELETED);
		}

		// TODO 변경 사유를 함께 받아 이력 테이블에 남길 것. Member.changeRole()의 TODO 참고.
		target.changeRole(request.role());

		return toAdminInfo(target);
	}

	/** 프로필 이미지는 R2 key로 저장되므로 응답에서는 공개 URL로 바꿔 내려준다. */
	private MemberResponse.AdminInfo toAdminInfo(Member member) {
		return MemberResponse.AdminInfo.from(
			member,
			imageUrlResolver.resolve(member.getProfileImg())
		);
	}

}
