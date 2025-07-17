package com.proj.taskmanager.service.token;

import com.proj.taskmanager.model.User;
import com.proj.taskmanager.model.UserToken;
import com.proj.taskmanager.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final UserTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserToken createToken(User user, String deviceInfo, String ipAddress, String userAgent) {
        String tokenId = UUID.randomUUID().toString();

        UserToken userToken = new UserToken();
        userToken.setTokenId(tokenId);
        userToken.setUserId(user.getId());
        userToken.setDeviceInfo(deviceInfo);
        userToken.setIpAddress(ipAddress);
        userToken.setUserAgent(userAgent);
        userToken.setAccessTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
        userToken.setRefreshTokenExpiresAt(LocalDateTime.now().plusDays(7));

        return tokenRepository.save(userToken);
    }

    @Transactional
    public void saveAccessToken(String tokenId, String accessToken) {
        UserToken userToken = tokenRepository.findById(tokenId).orElse(null);
        if (userToken != null) {
            userToken.setAccessTokenHash(hashToken(accessToken));
            tokenRepository.save(userToken);
        }
    }

    @Transactional
    public void saveRefreshToken(String tokenId, String refreshToken) {
        UserToken userToken = tokenRepository.findById(tokenId).orElse(null);
        if (userToken != null) {
            userToken.setRefreshTokenHash(hashToken(refreshToken));
            tokenRepository.save(userToken);
        }
    }

    @Transactional
    public boolean validateAccessToken(String tokenId) {
        UserToken userToken = tokenRepository.findById(tokenId).orElse(null);
        if (userToken == null || userToken.isRevoked()) {
            return false;
        }

        if (LocalDateTime.now().isAfter(userToken.getAccessTokenExpiresAt())) {
            return false;
        }

        userToken.setLastAccessedAt(LocalDateTime.now());
        tokenRepository.save(userToken);

        return true;
    }

    @Transactional
    public boolean validateRefreshToken(String refreshToken) {
        String refreshTokenHash = hashToken(refreshToken);
        UserToken userToken = tokenRepository.findByRefreshTokenHashAndRevokedFalse(refreshTokenHash).orElse(null);

        if (userToken == null) {
            return false;
        }

        return !LocalDateTime.now().isAfter(userToken.getRefreshTokenExpiresAt());
    }

    @Transactional
    public UserToken getTokenByRefreshToken(String refreshToken) {
        String refreshTokenHash = hashToken(refreshToken);
        return tokenRepository.findByRefreshTokenHashAndRevokedFalse(refreshTokenHash).orElse(null);
    }

    @Transactional
    public void revokeToken(String tokenId, String reason) {
        UserToken userToken = tokenRepository.findById(tokenId).orElse(null);
        if (userToken != null) {
            userToken.setRevoked(true);
            userToken.setRevokedAt(LocalDateTime.now());
            userToken.setRevokedReason(reason);
            tokenRepository.save(userToken);
        }
    }

    @Transactional
    public void revokeAllUserTokens(Long userId, String reason) {
        tokenRepository.revokeAllUserTokens(userId, LocalDateTime.now(), reason);
    }

    @Transactional
    public List<UserToken> getUserActiveTokens(Long userId) {
        return tokenRepository.findByUserIdAndRevokedFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public long getUserActiveTokenCount(Long userId) {
        return tokenRepository.countActiveTokensByUserId(userId);
    }

    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.deleteExpiredTokens(now);
    }

    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}