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

	/**
	 * 삭제를 되돌린다. 관리자가 잘못 지운 것을 복구할 때 쓴다.
	 *
	 * 주의: 작성자가 스스로 지운 것과 관리자가 지운 것을 deletedAt 하나로는 구분할 수 없다.
	 * 작성자가 지운 글을 관리자가 되살리면 안 되므로, 호출하는 쪽에서 그 판단을 해야 한다.
	 */
	public void restore() {
		this.deletedAt = null;
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}
}
