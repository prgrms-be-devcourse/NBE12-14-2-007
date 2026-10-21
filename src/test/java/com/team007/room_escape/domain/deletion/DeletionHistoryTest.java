package com.team007.room_escape.domain.deletion;

import static org.assertj.core.api.Assertions.*;
import com.team007.room_escape.domain.deletion.infra.entity.*;
import com.team007.room_escape.global.exception.BusinessException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeletionHistoryTest {
	@Test
	void authorDeletionUsesFixedReasonAndNormalizesTargetId() {
		var history = DeletionHistory.record(DeletionTargetType.FESTIVAL, "00012", DeletionAction.DELETE,
			UUID.randomUUID(), DeletionActorType.AUTHOR, DeletionReason.AUTHOR_REQUEST, null);
		assertThat(history.getTargetId()).isEqualTo("12");
		assertThat(history.getReason()).isEqualTo(DeletionReason.AUTHOR_REQUEST);
		assertThat(history.getCreatedAt()).isNotNull();
	}

	@Test
	void otherRequiresExplanationAndRestorationRequiresAdministrator() {
		UUID actor = UUID.randomUUID();
		assertThatThrownBy(() -> DeletionHistory.record(DeletionTargetType.POST, UUID.randomUUID().toString(),
			DeletionAction.DELETE, actor, DeletionActorType.ADMIN, DeletionReason.OTHER, " "))
			.isInstanceOf(BusinessException.class);
		assertThatThrownBy(() -> DeletionHistory.record(DeletionTargetType.FESTIVAL, "12",
			DeletionAction.RESTORE, actor, DeletionActorType.AUTHOR, DeletionReason.ADMIN_RESTORE, null))
			.isInstanceOf(BusinessException.class);
	}

	@Test
	void restoreRecordDoesNotErasePreviousDeleteRecord() {
		UUID actor = UUID.randomUUID();
		var deletion = DeletionHistory.record(DeletionTargetType.FESTIVAL, "12", DeletionAction.DELETE,
			actor, DeletionActorType.ADMIN, DeletionReason.SPAM, "광고 도배");
		var restore = DeletionHistory.record(DeletionTargetType.FESTIVAL, "12", DeletionAction.RESTORE,
			actor, DeletionActorType.ADMIN, DeletionReason.ADMIN_RESTORE, null);
		assertThat(deletion.getAction()).isEqualTo(DeletionAction.DELETE);
		assertThat(deletion.getReasonDetail()).isEqualTo("광고 도배");
		assertThat(restore.getAction()).isEqualTo(DeletionAction.RESTORE);
	}
}
