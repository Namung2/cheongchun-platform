package com.cheongchun.backend.util;

import com.cheongchun.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // 보안을 위해 최소 256비트(32바이트) 키 생성
        String secret = jwtProperties.getSecret();
        if (secret.length() < 32) {
            // 키가 너무 짧으면 패딩 추가
            secret = secret + "0".repeat(32 - secret.length());
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Authentication 객체로부터 JWT 토큰 생성
     */
    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        return generateTokenFromUsername(userPrincipal.getUsername());
    }

    /**
     * 사용자명으로부터 JWT 토큰 생성
     */
    public String generateTokenFromUsername(String username) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.getExpiration(), ChronoUnit.MILLIS);

        return Jwts.builder()
                .subject(username)                    // setSubject 대신 subject 사용
                .issuedAt(Date.from(now))             // setIssuedAt 대신 issuedAt 사용
                .expiration(Date.from(expiration))    // setExpiration 대신 expiration 사용
                .signWith(secretKey)                  // SignatureAlgorithm 제거하고 키만 사용
                .compact();
    }

    /**
     * JWT 토큰에서 사용자명 추출
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()            // parserBuilder 대신 parser 사용
                    .verifyWith(secretKey)           // setSigningKey 대신 verifyWith 사용
                    .build()
                    .parseSignedClaims(token)        // parseClaimsJws 대신 parseSignedClaims 사용
                    .getPayload();                   // getBody 대신 getPayload 사용

            return claims.getSubject();
        } catch (JwtException e) {
            logger.error("JWT 토큰에서 사용자명 추출 실패: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * JWT 토큰 유효성 검사
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        } catch (Exception ex) {
            logger.error("JWT token validation error: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * JWT 토큰 만료 여부 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            logger.error("토큰 만료 확인 중 오류: {}", e.getMessage());
            return true; // 오류 시 만료된 것으로 처리
        }
    }

    /**
     * JWT 토큰에서 만료 시간 추출
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getExpiration();
        } catch (JwtException e) {
            logger.error("JWT 토큰에서 만료 시간 추출 실패: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * JWT 토큰에서 발급 시간 추출
     */
    public Date getIssuedAtFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getIssuedAt();
        } catch (JwtException e) {
            logger.error("JWT 토큰에서 발급 시간 추출 실패: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * 토큰의 남은 유효 시간(밀리초) 계산
     */
    public long getTokenRemainingTime(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 토큰 갱신이 필요한지 확인 (만료 30분 전)
     */
    public boolean isTokenNeedRefresh(String token) {
        try {
            long remainingTime = getTokenRemainingTime(token);
            long refreshThreshold = 30 * 60 * 1000; // 30분
            return remainingTime < refreshThreshold && remainingTime > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 디버깅용: 토큰 정보 출력
     */
    public void printTokenInfo(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            logger.info("=== JWT Token Info ===");
            logger.info("Subject: {}", claims.getSubject());
            logger.info("Issued At: {}", claims.getIssuedAt());
            logger.info("Expiration: {}", claims.getExpiration());
            logger.info("Is Expired: {}", claims.getExpiration().before(new Date()));
            logger.info("Remaining Time: {} ms", getTokenRemainingTime(token));
            logger.info("=====================");
        } catch (Exception e) {
            logger.error("토큰 정보 출력 실패: {}", e.getMessage());
        }
    }
}