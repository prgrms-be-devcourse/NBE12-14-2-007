package com.team007.room_escape.domain.like.infra.dto;

import java.util.UUID;

/** 후기별 좋아요 수 (목록 조회 시 한 번에 세기 위한 결과) */
public record PostLikeCount(UUID postId, Long likeCount) {
}
