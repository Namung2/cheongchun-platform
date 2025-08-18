package com.cheongchun.backend.service;

import com.cheongchun.backend.entity.RefreshToken;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.repository.UserRepository;
import com.cheongchun.backend.security.CustomOAuth2User;
import com.cheongchun.backend.service.RefreshTokenService;
import com.cheongchun.backend.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
public class OAuth2LoginHandler {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    public OAuth2LoginHandler(JwtUtil jwtUtil, RefreshTokenService refreshTokenService, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
    }

    public void handleOAuth2Success(Object principal, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (principal instanceof CustomOAuth2User) {
                CustomOAuth2User customUser = (CustomOAuth2User) principal;
                
                // 사용자 정보 조회
                Optional<User> userOpt = userRepository.findByEmail(customUser.getEmail());
                if (userOpt.isEmpty()) {
                    throw new RuntimeException("OAuth2 사용자를 찾을 수 없습니다: " + customUser.getEmail());
                }
                
                User user = userOpt.get();
                
                // JWT 토큰 생성
                String jwt = jwtUtil.generateTokenFromUsername(customUser.getUsername());
                
                // 리프레시 토큰 생성
                String userAgent = request.getHeader("User-Agent");
                String ipAddress = getClientIpAddress(request);
                RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, userAgent, ipAddress);
                
                // JWT를 HttpOnly 쿠키로 설정 (7일)
                Cookie jwtCookie = new Cookie("accessToken", jwt);
                jwtCookie.setHttpOnly(true);
                jwtCookie.setSecure(true); // HTTPS에서만 전송
                jwtCookie.setPath("/");
                jwtCookie.setMaxAge(7 * 24 * 60 * 60); // 7일
                // SameSite는 Spring Boot 2.6+ 에서 자동 설정됨
                response.addCookie(jwtCookie);
                
                // 리프레시 토큰을 HttpOnly 쿠키로 설정 (7일)
                Cookie refreshCookie = new Cookie("refreshToken", refreshToken.getToken());
                refreshCookie.setHttpOnly(true);
                refreshCookie.setSecure(true);
                refreshCookie.setPath("/");
                refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7일
                // SameSite는 Spring Boot 2.6+ 에서 자동 설정됨
                response.addCookie(refreshCookie);
                
                // 성공 페이지로 리다이렉트 (토큰은 쿠키에 있으므로 URL 파라미터 불필요)
                String redirectUrl = String.format(
                    "https://cheongchun-backend-40635111975.asia-northeast3.run.app/auth/oauth-success?userId=%s&email=%s&name=%s&provider=%s",
                    user.getId(),
                    java.net.URLEncoder.encode(customUser.getEmail(), "UTF-8"),
                    java.net.URLEncoder.encode(customUser.getUserName(), "UTF-8"),
                    customUser.getProviderType().toLowerCase()
                );
                response.sendRedirect(redirectUrl);
                
            } else {
                // 실패 시에도 리다이렉트
                String errorUrl = "https://cheongchun-backend-40635111975.asia-northeast3.run.app/auth/oauth-error?code=unsupported_principal_type&message=" + 
                                java.net.URLEncoder.encode("지원하지 않는 사용자 타입입니다", "UTF-8");
                response.sendRedirect(errorUrl);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 오류 시에도 리다이렉트
            String errorUrl = String.format(
                "https://cheongchun-backend-40635111975.asia-northeast3.run.app/auth/oauth-error?code=oauth2_processing_error&message=%s", 
                java.net.URLEncoder.encode("OAuth2 로그인 처리 중 오류가 발생했습니다: " + e.getMessage(), "UTF-8")
            );
            response.sendRedirect(errorUrl);
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

}