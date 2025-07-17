package com.proj.taskmanager.request.auth;

import jakarta.validation.constraints.NotEmpty;

public record RefreshTokenReq(@NotEmpty String refreshToken) {
}