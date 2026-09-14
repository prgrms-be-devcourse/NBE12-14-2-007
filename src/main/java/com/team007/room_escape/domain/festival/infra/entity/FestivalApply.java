package com.team007.room_escape.domain.festival.infra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "festival_apply")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalApply {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "festival_id", nullable = false)
    private Long festivalId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    public static FestivalApply create(Long festivalId, String content) {
        FestivalApply festivalApply = new FestivalApply();
        festivalApply.festivalId = festivalId;
        festivalApply.content = content;
        return festivalApply;
    }
}