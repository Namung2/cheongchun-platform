package com.cheongchun.backend.controller;

import com.cheongchun.backend.dto.response.ApiResponse;
import com.cheongchun.backend.dto.response.SessionStatsResponse;
import com.cheongchun.backend.entity.RefreshToken;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.service.AuthService;
import com.cheongchun.backend.service.RefreshTokenService;
import com.cheongchun.backend.util.JwtUtil;
import com.cheongchun.backend.util.ControllerUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SessionController {

    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public SessionController(AuthService authService, JwtUtil jwtUtil, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * JWT 토큰 갱신
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 쿠키에서 리프레시 토큰 추출
            String refreshToken = extractRefreshTokenFromCookie(request);

            if (refreshToken == null) {
                ApiResponse<Void> errorResponse = ApiResponse.error(
                    "REFRESH_TOKEN_MISSING", 
                    "리프레시 토큰이 필요합니다", 
                    "다시 로그인해주세요"
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // 리프레시 토큰 검증 및 새 토큰 발급
            Optional<RefreshToken> refreshTokenOpt = refreshTokenService.findByToken(refreshToken);
            if (refreshTokenOpt.isEmpty()) {
                // 만료된 리프레시 토큰 쿠키 삭제
                clearAuthCookies(response);
                
                ApiResponse<Void> errorResponse = ApiResponse.error(
                    "REFRESH_TOKEN_EXPIRED", 
                    "리프레시 토큰이 만료되었습니다", 
                    "다시 로그인해주세요"
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // 토큰 유효성 검증
            RefreshToken validRefreshToken;
            try {
                validRefreshToken = refreshTokenService.verifyExpiration(refreshTokenOpt.get());
            } catch (RuntimeException e) {
                // 만료된 리프레시 토큰 쿠키 삭제
                clearAuthCookies(response);
                
                ApiResponse<Void> errorResponse = ApiResponse.error(
                    "REFRESH_TOKEN_EXPIRED", 
                    "리프레시 토큰이 만료되었습니다", 
                    e.getMessage()
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            User user = validRefreshToken.getUser();

            // 새 JWT 토큰 생성
            String newJwt = jwtUtil.generateTokenFromUsername(user.getEmail());

            // 새 리프레시 토큰 생성
            String userAgent = request.getHeader("User-Agent");
            String ipAddress = ControllerUtils.getClientIpAddress(request);
            RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user, userAgent, ipAddress);

            // 기존 리프레시 토큰 무효화
            refreshTokenService.revokeToken(refreshToken);

            // 새 토큰들을 쿠키로 설정
            setAuthCookies(response, newJwt, newRefreshToken.getToken());

            ApiResponse<Void> successResponse = ApiResponse.success("토큰이 갱신되었습니다");
            return ResponseEntity.ok(successResponse);

        } catch (Exception e) {
            logger.error("토큰 갱신 중 오류 발생", e);
            ApiResponse<Void> errorResponse = ApiResponse.error(
                "TOKEN_REFRESH_ERROR", 
                "토큰 갱신 중 오류가 발생했습니다", 
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 현재 디바이스 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User user) {
                
                // 쿠키에서 리프레시 토큰 추출
                String refreshTokenStr = extractRefreshTokenFromCookie(request);
                
                // 서버에서 토큰 무효화 (특정 디바이스만)
                authService.logout(user, refreshTokenStr);
                
                // 쿠키 삭제
                clearAuthCookies(response);
                
                ApiResponse<Void> successResponse = ApiResponse.success("성공적으로 로그아웃되었습니다");
                return ResponseEntity.ok(successResponse);
            } else {
                ApiResponse<Void> errorResponse = ApiResponse.error(
                    "LOGOUT_ERROR", 
                    "로그아웃 실패", 
                    "인증되지 않은 사용자입니다"
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
        } catch (Exception e) {
            logger.error("로그아웃 중 오류 발생", e);
            ApiResponse<Void> errorResponse = ApiResponse.error(
                "LOGOUT_ERROR", 
                "로그아웃 중 오류가 발생했습니다", 
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 모든 디바이스 로그아웃
     */
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutFromAllDevices(HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User user) {
                
                // 모든 디바이스에서 로그아웃
                authService.logoutFromAllDevices(user);
                
                // 현재 디바이스의 쿠키도 삭제
                clearAuthCookies(response);
                
                ApiResponse<Void> successResponse = ApiResponse.success("모든 디바이스에서 로그아웃되었습니다");
                
                return ResponseEntity.ok(successResponse);
            } else {
                ApiResponse<Void> errorResponse = ApiResponse.error(
                    "LOGOUT_ERROR", 
                    "로그아웃 실패", 
                    "인증되지 않은 사용자입니다"
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
        } catch (Exception e) {
            logger.error("전체 로그아웃 중 오류 발생", e);
            ApiResponse<Void> errorResponse = ApiResponse.error(
                "LOGOUT_ERROR", 
                "로그아웃 중 오류가 발생했습니다", 
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 활성 세션 조회
     */
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<SessionStatsResponse>> getSessions() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User user) {
                
                RefreshTokenService.RefreshTokenStats stats = authService.getUserTokenStats(user);
                
                SessionStatsResponse sessionStats = new SessionStatsResponse(
                    stats.getActiveTokens(),
                    stats.getTotalTokens(),
                    null // lastAccessTime - RefreshTokenStats에 없음
                );
                
                ApiResponse<SessionStatsResponse> successResponse = ApiResponse.success(sessionStats, "세션 정보 조회 성공");
                
                return ResponseEntity.ok(successResponse);
            } else {
                ApiResponse<SessionStatsResponse> errorResponse = ApiResponse.error(
                    "SESSIONS_ERROR", 
                    "세션 조회 실패", 
                    "인증되지 않은 사용자입니다"
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
        } catch (Exception e) {
            logger.error("세션 조회 중 오류 발생", e);
            ApiResponse<SessionStatsResponse> errorResponse = ApiResponse.error(
                "SESSIONS_ERROR", 
                "세션 조회 중 오류가 발생했습니다", 
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Helper methods
    
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        // JWT를 HttpOnly 쿠키로 설정 (7일)
        jakarta.servlet.http.Cookie jwtCookie = new jakarta.servlet.http.Cookie("accessToken", accessToken);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true); // HTTPS에서만 전송
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(7 * 24 * 60 * 60); // 7일
        response.addCookie(jwtCookie);

        // 리프레시 토큰을 HttpOnly 쿠키로 설정 (7일)
        jakarta.servlet.http.Cookie refreshCookie = new jakarta.servlet.http.Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7일
        response.addCookie(refreshCookie);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        // JWT 쿠키 삭제
        jakarta.servlet.http.Cookie jwtCookie = new jakarta.servlet.http.Cookie("accessToken", "");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0); // 즉시 만료
        response.addCookie(jwtCookie);

        // 리프레시 토큰 쿠키 삭제
        jakarta.servlet.http.Cookie refreshCookie = new jakarta.servlet.http.Cookie("refreshToken", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0); // 즉시 만료
        response.addCookie(refreshCookie);
    }


}