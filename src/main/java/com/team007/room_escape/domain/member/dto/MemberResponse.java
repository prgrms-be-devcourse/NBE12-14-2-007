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

    /**
     * 관리자 화면에서 보는 회원 정보. password 외의 모든 필드를 담는다.
     * 탈퇴 회원도 다뤄야 해서 deletedAt 까지 내려준다.
     */
    @Builder
    public record AdminInfo(
        UUID id,
        String email,
        String nickname,
        /** R2 key를 변환한 공개 URL. 이미지가 없으면 null */
        String profileImg,
        String phone,
        MemberRole role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        /** 탈퇴 시각. 탈퇴하지 않았으면 null */
        LocalDateTime deletedAt
    ) {
        public static AdminInfo from(Member member, String profileImgUrl) {
            return AdminInfo.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .profileImg(profileImgUrl)
                .phone(member.getPhone())
                .role(member.getRole())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .deletedAt(member.getDeletedAt())
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
        public static MemberInfo from(Member member, String profileImgUrl) {
            return MemberInfo.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .role(member.getRole())
                .profileImg(profileImgUrl)
                .build();
        }
    }
	
}
