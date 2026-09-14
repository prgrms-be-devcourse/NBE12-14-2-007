package com.team007.room_escape.domain.manager.service;

import com.team007.room_escape.domain.manager.dto.ManagerRequest.ManagerApply;
import com.team007.room_escape.domain.manager.infra.entity.Manager;
import com.team007.room_escape.domain.manager.infra.entity.ManagerStatus;
import com.team007.room_escape.domain.manager.infra.repository.ManagerRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManagerService {
    private final ManagerRepository managerRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void apply(ManagerApply request, UUID memberId) {

        Member member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));

        Manager manager = Manager.builder()
            .member(member)
            .organization(request.organization())
            .companyPhone(request.companyPhone())
            .reason(request.reason())
            .status(ManagerStatus.APPLYING)
            .build();

        managerRepository.save(manager);
    }

}
