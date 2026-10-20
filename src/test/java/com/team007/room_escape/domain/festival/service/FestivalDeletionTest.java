package com.team007.room_escape.domain.festival.service;

import static org.assertj.core.api.Assertions.*;
import com.team007.room_escape.domain.festival.infra.entity.*;
import com.team007.room_escape.domain.deletion.infra.entity.*;
import com.team007.room_escape.global.exception.BusinessException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FestivalDeletionTest {

	@Test
	void festivalTracksOnlyCurrentDeletionState() {
		Festival festival = Festival.builder().build();
		festival.delete();
		assertThat(festival.isDeleted()).isTrue();
		festival.restore();
		assertThat(festival.isDeleted()).isFalse();
	}

	@Test
	void repeatedDeletionCannotOverwriteDeletionTime() {
		Festival festival = Festival.builder().build();
		festival.delete();
		var deletedAt = festival.getDeletedAt();
		assertThatThrownBy(festival::delete).isInstanceOf(BusinessException.class);
		assertThat(festival.getDeletedAt()).isEqualTo(deletedAt);
		assertThat(festival.isDeleted()).isTrue();
	}

	@Test
	void adminDeletionRequiresNonBlankReasonAndValidActor() {
		assertThatThrownBy(() -> DeletionHistory.record(DeletionTargetType.FESTIVAL, "1", DeletionAction.DELETE,
			UUID.randomUUID(), DeletionActorType.ADMIN, DeletionReason.OTHER, " "))
			.isInstanceOf(BusinessException.class);
		assertThatThrownBy(() -> DeletionHistory.record(DeletionTargetType.FESTIVAL, "1", DeletionAction.DELETE,
			null, DeletionActorType.ADMIN, DeletionReason.OTHER, "사유"))
			.isInstanceOf(BusinessException.class);
		assertThatThrownBy(() -> DeletionHistory.record(DeletionTargetType.FESTIVAL, "1", DeletionAction.DELETE,
			UUID.randomUUID(), DeletionActorType.ADMIN, DeletionReason.OTHER, "가".repeat(501)))
			.isInstanceOf(BusinessException.class);
	}

	@Test
	void activeEventCannotBeRestored() {
		assertThatThrownBy(() -> Festival.builder().build().restore())
			.isInstanceOf(BusinessException.class);
	}
}
