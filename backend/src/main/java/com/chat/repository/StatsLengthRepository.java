package com.chat.repository;

import com.chat.entity.StatsLength;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatsLengthRepository extends JpaRepository<StatsLength, Long> {
    Optional<StatsLength> findByLengthGroup(String lengthGroup);
}

