package com.team007.room_escape.domain.manager.controller;

import com.team007.room_escape.domain.manager.dto.ManagerRequest;
import com.team007.room_escape.domain.manager.service.ManagerService;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/managers")
@RequiredArgsConstructor
public class ManagerController {

    private final ManagerService managerService;

    @PostMapping("/apply")
    public ApiResponse<Void> applyManager(
        @Valid @RequestBody ManagerRequest.ManagerApply request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        managerService.apply(request, userDetails.getId());
        return ApiResponse.noContentSuccess();
    }
}
