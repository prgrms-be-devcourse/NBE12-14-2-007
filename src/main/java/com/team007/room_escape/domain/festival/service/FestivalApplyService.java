package com.team007.room_escape.domain.festival.service;

import com.team007.room_escape.domain.festival.dto.FestivalApplyRequest.CreateFestivalApplyRequest;
import com.team007.room_escape.domain.festival.dto.FestivalApplyResponse.CreateFestivalApplyResponse;
import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalApply;
import com.team007.room_escape.domain.festival.infra.entity.FestivalApplyStatus;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalApplyRepository;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FestivalApplyService {

    private final FestivalRepository festivalRepository;
    private final FestivalApplyRepository festivalApplyRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public CreateFestivalApplyResponse create(UUID memberId, CreateFestivalApplyRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));

        Festival festival = Festival.builder()
                .member(member)
                .providerType(ProviderType.MEMBER)
                .instNm(request.instNm())
                .title(request.title())
                .category(request.category())
                .manager(request.manager())
                .content(request.festivalContent())
                .url(request.url())
                .imgUrl(request.imgUrl())
                .beginDe(request.beginDe())
                .endDe(request.endDe())
                .eventTmInfo(request.eventTmInfo())
                .partcptExpnInfo(request.partcptExpnInfo())
                .telnoInfo(request.telnoInfo())
                .hostInstNm(request.hostInstNm())
                .hmpgUrl(request.hmpgUrl())
                .writngDe(LocalDateTime.now())
                .applyStatus(FestivalApplyStatus.PENDING)
                .build();

        Festival savedFestival = festivalRepository.save(festival);

        FestivalApply festivalApply = FestivalApply.builder()
                .festivalId(savedFestival.getId())
                .content(request.applyContent())
                .build();

        FestivalApply savedFestivalApply =
                festivalApplyRepository.save(festivalApply);

        return CreateFestivalApplyResponse.from(
                savedFestival,
                savedFestivalApply
        );
    }
}