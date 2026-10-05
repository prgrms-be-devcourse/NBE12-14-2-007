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

	/** 좋아요 수. increase/decreaseLikeCount로만 바꾸고, 엔티티 저장 때 옛 값으로 덮지 않게 updatable = false. */
	@Column(nullable = false, updatable = false)
	private long likeCount;

	public void update(String title, String content, String thumbnail) {
		this.title = title;
		this.content = content;
		this.thumbnail = thumbnail;
	}
}

