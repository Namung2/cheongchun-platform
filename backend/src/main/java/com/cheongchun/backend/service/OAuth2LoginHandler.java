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

                Map<String, Object> successData = new HashMap<>();
                successData.put("success", true);
                successData.put("token", jwt);
                successData.put("user", Map.of(
                        "id", customUser.getUserId(),
                        "email", customUser.getEmail(),
                        "name", customUser.getUserName(),
                        "provider", customUser.getProviderType()
                ));
                successData.put("message", "소셜 로그인 성공");

                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(successData));
            } else {
                sendErrorResponse(response, 500, "unsupported_principal_type", 
                    "지원하지 않는 사용자 타입입니다", "Principal type: " + principal.getClass().getName());
            }
        } catch (Exception e) {
            sendErrorResponse(response, 400, "oauth2_processing_error", 
                "OAuth2 로그인 처리 중 오류가 발생했습니다", e.getMessage());
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