package com.team007.room_escape.domain.member.dto;

import java.util.Locale;

import com.team007.room_escape.domain.member.infra.entity.MemberRole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public class AdminMemberRequest {

	private AdminMemberRequest() {
	}

	/** 관리자 회원 검색 조건. 모든 항목이 선택이며, 비우면 전체를 조회한다. */
	@Schema(name = "AdminMemberSearchRequest", description = "관리자 회원 검색 조건")
	public record Search(

		@Schema(description = "닉네임 또는 이메일 검색어. 부분 일치, 대소문자 무시", example = "autumn")
		String keyword,

		@Schema(description = "회원 등급. 비우면 전체 등급을 조회한다", example = "ROLE_WARNING")
		MemberRole role,

		@Schema(description = "탈퇴 회원 포함 여부. 기본값 false", example = "false", defaultValue = "false")
		Boolean includeDeleted
	) {

		/** 검색어가 없으면 null 대신 빈 문자열을 돌려준다. null이면 Postgres LIKE가 실패한다. */
		public String keywordOrEmpty() {
			if (keyword == null || keyword.isBlank()) {
				return "";
			}

			return keyword.trim().toLowerCase(Locale.ROOT);
		}

		public boolean includeDeletedOrFalse() {
			return Boolean.TRUE.equals(includeDeleted);
		}
	}

	/** 회원 등급 변경 요청. ROLE_ADMIN은 서비스에서 막는다. */
	@Schema(name = "AdminMemberRoleChangeRequest", description = "회원 등급 변경 요청")
	public record ChangeRole(

		@NotNull
		@Schema(description = "변경할 등급. ROLE_ADMIN은 부여할 수 없다", example = "ROLE_TRUSTED")
		MemberRole role
	) {
	}
}
