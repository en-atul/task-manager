package com.proj.taskmanager.repository;

import com.proj.taskmanager.model.UserToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserTokenRepositoryTest {

    @Autowired
    private UserTokenRepository userTokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UserToken activeToken1;
    private UserToken activeToken2;
    private UserToken revokedToken;
    private UserToken expiredAccessToken;
    private UserToken expiredRefreshToken;

    @BeforeEach
    void setUp() {
        // Clear the database before each test
        entityManager.clear();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusDays(7);
        LocalDateTime past = now.minusDays(1);
        
        // Create active tokens with unique IDs
        activeToken1 = createUserToken("token1-" + UUID.randomUUID().toString().substring(0, 8), 1L, "hash1", "refresh1", false, future, future);
        activeToken2 = createUserToken("token2-" + UUID.randomUUID().toString().substring(0, 8), 1L, "hash2", "refresh2", false, future, future);
        
        // Create revoked token with unique ID
        revokedToken = createUserToken("token3-" + UUID.randomUUID().toString().substring(0, 8), 1L, "hash3", "refresh3", true, future, future);
        revokedToken.setRevokedAt(now);
        revokedToken.setRevokedReason("User logout");
        
        // Create expired tokens with unique IDs
        expiredAccessToken = createUserToken("token4-" + UUID.randomUUID().toString().substring(0, 8), 2L, "hash4", "refresh4", false, past, future);
        expiredRefreshToken = createUserToken("token5-" + UUID.randomUUID().toString().substring(0, 8), 2L, "hash5", "refresh5", false, future, past);
    }

    private UserToken createUserToken(String tokenId, Long userId, String accessHash, String refreshHash, 
                                    boolean revoked, LocalDateTime accessExpiry, LocalDateTime refreshExpiry) {
        UserToken token = new UserToken();
        token.setTokenId(tokenId);
        token.setUserId(userId);
        token.setAccessTokenHash(accessHash);
        token.setRefreshTokenHash(refreshHash);
        token.setRevoked(revoked);
        token.setAccessTokenExpiresAt(accessExpiry);
        token.setRefreshTokenExpiresAt(refreshExpiry);
        token.setCreatedAt(LocalDateTime.now());
        token.setLastAccessedAt(LocalDateTime.now());
        token.setDeviceInfo("Test Device");
        token.setIpAddress("127.0.0.1");
        token.setUserAgent("Test User Agent");
        return token;
    }

    @Test
    void findByUserIdAndRevokedFalse_ShouldReturnActiveTokens() {
        // Given
        entityManager.persistAndFlush(activeToken1);
        entityManager.persistAndFlush(activeToken2);
        entityManager.persistAndFlush(revokedToken);

        // When
        List<UserToken> activeTokens = userTokenRepository.findByUserIdAndRevokedFalse(1L);

        // Then
        assertEquals(2, activeTokens.size());
        assertTrue(activeTokens.stream().allMatch(token -> !token.isRevoked()));
        assertTrue(activeTokens.stream().allMatch(token -> token.getUserId().equals(1L)));
    }

    @Test
    void findByUserIdAndRevokedFalse_WhenNoActiveTokens_ShouldReturnEmpty() {
        // Given
        entityManager.persistAndFlush(revokedToken);

        // When
        List<UserToken> activeTokens = userTokenRepository.findByUserIdAndRevokedFalse(1L);

        // Then
        assertTrue(activeTokens.isEmpty());
    }

    @Test
    void findByUserIdAndRevokedFalseOrderByCreatedAtDesc_ShouldReturnOrderedTokens() {
        // Given
        activeToken1.setCreatedAt(LocalDateTime.now().minusHours(1));
        activeToken2.setCreatedAt(LocalDateTime.now());
        
        entityManager.persistAndFlush(activeToken1);
        entityManager.persistAndFlush(activeToken2);

        // When
        List<UserToken> orderedTokens = userTokenRepository.findByUserIdAndRevokedFalseOrderByCreatedAtDesc(1L);

        // Then
        assertEquals(2, orderedTokens.size());
        assertEquals(activeToken2.getTokenId(), orderedTokens.get(0).getTokenId()); // Most recent first
        assertEquals(activeToken1.getTokenId(), orderedTokens.get(1).getTokenId());
    }

    @Test
    void findByAccessTokenExpiresAtBeforeAndRevokedFalse_ShouldReturnExpiredAccessTokens() {
        // Given
        entityManager.persistAndFlush(activeToken1);
        entityManager.persistAndFlush(expiredAccessToken);
        entityManager.persistAndFlush(revokedToken);

        LocalDateTime now = LocalDateTime.now();

        // When
        List<UserToken> expiredTokens = userTokenRepository.findByAccessTokenExpiresAtBeforeAndRevokedFalse(now);

        // Then
        assertEquals(1, expiredTokens.size());
        assertEquals(expiredAccessToken.getTokenId(), expiredTokens.get(0).getTokenId());
        assertTrue(expiredTokens.get(0).getAccessTokenExpiresAt().isBefore(now));
        assertFalse(expiredTokens.get(0).isRevoked());
    }

    @Test
    void findByRefreshTokenExpiresAtBeforeAndRevokedFalse_ShouldReturnExpiredRefreshTokens() {
        // Given
        entityManager.persistAndFlush(activeToken1);
        entityManager.persistAndFlush(expiredRefreshToken);
        entityManager.persistAndFlush(revokedToken);

        LocalDateTime now = LocalDateTime.now();

        // When
        List<UserToken> expiredTokens = userTokenRepository.findByRefreshTokenExpiresAtBeforeAndRevokedFalse(now);

        // Then
        assertEquals(1, expiredTokens.size());
        assertEquals(expiredRefreshToken.getTokenId(), expiredTokens.get(0).getTokenId());
        assertTrue(expiredTokens.get(0).getRefreshTokenExpiresAt().isBefore(now));
        assertFalse(expiredTokens.get(0).isRevoked());
    }

    @Test
    void findByRefreshTokenHashAndRevokedFalse_WhenValidToken_ShouldReturnToken() {
        // Given
        entityManager.persistAndFlush(activeToken1);

        // When
        Optional<UserToken> foundToken = userTokenRepository.findByRefreshTokenHashAndRevokedFalse("refresh1");

        // Then
        assertTrue(foundToken.isPresent());
        assertEquals(activeToken1.getTokenId(), foundToken.get().getTokenId());
        assertEquals("refresh1", foundToken.get().getRefreshTokenHash());
        assertFalse(foundToken.get().isRevoked());
    }

    @Test
    void findByRefreshTokenHashAndRevokedFalse_WhenRevokedToken_ShouldReturnEmpty() {
        // Given
        entityManager.persistAndFlush(revokedToken);

        // When
        Optional<UserToken> foundToken = userTokenRepository.findByRefreshTokenHashAndRevokedFalse("refresh3");

        // Then
        assertFalse(foundToken.isPresent());
    }

    @Test
    void findByRefreshTokenHashAndRevokedFalse_WhenTokenDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<UserToken> foundToken = userTokenRepository.findByRefreshTokenHashAndRevokedFalse("nonexistent");

        // Then
        assertFalse(foundToken.isPresent());
    }

    @Test
    @Rollback(false)
    void revokeAllUserTokens_ShouldRevokeAllActiveTokens() {
        // Given
        entityManager.persistAndFlush(activeToken1);
        entityManager.persistAndFlush(activeToken2);
        entityManager.persistAndFlush(revokedToken); // Already revoked

        LocalDateTime revokedAt = LocalDateTime.now();
        String reason = "Security breach";

        // When
        userTokenRepository.revokeAllUserTokens(1L, revokedAt, reason);
        entityManager.flush();
        entityManager.clear();

        // Then
        UserToken token1After = entityManager.find(UserToken.class, activeToken1.getTokenId());
        UserToken token2After = entityManager.find(UserToken.class, activeToken2.getTokenId());
        UserToken token3After = entityManager.find(UserToken.class, revokedToken.getTokenId());

        assertTrue(token1After.isRevoked());
        assertEquals(revokedAt, token1After.getRevokedAt());
        assertEquals(reason, token1After.getRevokedReason());

        assertTrue(token2After.isRevoked());
        assertEquals(revokedAt, token2After.getRevokedAt());
        assertEquals(reason, token2After.getRevokedReason());

        // Already revoked token should remain unchanged
        assertTrue(token3After.isRevoked());
        assertNotEquals(revokedAt, token3After.getRevokedAt());
    }

    @Test
    @Rollback(false)
    void deleteExpiredTokens_ShouldDeleteExpiredRefreshTokens() {
        // Given
        entityManager.persistAndFlush(activeToken1);
        entityManager.persistAndFlush(expiredRefreshToken);
        entityManager.persistAndFlush(expiredAccessToken);

        LocalDateTime now = LocalDateTime.now();

        // When
        userTokenRepository.deleteExpiredTokens(now);
        entityManager.flush();
        entityManager.clear();

        // Then
        UserToken activeTokenAfter = entityManager.find(UserToken.class, activeToken1.getTokenId());
        UserToken expiredRefreshAfter = entityManager.find(UserToken.class, expiredRefreshToken.getTokenId());
        UserToken expiredAccessAfter = entityManager.find(UserToken.class, expiredAccessToken.getTokenId());

        assertNotNull(activeTokenAfter); // Should still exist
        assertNull(expiredRefreshAfter); // Should be deleted
        assertNotNull(expiredAccessAfter); // Should still exist (only refresh token expiry is checked)
    }

    @Test
    void countActiveTokensByUserId_ShouldReturnCorrectCount() {
        // Given
        entityManager.persistAndFlush(activeToken1);
        entityManager.persistAndFlush(activeToken2);
        entityManager.persistAndFlush(revokedToken);

        // When
        long activeCount = userTokenRepository.countActiveTokensByUserId(1L);

        // Then
        assertEquals(2, activeCount);
    }

    @Test
    void countActiveTokensByUserId_WhenNoActiveTokens_ShouldReturnZero() {
        // Given
        entityManager.persistAndFlush(revokedToken);

        // When
        long activeCount = userTokenRepository.countActiveTokensByUserId(1L);

        // Then
        assertEquals(0, activeCount);
    }

    @Test
    void save_ShouldPersistUserToken() {
        // When
        UserToken savedToken = userTokenRepository.save(activeToken1);

        // Then
        assertNotNull(savedToken.getTokenId());
        assertEquals(1L, savedToken.getUserId());
        assertEquals("hash1", savedToken.getAccessTokenHash());
        assertEquals("refresh1", savedToken.getRefreshTokenHash());
        assertFalse(savedToken.isRevoked());
        
        // Verify it's actually persisted
        UserToken foundToken = entityManager.find(UserToken.class, savedToken.getTokenId());
        assertNotNull(foundToken);
        assertEquals(1L, foundToken.getUserId());
    }

    @Test
    void findById_WhenTokenExists_ShouldReturnToken() {
        // Given
        entityManager.persistAndFlush(activeToken1);

        // When
        Optional<UserToken> foundToken = userTokenRepository.findById(activeToken1.getTokenId());

        // Then
        assertTrue(foundToken.isPresent());
        assertEquals(activeToken1.getTokenId(), foundToken.get().getTokenId());
        assertEquals(1L, foundToken.get().getUserId());
    }

    @Test
    void findById_WhenTokenDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<UserToken> foundToken = userTokenRepository.findById("nonexistent");

        // Then
        assertFalse(foundToken.isPresent());
    }

    @Test
    void findAll_ShouldReturnAllTokens() {
        // Given
        entityManager.persistAndFlush(activeToken1);
        entityManager.persistAndFlush(activeToken2);
        entityManager.persistAndFlush(revokedToken);

        // When
        List<UserToken> allTokens = userTokenRepository.findAll();

        // Then
        assertEquals(3, allTokens.size());
        assertTrue(allTokens.stream().anyMatch(token -> token.getTokenId().equals(activeToken1.getTokenId())));
        assertTrue(allTokens.stream().anyMatch(token -> token.getTokenId().equals(activeToken2.getTokenId())));
        assertTrue(allTokens.stream().anyMatch(token -> token.getTokenId().equals(revokedToken.getTokenId())));
    }

    @Test
    void delete_ShouldRemoveToken() {
        // Given
        entityManager.persistAndFlush(activeToken1);

        // When
        userTokenRepository.delete(activeToken1);
        entityManager.flush();

        // Then
        UserToken deletedToken = entityManager.find(UserToken.class, activeToken1.getTokenId());
        assertNull(deletedToken);
    }

    @Test
    void deleteById_ShouldRemoveToken() {
        // Given
        entityManager.persistAndFlush(activeToken1);

        // When
        userTokenRepository.deleteById(activeToken1.getTokenId());
        entityManager.flush();

        // Then
        UserToken deletedToken = entityManager.find(UserToken.class, activeToken1.getTokenId());
        assertNull(deletedToken);
    }

    @Test
    void existsById_WhenTokenExists_ShouldReturnTrue() {
        // Given
        entityManager.persistAndFlush(activeToken1);

        // When
        boolean exists = userTokenRepository.existsById(activeToken1.getTokenId());

        // Then
        assertTrue(exists);
    }

    @Test
    void existsById_WhenTokenDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = userTokenRepository.existsById("nonexistent");

        // Then
        assertFalse(exists);
    }

    @Test
    void count_ShouldReturnCorrectCount() {
        // Given
        entityManager.persistAndFlush(activeToken1);
        entityManager.persistAndFlush(activeToken2);
        entityManager.persistAndFlush(revokedToken);

        // When
        long count = userTokenRepository.count();

        // Then
        assertEquals(3, count);
    }
}