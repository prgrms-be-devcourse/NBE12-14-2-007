package com.team007.room_escape.domain.festival.infra.entity;

import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.global.entity.SoftDeletableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;

import lombok.*;

// 삭제된 행사를 참조하는 후기·제보 조회가 깨지지 않게 @SQLRestriction을 걸지 않는다. 목록·상세는 deletedAtIsNull로 거른다.
@Entity
@Table(name = "festival")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Festival extends SoftDeletableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider_type", nullable = false, length = 32)
	private ProviderType providerType;

	@Column(name = "external_id", length = 64)
	private String externalId;

	private String title;

	private String category;

	private String manager;

	@Column(columnDefinition = "text")
	private String content;

	@Column(length = 2048)
	private String url;

	@Column(name = "img_url", length = 2048)
	private String imgUrl;

	@Column(name = "begin_de")
	private LocalDateTime beginDe;

	@Column(name = "end_de")
	private LocalDateTime endDe;

	@Column(name = "partcpt_expn_info", columnDefinition = "text")
	private String partcptExpnInfo;

	@Column(name = "telno_info", columnDefinition = "text")
	private String telnoInfo;

	@Enumerated(EnumType.STRING)
	@Column(length = 32)
	private FestivalStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "region", length = 32)
	private FestivalRegion region;

	@Column(name = "region_detail", columnDefinition = "text")
	private String regionDetail;

	@Column(columnDefinition = "text")
	private String place;

	@Column(name = "place_address", columnDefinition = "text")
	private String placeAddress;

	/** 삭제 주체·사유는 공통 이력에 기록하며 행사는 현재 삭제 상태만 관리한다. */
	@Override
	public void delete() {
		if (isDeleted()) {
			throw new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_FOUND);
		}
		super.delete();
	}

	@Override
	public void restore() {
		if (!isDeleted()) {
			throw new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_DELETED);
		}
		super.restore();
	}

	// TODO 인자가 많아서 정리 필요
	public void updateDetails(
			String title, String category,
			String content, String referenceUrl,
			FestivalRegion region, String regionDetail, String imgUrl,
			LocalDateTime beginDe, LocalDateTime endDe,
			String partcptExpnInfo, String telnoInfo
	) {
		this.title = title;
		this.category = category;
		this.content = content;
		this.url = referenceUrl;
		this.region = region;
		this.regionDetail = regionDetail;
		this.imgUrl = imgUrl;
		this.beginDe = beginDe;
		this.endDe = endDe;
		this.status = FestivalStatus.from(endDe);
		this.partcptExpnInfo = partcptExpnInfo;
		this.telnoInfo = telnoInfo;
	}

	/** 이관 배치에서 원본 URL을 R2 URL로 교체할 때 쓴다. 다른 필드는 건드리지 않는다. */
	public void updateImgUrl(String imgUrl) {
		this.imgUrl = imgUrl;
	}
}
