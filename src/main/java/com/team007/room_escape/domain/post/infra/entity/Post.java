package com.team007.room_escape.domain.post.infra.entity;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.global.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Post extends SoftDeletableEntity {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pu_fev_id")
	private Festival festival;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(length = 2048)
	private String thumbnail;

	/**
	 * 좋아요 수. 좋아요순 정렬용으로 "like" 테이블 건수를 따로 들고 있는다.
	 * 좋아요 등록·취소 때 PostRepository.increaseLikeCount / decreaseLikeCount 로만 바꾼다.
	 *
	 * updatable = false: 후기 수정·삭제로 엔티티를 저장할 때 읽어 둔 옛 값으로 덮어쓰지 않게 한다.
	 * (그 사이 다른 사람이 누른 좋아요가 사라지는 것을 막는다)
	 */
	@Column(nullable = false, updatable = false)
	private long likeCount;

	public void update(String title, String content, String thumbnail) {
		this.title = title;
		this.content = content;
		this.thumbnail = thumbnail;
	}
}

