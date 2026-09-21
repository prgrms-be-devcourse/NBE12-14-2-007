package com.team007.room_escape.domain.festival.dto;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import java.time.LocalDateTime;
import java.util.UUID;

/** 행사 공개 정보. 개인 제보 내용과 회원 연락처는 포함하지 않는다. */
public record FestivalBrowseResponse(
        Long festivalId,
        ProviderType providerType,
        String title,
        String category,
        String instNm,
        String manager,
        String festivalContent,
        String referenceUrl,
        FestivalRegion region,
        String regionDetail,
        String imgUrl,
        LocalDateTime beginDe,
        LocalDateTime endDe,
        String eventTmInfo,
        String partcptExpnInfo,
        String telnoInfo,
        String hostInstNm,
        LocalDateTime writngDe,
        FestivalStatus status,
        Submitter submitter
) {
    public record Submitter(UUID id, String nickname) {}

    public static FestivalBrowseResponse from(Festival festival) {
        var member = festival.getMember();
        Submitter submitter = member == null ? null : new Submitter(
                member.getId(), member.isDeleted() ? "탈퇴한 회원" : member.getNickname());
        return new FestivalBrowseResponse(
                festival.getId(), festival.getProviderType(), festival.getTitle(),
                festival.getCategory(), festival.getInstNm(), festival.getManager(),
                festival.getContent(), festival.getUrl(), festival.getRegion(),
                festival.getRegionDetail(), festival.getImgUrl(), festival.getBeginDe(),
                festival.getEndDe(), festival.getEventTmInfo(), festival.getPartcptExpnInfo(),
                festival.getTelnoInfo(), festival.getHostInstNm(), festival.getWritngDe(),
                FestivalStatus.from(festival.getEndDe()), submitter);
    }
}
