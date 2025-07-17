package com.proj.taskmanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_tokens")
@Getter
@Setter
@NoArgsConstructor
public class UserToken {

    @Id
    private String tokenId;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "access_token_hash")
    private String accessTokenHash;
    
    @Column(name = "refresh_token_hash")
    private String refreshTokenHash;
    
    @Column(name = "device_info")
    private String deviceInfo;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "access_token_expires_at")
    private LocalDateTime accessTokenExpiresAt;
    
    @Column(name = "refresh_token_expires_at")
    private LocalDateTime refreshTokenExpiresAt;
    
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;
    
    @Column(name = "is_revoked")
    private boolean revoked;
    
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
    @Column(name = "revoked_reason")
    private String revokedReason;
    
    @Column(name = "session_type")
    private String sessionType = "WEB";
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastAccessedAt = LocalDateTime.now();
    }
} 