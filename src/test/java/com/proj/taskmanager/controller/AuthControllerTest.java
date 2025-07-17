package com.proj.taskmanager.controller;

import com.proj.taskmanager.dto.TokenResponse;
import com.proj.taskmanager.dto.UserDto;
import com.proj.taskmanager.enums.UserRole;
import com.proj.taskmanager.model.Role;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private IUserService userService;

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenService tokenService;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;

    private CreateUserReq createUserReq;
    private LoginReq loginReq;
    private RefreshTokenReq refreshTokenReq;
    private User user;
    private UserDto userDto;
    private Role role;
    private UserToken userToken;
    private UserDetailsImpl userDetails;
    private TokenResponse tokenResponse;

    @BeforeEach
    void setUp() {
        // Setup CreateUserReq
        createUserReq = new CreateUserReq(
                "John",
                "Doe",
                "john.doe@example.com",
                "Password123!",
                UserRole.USER
        );

        // Setup LoginReq
        loginReq = new LoginReq(
                "john.doe@example.com",
                "Password123!"
        );

        // Setup RefreshTokenReq
        refreshTokenReq = new RefreshTokenReq("refresh-token-123");

        // Setup User
        user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setPassword("encodedPassword123");

        // Setup Role
        role = new Role(UserRole.USER);
        role.setId(1L);
        user.setRoles(Arrays.asList(role));

        // Setup UserDto
        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setFirstName("John");
        userDto.setLastName("Doe");
        userDto.setEmail("john.doe@example.com");

        // Setup UserToken
        userToken = new UserToken();
        userToken.setTokenId("token-id-123");
        userToken.setUserId(1L);
        userToken.setDeviceInfo("DESKTOP");
        userToken.setIpAddress("192.168.1.1");
        userToken.setUserAgent("Mozilla/5.0");

        // Setup UserDetailsImpl
        userDetails = new UserDetailsImpl(user);

        // Setup TokenResponse
        tokenResponse = new TokenResponse(
                "access-token-123",
                "refresh-token-123",
                "Bearer",
                1800,
                604800
        );
    }

    @Test
    void createUser_Success() {
        // Given
        when(userService.createUser(createUserReq)).thenReturn(user);
        when(userService.convertUserToDto(user)).thenReturn(userDto);

        // When
        ResponseEntity<ApiResponse> response = authController.createUser(createUserReq);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Register Success!", response.getBody().getMessage());
        assertEquals(userDto, response.getBody().getData());

        verify(userService).createUser(createUserReq);
        verify(userService).convertUserToDto(user);
    }

    @Test
    void createUser_UserAlreadyExists_ReturnsConflict() {
        // Given
        when(userService.createUser(createUserReq))
                .thenThrow(new RuntimeException("User with this email already exists"));

        // When
        ResponseEntity<ApiResponse> response = authController.createUser(createUserReq);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User with this email already exists", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(userService).createUser(createUserReq);
        verify(userService, never()).convertUserToDto(any());
    }

    @Test
    void login_Success() {
        // Given
        Collection<String> roles = Arrays.asList("USER");
        
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getHeader("X-Real-IP")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(tokenService.createToken(eq(user), anyString(), anyString(), anyString()))
                .thenReturn(userToken);
        when(jwtUtil.generateAccessToken(anyString(), anyLong(), anyList(), anyString()))
                .thenReturn("access-token-123");
        when(jwtUtil.generateRefreshToken(anyString())).thenReturn("refresh-token-123");
        doNothing().when(tokenService).saveAccessToken(anyString(), anyString());
        doNothing().when(tokenService).saveRefreshToken(anyString(), anyString());

        // When
        ResponseEntity<ApiResponse> response = authController.login(loginReq, httpRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Login Success!", response.getBody().getMessage());
        
        TokenResponse responseData = (TokenResponse) response.getBody().getData();
        assertNotNull(responseData);
        assertEquals("access-token-123", responseData.getAccessToken());
        assertEquals("refresh-token-123", responseData.getRefreshToken());
        assertEquals("Bearer", responseData.getTokenType());
        assertEquals(1800, responseData.getExpiresIn());
        assertEquals(604800, responseData.getRefreshExpiresIn());

        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService).createToken(eq(user), anyString(), anyString(), anyString());
        verify(jwtUtil).generateAccessToken(anyString(), anyLong(), anyList(), anyString());
        verify(jwtUtil).generateRefreshToken(anyString());
        verify(tokenService).saveAccessToken(anyString(), anyString());
        verify(tokenService).saveRefreshToken(anyString(), anyString());
    }

    @Test
    void login_BadCredentials_ReturnsUnauthorized() {
        // Given
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When
        ResponseEntity<ApiResponse> response = authController.login(loginReq, httpRequest);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Bad credentials", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService, never()).createToken(any(), anyString(), anyString(), anyString());
    }

    @Test
    void login_UserNotFound_ReturnsUnauthorized() {
        // Given
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new UsernameNotFoundException("User not found"));

        // When
        ResponseEntity<ApiResponse> response = authController.login(loginReq, httpRequest);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User not found", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService, never()).createToken(any(), anyString(), anyString(), anyString());
    }

    @Test
    void login_GeneralException_ReturnsInternalServerError() {
        // Given
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When
        ResponseEntity<ApiResponse> response = authController.login(loginReq, httpRequest);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().startsWith("Authentication failed:"));
        assertNull(response.getBody().getData());

        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService, never()).createToken(any(), anyString(), anyString(), anyString());
    }

    @Test
    void logout_Success() {
        // Given
        String authHeader = "Bearer valid-token-123";
        when(jwtUtil.extractTokenId("valid-token-123")).thenReturn("token-id-123");
        doNothing().when(tokenService).revokeToken("token-id-123", "User logout");

        // When
        ResponseEntity<ApiResponse> response = authController.logout(authHeader);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Logout Success!", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(jwtUtil).extractTokenId("valid-token-123");
        verify(tokenService).revokeToken("token-id-123", "User logout");
    }

    @Test
    void logout_InvalidToken_ReturnsBadRequest() {
        // Given
        String authHeader = "Bearer invalid-token";
        when(jwtUtil.extractTokenId("invalid-token")).thenThrow(new RuntimeException("Invalid token"));

        // When
        ResponseEntity<ApiResponse> response = authController.logout(authHeader);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid token", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(jwtUtil).extractTokenId("invalid-token");
        verify(tokenService, never()).revokeToken(anyString(), anyString());
    }

    @Test
    void logout_NoBearerToken_ReturnsBadRequest() {
        // Given
        String authHeader = "InvalidHeader";

        // When
        ResponseEntity<ApiResponse> response = authController.logout(authHeader);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid token", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(jwtUtil, never()).extractTokenId(anyString());
        verify(tokenService, never()).revokeToken(anyString(), anyString());
    }

    @Test
    void logout_NullHeader_ReturnsBadRequest() {
        // Given
        String authHeader = null;

        // When
        ResponseEntity<ApiResponse> response = authController.logout(authHeader);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid token", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(jwtUtil, never()).extractTokenId(anyString());
        verify(tokenService, never()).revokeToken(anyString(), anyString());
    }

    @Test
    void refreshToken_Success() {
        // Given
        Collection<String> roles = Arrays.asList("USER");
        
        when(jwtUtil.validateToken("refresh-token-123")).thenReturn(true);
        when(tokenService.getTokenByRefreshToken("refresh-token-123")).thenReturn(userToken);
        when(userService.getUserById(1L)).thenReturn(user);
        when(jwtUtil.generateAccessToken(anyString(), anyLong(), anyList(), anyString()))
                .thenReturn("new-access-token-123");
        doNothing().when(tokenService).saveAccessToken(anyString(), anyString());

        // When
        ResponseEntity<ApiResponse> response = authController.refreshToken(refreshTokenReq);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Token refreshed successfully", response.getBody().getMessage());
        
        TokenResponse responseData = (TokenResponse) response.getBody().getData();
        assertNotNull(responseData);
        assertEquals("new-access-token-123", responseData.getAccessToken());
        assertEquals("refresh-token-123", responseData.getRefreshToken());
        assertEquals("Bearer", responseData.getTokenType());
        assertEquals(1800, responseData.getExpiresIn());
        assertEquals(604800, responseData.getRefreshExpiresIn());

        verify(jwtUtil).validateToken("refresh-token-123");
        verify(tokenService).getTokenByRefreshToken("refresh-token-123");
        verify(userService).getUserById(1L);
        verify(jwtUtil).generateAccessToken(anyString(), anyLong(), anyList(), anyString());
        verify(tokenService).saveAccessToken(anyString(), anyString());
    }

    @Test
    void refreshToken_InvalidRefreshToken_ReturnsUnauthorized() {
        // Given
        when(jwtUtil.validateToken("refresh-token-123")).thenReturn(false);

        // When
        ResponseEntity<ApiResponse> response = authController.refreshToken(refreshTokenReq);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid refresh token", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(jwtUtil).validateToken("refresh-token-123");
        verify(tokenService, never()).getTokenByRefreshToken(anyString());
    }

    @Test
    void refreshToken_RefreshTokenNotFound_ReturnsUnauthorized() {
        // Given
        when(jwtUtil.validateToken("refresh-token-123")).thenReturn(true);
        when(tokenService.getTokenByRefreshToken("refresh-token-123")).thenReturn(null);

        // When
        ResponseEntity<ApiResponse> response = authController.refreshToken(refreshTokenReq);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Refresh token not found", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(jwtUtil).validateToken("refresh-token-123");
        verify(tokenService).getTokenByRefreshToken("refresh-token-123");
        verify(userService, never()).getUserById(anyLong());
    }

    @Test
    void refreshToken_GeneralException_ReturnsUnauthorized() {
        // Given
        when(jwtUtil.validateToken("refresh-token-123")).thenThrow(new RuntimeException("Token validation failed"));

        // When
        ResponseEntity<ApiResponse> response = authController.refreshToken(refreshTokenReq);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Failed to refresh token", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(jwtUtil).validateToken("refresh-token-123");
        verify(tokenService, never()).getTokenByRefreshToken(anyString());
    }

    @Test
    void getCurrentUser_Success() {
        // Given
        String authHeader = "Bearer valid-token-123";
        when(jwtUtil.validateToken("valid-token-123")).thenReturn(true);
        when(jwtUtil.extractUserId("valid-token-123")).thenReturn(1L);
        when(userService.getUserById(1L)).thenReturn(user);
        when(userService.convertUserToDto(user)).thenReturn(userDto);

        // When
        ResponseEntity<ApiResponse> response = authController.getCurrentUser(authHeader);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User details retrieved successfully", response.getBody().getMessage());
        assertEquals(userDto, response.getBody().getData());

        verify(jwtUtil).validateToken("valid-token-123");
        verify(jwtUtil).extractUserId("valid-token-123");
        verify(userService).getUserById(1L);
        verify(userService).convertUserToDto(user);
    }

    @Test
    void getCurrentUser_InvalidToken_ReturnsUnauthorized() {
        // Given
        String authHeader = "Bearer invalid-token";
        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        // When
        ResponseEntity<ApiResponse> response = authController.getCurrentUser(authHeader);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid or expired token", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(jwtUtil).validateToken("invalid-token");
        verify(jwtUtil, never()).extractUserId(anyString());
        verify(userService, never()).getUserById(anyLong());
    }

    @Test
    void getCurrentUser_Exception_ReturnsUnauthorized() {
        // Given
        String authHeader = "Bearer valid-token-123";
        when(jwtUtil.validateToken("valid-token-123")).thenThrow(new RuntimeException("Token processing failed"));

        // When
        ResponseEntity<ApiResponse> response = authController.getCurrentUser(authHeader);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid token", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(jwtUtil).validateToken("valid-token-123");
        verify(jwtUtil, never()).extractUserId(anyString());
        verify(userService, never()).getUserById(anyLong());
    }

    @Test
    void getCurrentUser_NoToken_ReturnsUnauthorized() {
        // Given
        String authHeader = null;

        // When
        ResponseEntity<ApiResponse> response = authController.getCurrentUser(authHeader);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("No token provided", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(jwtUtil, never()).validateToken(anyString());
        verify(userService, never()).getUserById(anyLong());
    }

    @Test
    void getCurrentUser_NoBearerPrefix_ReturnsUnauthorized() {
        // Given
        String authHeader = "InvalidHeader";

        // When
        ResponseEntity<ApiResponse> response = authController.getCurrentUser(authHeader);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("No token provided", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(jwtUtil, never()).validateToken(anyString());
        verify(userService, never()).getUserById(anyLong());
    }

    @Test
    void getUserSessions_Success() {
        // Given
        String authHeader = "Bearer valid-token-123";
        List<UserToken> sessions = Arrays.asList(userToken);
        
        when(jwtUtil.validateToken("valid-token-123")).thenReturn(true);
        when(jwtUtil.extractUserId("valid-token-123")).thenReturn(1L);
        when(tokenService.getUserActiveTokens(1L)).thenReturn(sessions);

        // When
        ResponseEntity<ApiResponse> response = authController.getUserSessions(authHeader);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User sessions retrieved successfully", response.getBody().getMessage());
        assertEquals(sessions, response.getBody().getData());

        verify(jwtUtil).validateToken("valid-token-123");
        verify(jwtUtil).extractUserId("valid-token-123");
        verify(tokenService).getUserActiveTokens(1L);
    }

    @Test
    void getUserSessions_InvalidToken_ReturnsUnauthorized() {
        // Given
        String authHeader = "Bearer invalid-token";
        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        // When
        ResponseEntity<ApiResponse> response = authController.getUserSessions(authHeader);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid or expired token", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(jwtUtil).validateToken("invalid-token");
        verify(jwtUtil, never()).extractUserId(anyString());
        verify(tokenService, never()).getUserActiveTokens(anyLong());
    }

    @Test
    void getUserSessions_Exception_ReturnsUnauthorized() {
        // Given
        String authHeader = "Bearer valid-token-123";
        when(jwtUtil.validateToken("valid-token-123")).thenThrow(new RuntimeException("Token processing failed"));

        // When
        ResponseEntity<ApiResponse> response = authController.getUserSessions(authHeader);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid token", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(jwtUtil).validateToken("valid-token-123");
        verify(jwtUtil, never()).extractUserId(anyString());
        verify(tokenService, never()).getUserActiveTokens(anyLong());
    }

    @Test
    void getUserSessions_NoToken_ReturnsUnauthorized() {
        // Given
        String authHeader = null;

        // When
        ResponseEntity<ApiResponse> response = authController.getUserSessions(authHeader);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("No token provided", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(jwtUtil, never()).validateToken(anyString());
        verify(tokenService, never()).getUserActiveTokens(anyLong());
    }

    @Test
    void getUserSessions_NoBearerPrefix_ReturnsUnauthorized() {
        // Given
        String authHeader = "InvalidHeader";

        // When
        ResponseEntity<ApiResponse> response = authController.getUserSessions(authHeader);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("No token provided", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(jwtUtil, never()).validateToken(anyString());
        verify(tokenService, never()).getUserActiveTokens(anyLong());
    }
}