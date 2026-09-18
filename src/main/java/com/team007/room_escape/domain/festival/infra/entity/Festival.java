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

import lombok.*;

// Post, FestivalSubmission 이 이 엔티티를 @ManyToOne 으로 참조한다.
// @SQLRestriction 을 걸면 삭제된 행사를 참조하는 후기·제보를 조회할 때 예외가 나므로 걸지 않는다.
// 목록/상세 조회는 리포지토리에서 deletedAtIsNull 로 직접 걸러야 한다.
//TODO : 삭제시 "삭제된 행사입니다" 로 응답에 표시하기
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

	@Column(name = "inst_nm")
	private String instNm;

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

	@Column(name = "event_tm_info")
	private String eventTmInfo;

	@Column(name = "partcpt_expn_info")
	private String partcptExpnInfo;

	@Column(name = "telno_info")
	private String telnoInfo;

	@Column(name = "host_inst_nm")
	private String hostInstNm;

	@Column(name = "hmpg_url", length = 2048)
	private String hmpgUrl;

	@Column(name = "writng_de")
	private LocalDateTime writngDe;

	@Enumerated(EnumType.STRING)
	@Column(length = 32)
	private FestivalStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "region", length = 32)
	private FestivalRegion region;

	@Column(name = "region_detail", length = 255)
	private String regionDetail;

	//TODO 로 나중에 코드를 좀더 이쁘게 해봅시다
	public void updateDetails(
			String instNm, String title, String category,
			String manager, String content, String url, String imgUrl,
			LocalDateTime beginDe, LocalDateTime endDe,
			String eventTmInfo, String partcptExpnInfo,
			String telnoInfo, String hostInstNm, String hmpgUrl
	) {
		this.instNm = instNm;
		this.title = title;
		this.category = category;
		this.manager = manager;
		this.content = content;
		this.url = url;
		this.imgUrl = imgUrl;
		this.beginDe = beginDe;
		this.endDe = endDe;
		this.status = FestivalStatus.from(endDe);
		this.eventTmInfo = eventTmInfo;
		this.partcptExpnInfo = partcptExpnInfo;
		this.telnoInfo = telnoInfo;
		this.hostInstNm = hostInstNm;
		this.hmpgUrl = hmpgUrl;
	}
}
