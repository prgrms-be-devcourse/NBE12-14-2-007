package com.team007.room_escape.domain.post.infra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
        @Size(min = 2, max = 30, message = "제목은 2글자 이상 30글자 이하로 작성해주세요.")
        @NotBlank
        String title,
        @Size(min = 10, max = 300, message = "내용은 2글자 이상 10글자 이하로 작성해주세요.")
        @NotBlank
        String content,
        @Size(max = 2048)
        String thumbnail
) {
}
