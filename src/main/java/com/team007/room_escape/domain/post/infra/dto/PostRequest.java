package com.team007.room_escape.domain.post.infra.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PostRequest {

    @Schema(name = "PostCreateRequest", description = "후기 작성 요청")
    public record PostCreateRequest(
            @Size(min = 2, max = 30, message = "제목은 2글자 이상 30글자 이하로 작성해주세요.")
            @NotBlank
            @Schema(description = "후기 제목", example = "서울 불꽃축제 다녀왔어요")
            String title,
            @NotBlank
            @Schema(description = "후기 내용", example = "불꽃축제가 정말 재미있었습니다.")
            String content,
            @Size(max = 2048)
            @Schema(description = "썸네일 이미지 URL", example = "https://example.com/thumbnail.jpg")
            String thumbnail
    ) {
    }

    @Schema(name = "PostUpdateRequest", description = "후기 수정 요청")
    public record PostUpdateRequest(
            @Size(min = 2, max = 30, message = "제목은 2글자 이상 30글자 이하로 작성해주세요.")
            @NotBlank
            @Schema(description = "수정할 후기 제목", example = "서울 불꽃축제 후기 수정")
            String title,
            @NotBlank
            @Schema(description = "수정할 후기 내용", example = "후기 내용을 수정했습니다.")
            String content,
            @Size(max = 2048)
            @Schema(description = "수정할 썸네일 이미지 URL", example = "https://example.com/thumbnail.jpg")
            String thumbnail
    ) {
    }

}
