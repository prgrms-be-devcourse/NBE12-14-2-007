package com.team007.room_escape.domain.comment.infra.entity;

import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.post.infra.entity.Post;
import com.team007.room_escape.global.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "`comment`")
// 소프트 삭제된 댓글은 모든 조회(findById, findAll, JPQL, 연관관계 로딩)에서 자동으로 제외된다.
@SQLRestriction("deleted_at is null")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Comment extends SoftDeletableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	//TODO : 별점과, 대댓글 생각해보기 ~ 

	public void update(String content) {
		this.content = content;
	}
}
