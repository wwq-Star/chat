package com.chat.repository;

import com.chat.entity.StatsGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatsGroupRepository extends JpaRepository<StatsGroup, Long> {
    Optional<StatsGroup> findByGroupId(String groupId);
}

