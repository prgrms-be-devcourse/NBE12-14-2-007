package com.team007.room_escape.domain.member.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;

import lombok.Builder;
import lombok.Getter;

@Getter
public class MemberResponse {

    /**
     * 마이페이지 응답. 본인만 조회하므로 email, phone 같은 개인정보를 포함한다.
     * password는 절대 내려보내지 않는다.
     */
    @Builder
    public record MyPageInfo(
        UUID id,
        String email,
        String nickname,
        String profileImg,
        String phone,
        MemberRole role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        /**
         * @param profileImgUrl R2 key를 변환한 공개 URL. 이미지가 없으면 null
         */
        public static MyPageInfo from(Member member, String profileImgUrl) {
            return MyPageInfo.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .profileImg(profileImgUrl)
                .phone(member.getPhone())
                .role(member.getRole())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
        }
    }

    @Builder 
    public record MemberInfo(
        UUID id,
        String nickname,
        MemberRole role,
        String profileImg
    ) {
        public static MemberInfo form(Member member) {
            return MemberInfo.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .role(member.getRole())
                .profileImg(member.getProfileImg())
                .build();
        }
    }
	
}