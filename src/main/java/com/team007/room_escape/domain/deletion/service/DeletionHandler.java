package com.team007.room_escape.domain.deletion.service;

import com.team007.room_escape.domain.deletion.infra.entity.DeletionHistory;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionTargetType;

public interface DeletionHandler {
	DeletionTargetType supports();
	void delete(DeletionHistory history);
	void restore(DeletionHistory history);
}
