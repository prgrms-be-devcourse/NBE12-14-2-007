package com.team007.room_escape.domain.festival.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team007.room_escape.domain.festival.infra.entity.Festival;

public interface FestivalPublicRepository extends JpaRepository<Festival, Long> {
    
}
