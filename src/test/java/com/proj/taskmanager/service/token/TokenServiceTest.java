package com.proj.taskmanager.service.token;

import com.proj.taskmanager.model.User;
import com.proj.taskmanager.model.UserToken;
import com.proj.taskmanager.repository.UserTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private UserTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TokenService tokenService;

    private User user;
    private UserToken userToken;
    private final String tokenId = UUID.randomUUID().toString();
    private final String accessToken = "access.token.here";
    private final String refreshToken = "refresh.token.here";
    
    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setPassword("encodedPassword123");

        userToken = new UserToken();
        userToken.setTokenId(tokenId);
        userToken.setUserId(1L);
        userToken.setDeviceInfo("Chrome on Windows");
        userToken.setIpAddress("127.0.0.1");
        userToken.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        userToken.setAccessTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
        userToken.setRefreshTokenExpiresAt(LocalDateTime.now().plusDays(7));
        userToken.setRevoked(false);
    }

    @Test
    void createToken() {
        // Given
        when(tokenRepository.save(any(UserToken.class))).thenReturn(userToken);

        // When
        UserToken result = tokenService.createToken(
                user,
                userToken.getDeviceInfo(),
                userToken.getIpAddress(),
                userToken.getUserAgent()
        );

        // Then
        assertNotNull(result);
        assertEquals(userToken.getTokenId(), result.getTokenId());
        assertEquals(userToken.getUserId(), result.getUserId());
        assertEquals(userToken.getDeviceInfo(), result.getDeviceInfo());
        assertEquals(userToken.getIpAddress(), result.getIpAddress());
        assertEquals(userToken.getUserAgent(), result.getUserAgent());
        assertEquals(userToken.getAccessTokenExpiresAt(), result.getAccessTokenExpiresAt());
        assertEquals(userToken.getRefreshTokenExpiresAt(), result.getRefreshTokenExpiresAt());

        // Verify interactions
        verify(tokenRepository).save(any(UserToken.class));
    }

    @Test
    void saveAccessToken_WhenTokenExists() {
        // Given
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(userToken));
        when(tokenRepository.save(any(UserToken.class))).thenReturn(userToken);

        // When
        tokenService.saveAccessToken(tokenId, accessToken);

        // Then
        verify(tokenRepository).findById(tokenId);
        verify(tokenRepository).save(any(UserToken.class));
    }

    @Test
    void saveAccessToken_WhenTokenDoesNotExist() {
        // Given
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.empty());

        // When
        tokenService.saveAccessToken(tokenId, accessToken);

        // Then
        verify(tokenRepository).findById(tokenId);
        verify(tokenRepository, never()).save(any(UserToken.class));
    }

    @Test
    void saveRefreshToken_WhenTokenExists() {
        // Given
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(userToken));
        when(tokenRepository.save(any(UserToken.class))).thenReturn(userToken);

        // When
        tokenService.saveRefreshToken(tokenId, refreshToken);

        // Then
        verify(tokenRepository).findById(tokenId);
        verify(tokenRepository).save(any(UserToken.class));
    }

    @Test
    void saveRefreshToken_WhenTokenDoesNotExist() {
        // Given
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.empty());

        // When
        tokenService.saveRefreshToken(tokenId, refreshToken);

        // Then
        verify(tokenRepository).findById(tokenId);
        verify(tokenRepository, never()).save(any(UserToken.class));
    }

    @Test
    void validateAccessToken_WhenTokenExistsAndNotRevokedAndNotExpired() {
        // Given
        userToken.setAccessTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
        userToken.setRevoked(false);
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(userToken));
        when(tokenRepository.save(any(UserToken.class))).thenReturn(userToken);

        // When
        boolean result = tokenService.validateAccessToken(tokenId);

        // Then
        assertTrue(result);
        verify(tokenRepository).findById(tokenId);
        verify(tokenRepository).save(any(UserToken.class));
    }

    @Test
    void validateAccessToken_WhenTokenDoesNotExist() {
        // Given
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.empty());

        // When
        boolean result = tokenService.validateAccessToken(tokenId);

        // Then
        assertFalse(result);
        verify(tokenRepository).findById(tokenId);
        verify(tokenRepository, never()).save(any(UserToken.class));
    }

    @Test
    void validateAccessToken_WhenTokenIsRevoked() {
        // Given
        userToken.setRevoked(true);
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(userToken));

        // When
        boolean result = tokenService.validateAccessToken(tokenId);

        // Then
        assertFalse(result);
        verify(tokenRepository).findById(tokenId);
        verify(tokenRepository, never()).save(any(UserToken.class));
    }

    @Test
    void validateAccessToken_WhenTokenIsExpired() {
        // Given
        userToken.setAccessTokenExpiresAt(LocalDateTime.now().minusMinutes(1));
        userToken.setRevoked(false);
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(userToken));

        // When
        boolean result = tokenService.validateAccessToken(tokenId);

        // Then
        assertFalse(result);
        verify(tokenRepository).findById(tokenId);
        verify(tokenRepository, never()).save(any(UserToken.class));
    }

    @Test
    void validateRefreshToken_WhenTokenExistsAndNotExpired() {
        // Given
        userToken.setRefreshTokenExpiresAt(LocalDateTime.now().plusDays(1));
        when(tokenRepository.findByRefreshTokenHashAndRevokedFalse(anyString())).thenReturn(Optional.of(userToken));

        // When
        boolean result = tokenService.validateRefreshToken(refreshToken);

        // Then
        assertTrue(result);
        verify(tokenRepository).findByRefreshTokenHashAndRevokedFalse(anyString());
    }

    @Test
    void validateRefreshToken_WhenTokenDoesNotExist() {
        // Given
        when(tokenRepository.findByRefreshTokenHashAndRevokedFalse(anyString())).thenReturn(Optional.empty());

        // When
        boolean result = tokenService.validateRefreshToken(refreshToken);

        // Then
        assertFalse(result);
        verify(tokenRepository).findByRefreshTokenHashAndRevokedFalse(anyString());
    }

    @Test
    void validateRefreshToken_WhenTokenIsExpired() {
        // Given
        userToken.setRefreshTokenExpiresAt(LocalDateTime.now().minusDays(1));
        when(tokenRepository.findByRefreshTokenHashAndRevokedFalse(anyString())).thenReturn(Optional.of(userToken));

        // When
        boolean result = tokenService.validateRefreshToken(refreshToken);

        // Then
        assertFalse(result);
        verify(tokenRepository).findByRefreshTokenHashAndRevokedFalse(anyString());
    }

    @Test
    void getTokenByRefreshToken_WhenTokenExists() {
        // Given
        when(tokenRepository.findByRefreshTokenHashAndRevokedFalse(anyString())).thenReturn(Optional.of(userToken));

        // When
        UserToken result = tokenService.getTokenByRefreshToken(refreshToken);

        // Then
        assertNotNull(result);
        assertEquals(userToken.getTokenId(), result.getTokenId());
        verify(tokenRepository).findByRefreshTokenHashAndRevokedFalse(anyString());
    }

    @Test
    void getTokenByRefreshToken_WhenTokenDoesNotExist() {
        // Given
        when(tokenRepository.findByRefreshTokenHashAndRevokedFalse(anyString())).thenReturn(Optional.empty());

        // When
        UserToken result = tokenService.getTokenByRefreshToken(refreshToken);

        // Then
        assertNull(result);
        verify(tokenRepository).findByRefreshTokenHashAndRevokedFalse(anyString());
    }

    @Test
    void revokeToken_WhenTokenExists() {
        // Given
        String reason = "User logout";
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(userToken));
        when(tokenRepository.save(any(UserToken.class))).thenReturn(userToken);

        // When
        tokenService.revokeToken(tokenId, reason);

        // Then
        verify(tokenRepository).findById(tokenId);
        verify(tokenRepository).save(any(UserToken.class));
    }

    @Test
    void revokeToken_WhenTokenDoesNotExist() {
        // Given
        String reason = "User logout";
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.empty());

        // When
        tokenService.revokeToken(tokenId, reason);

        // Then
        verify(tokenRepository).findById(tokenId);
        verify(tokenRepository, never()).save(any(UserToken.class));
    }

    @Test
    void revokeAllUserTokens() {
        // Given
        Long userId = 1L;
        String reason = "Security breach";

        // When
        tokenService.revokeAllUserTokens(userId, reason);

        // Then
        verify(tokenRepository).revokeAllUserTokens(eq(userId), any(LocalDateTime.class), eq(reason));
    }

    @Test
    void getUserActiveTokens() {
        // Given
        Long userId = 1L;
        List<UserToken> expectedTokens = Collections.singletonList(userToken);
        when(tokenRepository.findByUserIdAndRevokedFalseOrderByCreatedAtDesc(userId)).thenReturn(expectedTokens);

        // When
        List<UserToken> result = tokenService.getUserActiveTokens(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userToken, result.get(0));
        verify(tokenRepository).findByUserIdAndRevokedFalseOrderByCreatedAtDesc(userId);
    }

    @Test
    void getUserActiveTokenCount() {
        // Given
        Long userId = 1L;
        long expectedCount = 3L;
        when(tokenRepository.countActiveTokensByUserId(userId)).thenReturn(expectedCount);

        // When
        long result = tokenService.getUserActiveTokenCount(userId);

        // Then
        assertEquals(expectedCount, result);
        verify(tokenRepository).countActiveTokensByUserId(userId);
    }

    @Test
    void cleanupExpiredTokens() {
        // When
        tokenService.cleanupExpiredTokens();

        // Then
        verify(tokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
    }
}