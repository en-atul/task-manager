package com.proj.taskmanager.security;

import com.proj.taskmanager.service.token.TokenService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${auth.token.jwtSecret}")
    private String jwtSecret;

    @Value("${auth.token.expirationInMils}")
    private int jwtExpirationMs;
    
    @Value("${auth.token.refreshExpirationInMils}")
    private int refreshExpirationMs;
    
    private final TokenService tokenService;
    
    public JwtUtil(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public String generateAccessToken(String username, Long userId, Collection<String> roles, String tokenId) {
        return Jwts.builder()
                .setId(tokenId)
                .setSubject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .claim("type", "ACCESS")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256).compact();
    }
    
    public String generateRefreshToken(String tokenId) {
        return Jwts.builder()
                .setId(tokenId)
                .claim("type", "REFRESH")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256).compact();
    }
    
    // Backward compatibility method
    public String generateToken(String username, Long userId, Collection<String> roles) {
        String tokenId = UUID.randomUUID().toString();
        return generateAccessToken(username, userId, roles, tokenId);
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody().getSubject();
    }

    public String extractTokenId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getId();
    }

    public Long extractUserId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("userId", Long.class);
    }

    @SuppressWarnings("unchecked")
    public Collection<String> extractRoles(String token) {
        return (Collection<String>) Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("roles", Collection.class);
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token).getBody();
            String tokenId = claims.getId();
            String tokenType = claims.get("type", String.class);
            
            // For access tokens, validate against database
            if ("ACCESS".equals(tokenType)) {
                return tokenService.validateAccessToken(tokenId);
            }
            
            // For refresh tokens, validate against database
            if ("REFRESH".equals(tokenType)) {
                return tokenService.validateRefreshToken(token);
            }
            
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredTokens() {
        tokenService.cleanupExpiredTokens();
    }
}
