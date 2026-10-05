package com.team007.room_escape.domain.like.type;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** 좋아요순 정렬(sort=likeCount,desc)이면 전용 쿼리를 쓰고 Pageable에서는 정렬을 뗀다. desc만 지원한다. */
public final class LikeSort {

	public static final String PROPERTY = "likeCount";

	private LikeSort() {
	}

	public static boolean isRequested(Pageable pageable) {
		return pageable.getSort().getOrderFor(PROPERTY) != null;
	}

	/** 페이지 번호와 크기만 남긴다. 순서는 전용 쿼리의 ORDER BY 가 정한다. */
	public static Pageable withoutSort(Pageable pageable) {
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
	}
}
