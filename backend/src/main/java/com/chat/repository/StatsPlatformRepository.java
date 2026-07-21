package com.chat.repository;

import com.chat.entity.StatsPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatsPlatformRepository extends JpaRepository<StatsPlatform, Long> {
    Optional<StatsPlatform> findByPlatform(String platform);
}

