package com.team007.room_escape.domain.festival.infra.repository;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestivalRepository extends JpaRepository<Festival, Long> {
}
