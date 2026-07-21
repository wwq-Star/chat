package com.chat.repository;

import com.chat.entity.StatsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatsTypeRepository extends JpaRepository<StatsType, Long> {
    Optional<StatsType> findByMessageType(Integer messageType);
}

