package com.team007.room_escape.domain.festival.service;

import static org.assertj.core.api.Assertions.*;
import com.team007.room_escape.domain.festival.infra.entity.*;
import com.team007.room_escape.global.exception.BusinessException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FestivalDeletionTest {

	@Test
	void authorDeletionKeepsActorAndCannotBeRestored() {
		UUID authorId = UUID.randomUUID();
		Festival festival = Festival.builder().build();
		festival.deleteBy(authorId, FestivalDeletionType.AUTHOR, "작성자 삭제");
		assertThat(festival.isDeleted()).isTrue();
		assertThat(festival.getDeletedBy()).isEqualTo(authorId);
		assertThat(festival.getDeletionReason()).isEqualTo("작성자 삭제");
		assertThatThrownBy(festival::restoreByAdmin).isInstanceOf(BusinessException.class);
	}

	@Test
	void legacyDeletionCannotBeRestoredWithoutKnownActor() {
		Festival festival = Festival.builder().build();
		festival.delete();
		assertThatThrownBy(festival::restoreByAdmin).isInstanceOf(BusinessException.class);
		assertThat(festival.isDeleted()).isTrue();
	}

	@Test
	void adminDeletionRequiresNonBlankReasonAndValidActor() {
		Festival festival = Festival.builder().build();
		assertThatThrownBy(() -> festival.deleteBy(UUID.randomUUID(), FestivalDeletionType.ADMIN, " "))
			.isInstanceOf(BusinessException.class);
		assertThatThrownBy(() -> festival.deleteBy(null, FestivalDeletionType.ADMIN, "사유"))
			.isInstanceOf(BusinessException.class);
		assertThatThrownBy(() -> festival.deleteBy(UUID.randomUUID(), FestivalDeletionType.ADMIN, "가".repeat(501)))
			.isInstanceOf(BusinessException.class);
		assertThat(festival.isDeleted()).isFalse();
	}

	@Test
	void activeEventCannotBeRestored() {
		assertThatThrownBy(() -> Festival.builder().build().restoreByAdmin())
			.isInstanceOf(BusinessException.class);
	}
}
