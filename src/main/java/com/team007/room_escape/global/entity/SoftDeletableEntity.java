package com.team007.room_escape.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 소프트 삭제(deleted_at)를 쓰는 엔티티의 상위 클래스.
 * delete() 호출 시 행을 지우지 않고 삭제 시각만 기록한다.
 */
@Getter
@MappedSuperclass
public abstract class SoftDeletableEntity extends BaseTimeEntity {

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public void delete() {
		this.deletedAt = LocalDateTime.now();
	}

	/** 삭제를 되돌린다. 누가 지웠는지 구분할 수 없으니 작성자가 지운 글인지는 호출하는 쪽이 판단한다. */
	public void restore() {
		this.deletedAt = null;
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}
}
