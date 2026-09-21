package com.team007.room_escape.domain.festival.infra.entity;

import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "festival_accuracy_vote", uniqueConstraints = {
        @UniqueConstraint(name="uk_festival_accuracy_vote_festival_member",
        columnNames = {"festival_id", "member_id"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalAccuracyVote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "festival_id", nullable = false)
    private Festival festival;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false, length = 16)
    private FestivalAccuracyVoteType voteType;

    @Builder
    private FestivalAccuracyVote(
            Festival festival,
            Member member,
            FestivalAccuracyVoteType voteType
    ) {
        this.festival = festival;
        this.member = member;
        this.voteType = voteType;
    }
    public void changeVote(FestivalAccuracyVoteType voteType) {
        this.voteType = voteType;
    }
}
