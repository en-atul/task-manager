package com.proj.taskmanager.repository;

import com.proj.taskmanager.model.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, String> {
    
    List<UserToken> findByUserIdAndRevokedFalse(Long userId);
    
    List<UserToken> findByUserIdAndRevokedFalseOrderByCreatedAtDesc(Long userId);
    
    List<UserToken> findByAccessTokenExpiresAtBeforeAndRevokedFalse(LocalDateTime dateTime);
    
    List<UserToken> findByRefreshTokenExpiresAtBeforeAndRevokedFalse(LocalDateTime dateTime);
    
    Optional<UserToken> findByRefreshTokenHashAndRevokedFalse(String refreshTokenHash);
    
    @Modifying
    @Query("UPDATE UserToken t SET t.revoked = true, t.revokedAt = :revokedAt, t.revokedReason = :reason WHERE t.userId = :userId AND t.revoked = false")
    void revokeAllUserTokens(@Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") String reason);
    
    @Modifying
    @Query("DELETE FROM UserToken t WHERE t.refreshTokenExpiresAt < :dateTime")
    void deleteExpiredTokens(@Param("dateTime") LocalDateTime dateTime);
    
    @Query("SELECT COUNT(t) FROM UserToken t WHERE t.userId = :userId AND t.revoked = false")
    long countActiveTokensByUserId(@Param("userId") Long userId);
} 