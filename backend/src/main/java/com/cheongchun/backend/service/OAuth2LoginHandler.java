package com.cheongchun.backend.service;

import com.cheongchun.backend.security.CustomOAuth2User;
import com.cheongchun.backend.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class OAuth2LoginHandler {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public OAuth2LoginHandler(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = new ObjectMapper();
    }

    public void handleOAuth2Success(Object principal, HttpServletResponse response) throws IOException {
        try {
            if (principal instanceof CustomOAuth2User) {
                CustomOAuth2User customUser = (CustomOAuth2User) principal;
                String jwt = jwtUtil.generateTokenFromUsername(customUser.getUsername());

                // Provider별 리다이렉트 처리
                String provider = customUser.getProviderType().toLowerCase();
                String redirectUrl;
                
                if ("google".equals(provider)) {
                    // Google: 딥링크로 앱 복귀
                    redirectUrl = String.format(
                        "myapp://auth-success?token=%s&userId=%s&email=%s&name=%s",
                        jwt,
                        customUser.getUserId(),
                        java.net.URLEncoder.encode(customUser.getEmail(), "UTF-8"),
                        java.net.URLEncoder.encode(customUser.getUserName(), "UTF-8")
                    );
                } else {
                    // 카카오/네이버: 기존 WebView 방식
                    redirectUrl = String.format(
                        "https://cheongchun-backend-40635111975.asia-northeast3.run.app/api/auth/oauth-success?token=%s&userId=%s&email=%s&name=%s", 
                        jwt,
                        customUser.getUserId(),
                        java.net.URLEncoder.encode(customUser.getEmail(), "UTF-8"),
                        java.net.URLEncoder.encode(customUser.getUserName(), "UTF-8")
                    );
                }
                response.sendRedirect(redirectUrl);
                
            } else {
                // 실패 시에도 리다이렉트
                String errorUrl = "https://cheongchun-backend-40635111975.asia-northeast3.run.app/api/auth/oauth-error?code=unsupported_principal_type&message=" + 
                                java.net.URLEncoder.encode("지원하지 않는 사용자 타입입니다", "UTF-8");
                response.sendRedirect(errorUrl);
            }
        } catch (Exception e) {
            // 오류 시에도 리다이렉트
            String errorUrl = String.format(
                "https://cheongchun-backend-40635111975.asia-northeast3.run.app/api/auth/oauth-error?code=oauth2_processing_error&message=%s", 
                java.net.URLEncoder.encode("OAuth2 로그인 처리 중 오류가 발생했습니다", "UTF-8")
            );
            response.sendRedirect(errorUrl);
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String code, String message, String details) throws IOException {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("success", false);
        errorData.put("error", Map.of(
                "code", code,
                "message", message,
                "details", details
        ));

        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorData));
    }
}