package com.chat.repository;

import com.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    // 统计总消息数
    @Query("SELECT COUNT(c) FROM ChatMessage c")
    Long countTotalMessages();
    
    // 统计用户数
    @Query("SELECT COUNT(DISTINCT c.userId) FROM ChatMessage c")
    Long countDistinctUsers();
    
    // 获取最早时间
    @Query("SELECT MIN(c.msgTime) FROM ChatMessage c")
    LocalDateTime getMinTime();
    
    // 获取最晚时间
    @Query("SELECT MAX(c.msgTime) FROM ChatMessage c")
    LocalDateTime getMaxTime();
    
    // 每日消息统计
    @Query(value = "SELECT DATE(msg_time) as stat_date, COUNT(*) as message_count " +
                   "FROM chat_message " +
                   "GROUP BY DATE(msg_time) " +
                   "ORDER BY stat_date", nativeQuery = true)
    List<Object[]> getDailyStats();
    
    // 消息类型统计
    @Query(value = "SELECT message_type, COUNT(*) as message_count " +
                   "FROM chat_message " +
                   "GROUP BY message_type", nativeQuery = true)
    List<Object[]> getTypeStats();
    
    // 性别统计
    @Query(value = "SELECT user_gender, COUNT(*) as message_count " +
                   "FROM chat_message " +
                   "WHERE user_gender IS NOT NULL " +
                   "GROUP BY user_gender", nativeQuery = true)
    List<Object[]> getGenderStats();
    
    // 平台统计
    @Query(value = "SELECT platform, COUNT(*) as message_count " +
                   "FROM chat_message " +
                   "WHERE platform IS NOT NULL " +
                   "GROUP BY platform", nativeQuery = true)
    List<Object[]> getPlatformStats();
    
    // 年龄统计（按年龄段分组）
    @Query(value = "SELECT " +
                   "CASE " +
                   "  WHEN user_age < 18 THEN '0-17' " +
                   "  WHEN user_age < 25 THEN '18-24' " +
                   "  WHEN user_age < 35 THEN '25-34' " +
                   "  WHEN user_age < 45 THEN '35-44' " +
                   "  WHEN user_age < 55 THEN '45-54' " +
                   "  ELSE '55+' " +
                   "END as age_group, " +
                   "COUNT(*) as message_count " +
                   "FROM chat_message " +
                   "WHERE user_age IS NOT NULL " +
                   "GROUP BY age_group " +
                   "ORDER BY age_group", nativeQuery = true)
    List<Object[]> getAgeStats();
    
    // 小时统计
    @Query(value = "SELECT HOUR(msg_time) as hour, COUNT(*) as message_count " +
                   "FROM chat_message " +
                   "GROUP BY HOUR(msg_time) " +
                   "ORDER BY hour", nativeQuery = true)
    List<Object[]> getHourStats();
    
    // 分页查询
    Page<ChatMessage> findAll(Pageable pageable);
    
    // 使用TRUNCATE TABLE清空数据（性能更好，适合大数据量）
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE chat_message", nativeQuery = true)
    void truncateTable();
}

