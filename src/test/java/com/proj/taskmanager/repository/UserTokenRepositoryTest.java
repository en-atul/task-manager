package com.proj.taskmanager.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTokenRepositoryTest {

    @Test
    void findByUserIdAndRevokedFalse() {
    }

    @Test
    void findByUserIdAndRevokedFalseOrderByCreatedAtDesc() {
    }

    @Test
    void findByAccessTokenExpiresAtBeforeAndRevokedFalse() {
    }

    @Test
    void findByRefreshTokenExpiresAtBeforeAndRevokedFalse() {
    }

    @Test
    void findByRefreshTokenHashAndRevokedFalse() {
    }

    @Test
    void revokeAllUserTokens() {
    }

    @Test
    void deleteExpiredTokens() {
    }

    @Test
    void countActiveTokensByUserId() {
    }
}