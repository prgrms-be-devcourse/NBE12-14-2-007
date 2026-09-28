package com.team007.room_escape.domain.like.type;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * 목록 API의 좋아요순 정렬(sort=likeCount,desc) 판별.
 *
 * likeCount 는 엔티티 컬럼이 아니라 like 테이블을 세서 만드는 값이라
 * Pageable 정렬로 그대로 넘기면 없는 속성이라 실패한다.
 * 그래서 좋아요순이 요청되면 정렬이 박혀 있는 전용 쿼리를 쓰고, Pageable 에서는 정렬을 뗀다.
 * 방향은 많은 순(desc)만 지원한다.
 */
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
