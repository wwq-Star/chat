package com.chat.repository;

import com.chat.entity.StatsHour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatsHourRepository extends JpaRepository<StatsHour, Long> {
    Optional<StatsHour> findByHour(Integer hour);
}

