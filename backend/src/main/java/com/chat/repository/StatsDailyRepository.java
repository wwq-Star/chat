package com.chat.repository;

import com.chat.entity.StatsDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface StatsDailyRepository extends JpaRepository<StatsDaily, Long> {
    Optional<StatsDaily> findByStatDate(LocalDate statDate);
}

