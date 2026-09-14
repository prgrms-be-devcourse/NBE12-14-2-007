package com.team007.room_escape.domain.manager.infra.repository;

import com.team007.room_escape.domain.manager.infra.entity.Manager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManagerRepository extends JpaRepository<Manager, UUID> {

	Optional<Manager> findByMember_Id(UUID memberId);

	List<Manager> findAllByOrderByCreatedAtDesc();
}
