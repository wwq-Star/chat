package com.chat.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
@Data
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "msg_time", nullable = false)
    private LocalDateTime msgTime;
    
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;
    
    @Column(name = "group_id", length = 100)
    private String groupId;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    
    @Column(name = "message_length")
    private Integer messageLength;
    
    @Column(name = "message_type")
    private Integer messageType; // 1-文本，2-图片，3-语音
    
    @Column(name = "user_age")
    private Integer userAge;
    
    @Column(name = "user_gender", length = 10)
    private String userGender;
    
    @Column(name = "platform", length = 50)
    private String platform;
    
    @Column(name = "user_name", length = 100)
    private String userName;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

