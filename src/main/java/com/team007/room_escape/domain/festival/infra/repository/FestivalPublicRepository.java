package com.team007.room_escape.domain.festival.infra.repository;

import com.team007.room_escape.domain.festival.infra.entity.FestivalApply;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestivalPublicRepository extends JpaRepository<FestivalApply, UUID> {
}
