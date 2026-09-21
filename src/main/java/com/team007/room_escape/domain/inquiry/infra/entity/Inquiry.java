package com.team007.room_escape.domain.inquiry.infra.entity;

import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.global.entity.SoftDeletableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "inquiry")
@Getter
@SQLRestriction("deleted_at is null")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor (access = AccessLevel.PRIVATE)
@Builder 
public class Inquiry extends SoftDeletableEntity {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private InquiryCategory category;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(length = 2048)
	private String img;

	@Column(columnDefinition = "text")
	private String answer;

	@Enumerated(EnumType.STRING)
	@Column(length = 32)
	private InquiryStatus status;

	/** 작성자 본인인지. member가 없는 과거 데이터도 있어 null을 먼저 거른다. */
	public boolean isWrittenBy(UUID memberId) {
		return member != null && member.getId().equals(memberId);
	}

	/**
	 * 답변이 달렸는지.
	 * status와 answer 둘 다 보는 이유는, 답변만 넣고 상태를 안 바꾼 경우에도
	 * 수정이 막혀야 하기 때문이다.
	 */
	public boolean isAnswered() {
		return status == InquiryStatus.ANSWERED || (answer != null && !answer.isBlank());
	}

	/**
	 * 문의 수정. PATCH 의미에 맞춰 null인 값은 건드리지 않는다.
	 * img는 빈 문자열이면 첨부를 지운다.
	 *
	 * @param img R2 저장 key. 공개 URL이 아니다
	 */
	public void update(InquiryCategory category, String title, String content, String img) {
		if (category != null) {
			this.category = category;
		}
		if (title != null && !title.isBlank()) {
			this.title = title;
		}
		if (content != null && !content.isBlank()) {
			this.content = content;
		}
		if (img != null) {
			this.img = img.isBlank() ? null : img;
		}
	}
}
