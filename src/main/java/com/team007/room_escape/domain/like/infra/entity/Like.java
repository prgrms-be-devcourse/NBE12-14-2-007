package com.team007.room_escape.domain.like.infra.entity;

import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.post.infra.entity.Post;
import com.team007.room_escape.global.entity.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
		name = "`like`",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uk_like_post_member",
						columnNames = {"src_id", "member_id"}
				)
		}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Like extends BaseTimeEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "src_id", nullable = false)
	private Post post;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;
}
