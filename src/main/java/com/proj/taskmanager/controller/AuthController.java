package com.proj.taskmanager.controller;

import com.proj.taskmanager.dto.TokenResponse;
import com.proj.taskmanager.dto.UserDto;
import com.proj.taskmanager.model.User;
import com.proj.taskmanager.model.UserToken;
import com.proj.taskmanager.request.auth.LoginReq;
import com.proj.taskmanager.request.auth.RefreshTokenReq;
import com.proj.taskmanager.request.user.CreateUserReq;
import com.proj.taskmanager.response.ApiResponse;
import com.proj.taskmanager.security.JwtUtil;
import com.proj.taskmanager.security.UserDetailsImpl;
import com.proj.taskmanager.service.token.TokenService;
import com.proj.taskmanager.service.user.IUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

import static org.springframework.http.HttpStatus.CONFLICT;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/auth")
public class AuthController {
    private final IUserService userService;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> createUser(@Valid @RequestBody CreateUserReq request) {
        try {
            User user = userService.createUser(request);
            UserDto userDto = userService.convertUserToDto(user);
            return ResponseEntity.ok(new ApiResponse("Register Success!", userDto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(CONFLICT).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginReq request, HttpServletRequest httpRequest) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );


            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            User user = userDetails.getUser();

            Collection<String> roles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(java.util.stream.Collectors.toList());

            // Create token record in database
            String deviceInfo = getDeviceInfo(httpRequest);
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            UserToken userToken = tokenService.createToken(user, deviceInfo, ipAddress, userAgent);

            // Generate tokens
            String accessToken = jwtUtil.generateAccessToken(userDetails.getUsername(), user.getId(), roles, userToken.getTokenId());
            String refreshToken = jwtUtil.generateRefreshToken(userToken.getTokenId());

            // Save tokens to database
            tokenService.saveAccessToken(userToken.getTokenId(), accessToken);
            tokenService.saveRefreshToken(userToken.getTokenId(), refreshToken);
            
            TokenResponse tokenResponse = new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                1800, // 30 minutes in seconds
                604800 // 7 days in seconds
            );
            
            return ResponseEntity.ok(new ApiResponse("Login Success!", tokenResponse));
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.status(401).body(new ApiResponse("Bad credentials", null));
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            return ResponseEntity.status(401).body(new ApiResponse("User not found", null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse("Authentication failed: " + e.getMessage(), null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                String tokenId = jwtUtil.extractTokenId(token);

                tokenService.revokeToken(tokenId, "User logout");
                
                SecurityContextHolder.clearContext();
                return ResponseEntity.ok(new ApiResponse("Logout Success!", null));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new ApiResponse("Invalid token", null));
            }
        }
        return ResponseEntity.badRequest().body(new ApiResponse("Invalid token", null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refreshToken(@Valid @RequestBody RefreshTokenReq request) {
        try {
            if (!jwtUtil.validateToken(request.refreshToken())) {
                return ResponseEntity.status(401).body(new ApiResponse("Invalid refresh token", null));
            }
            
            UserToken userToken = tokenService.getTokenByRefreshToken(request.refreshToken());
            if (userToken == null) {
                return ResponseEntity.status(401).body(new ApiResponse("Refresh token not found", null));
            }
            
            User user = userService.getUserById(userToken.getUserId());
            Collection<String> roles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(java.util.stream.Collectors.toList());

            String newAccessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getId(), roles, userToken.getTokenId());
            
            tokenService.saveAccessToken(userToken.getTokenId(), newAccessToken);
            
            TokenResponse tokenResponse = new TokenResponse(
                newAccessToken,
                request.refreshToken(),
                "Bearer",
                1800, // 30 minutes in seconds
                604800 // 7 days in seconds
            );
            
            return ResponseEntity.ok(new ApiResponse("Token refreshed successfully", tokenResponse));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(new ApiResponse("Failed to refresh token", null));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                if (!jwtUtil.validateToken(token)) {
                    return ResponseEntity.status(401).body(new ApiResponse("Invalid or expired token", null));
                }
                
                Long userId = jwtUtil.extractUserId(token);

                User user = userService.getUserById(userId);
                UserDto userDto = userService.convertUserToDto(user);
                
                return ResponseEntity.ok(new ApiResponse("User details retrieved successfully", userDto));
            } catch (Exception e) {
                return ResponseEntity.status(401).body(new ApiResponse("Invalid token", null));
            }
        }
        return ResponseEntity.status(401).body(new ApiResponse("No token provided", null));
    }
    
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse> getUserSessions(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                if (!jwtUtil.validateToken(token)) {
                    return ResponseEntity.status(401).body(new ApiResponse("Invalid or expired token", null));
                }
                
                Long userId = jwtUtil.extractUserId(token);
                var sessions = tokenService.getUserActiveTokens(userId);
                
                return ResponseEntity.ok(new ApiResponse("User sessions retrieved successfully", sessions));
            } catch (Exception e) {
                return ResponseEntity.status(401).body(new ApiResponse("Invalid token", null));
            }
        }
        return ResponseEntity.status(401).body(new ApiResponse("No token provided", null));
    }
    
    // Helper methods
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
    
    private String getDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            if (userAgent.contains("Mobile")) {
                return "MOBILE";
            } else if (userAgent.contains("Tablet")) {
                return "TABLET";
            } else {
                return "DESKTOP";
            }
        }
        return "UNKNOWN";
    }
}
