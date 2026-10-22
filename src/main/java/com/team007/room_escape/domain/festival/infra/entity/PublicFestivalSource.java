package com.team007.room_escape.domain.festival.infra.entity;

import com.team007.room_escape.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "public_festival_source")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicFestivalSource extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String source;

	/** 이번 배치에서 받아온 행 개수. jsonb를 파싱하지 않고 건수를 보려고 둔다. */
	@Column(name = "total_count", nullable = false)
	private int totalCount;

	public PublicFestivalSource(String source, int totalCount) {
		this.source = source;
		this.totalCount = totalCount;
	}
}
