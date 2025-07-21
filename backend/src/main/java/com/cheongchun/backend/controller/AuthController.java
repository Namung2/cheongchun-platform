package com.cheongchun.backend.controller;

import com.cheongchun.backend.dto.JwtResponse;
import com.cheongchun.backend.dto.LoginRequest;
import com.cheongchun.backend.dto.SignUpRequest;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.repository.UserRepository;
import com.cheongchun.backend.service.AuthService;
import com.cheongchun.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cheongchun.backend.security.CustomOAuth2User;
import com.cheongchun.backend.security.CustomOidcUser;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")  //
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    public AuthController(AuthService authService, JwtUtil jwtUtil,UserRepository userRepository) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    /**
     * 이메일 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        try {
            JwtResponse jwtResponse = authService.registerUser(signUpRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", jwtResponse);
            response.put("message", "회원가입이 완료되었습니다");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", Map.of(
                    "code", "SIGNUP_FAILED",
                    "message", e.getMessage(),
                    "details", "회원가입 중 오류가 발생했습니다"
            ));
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            JwtResponse jwtResponse = authService.authenticateUser(loginRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", jwtResponse);
            response.put("message", "로그인 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", Map.of(
                    "code", "LOGIN_FAILED",
                    "message", e.getMessage(),
                    "details", "이메일 또는 비밀번호가 올바르지 않습니다"
            ));
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    /**
     * 현재 사용자 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getName())) {

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", Map.of(
                    "code", "UNAUTHORIZED",
                    "message", "인증이 필요합니다",
                    "details", "유효한 토큰을 제공해주세요"
            ));
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        try {
            User currentUser = (User) authentication.getPrincipal();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", currentUser);
            response.put("message", "사용자 정보 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (ClassCastException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", Map.of(
                    "code", "INVALID_TOKEN",
                    "message", "잘못된 토큰입니다",
                    "details", "토큰을 다시 발급받아주세요"
            ));
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // JWT는 stateless이므로 서버에서 할 일은 없지만,
        // 나중에 블랙리스트 기능 추가 시 사용

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "로그아웃되었습니다");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * 토큰 갱신 (나중에 구현)
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", Map.of(
                    "code", "REFRESH_TOKEN_REQUIRED",
                    "message", "리프레시 토큰이 필요합니다",
                    "details", "refreshToken 필드를 제공해주세요"
            ));
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.badRequest().body(errorResponse);
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", Map.of(
                "code", "NOT_IMPLEMENTED",
                "message", "리프레시 토큰 기능은 아직 구현되지 않았습니다",
                "details", "추후 업데이트 예정입니다"
        ));
        errorResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(errorResponse);
    }

    /**
     * OAuth2 성공 콜백 (수정된 버전)
     */
    @GetMapping("/oauth2/success")
    public void oAuth2LoginSuccess(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            try {
                // OAuth2User, CustomOAuth2User, DefaultOidcUser 처리
                Object principal = authentication.getPrincipal();
                User user = null;

                if (principal instanceof CustomOAuth2User) {
                    // CustomOAuth2UserService를 통한 로그인
                    CustomOAuth2User customUser = (CustomOAuth2User) principal;
                    String username = customUser.getUsername();
                    user = userRepository.findByUsername(username).orElse(null);
                } else if (principal instanceof CustomOidcUser) {
                    // CustomOidcUserService를 통한 로그인
                    CustomOidcUser customOidcUser = (CustomOidcUser) principal;
                    String username = customOidcUser.getUsername();
                    user = userRepository.findByUsername(username).orElse(null);
                }

                if (user != null) {
                    // JWT 토큰 생성
                    String jwt = jwtUtil.generateTokenFromUsername(user.getUsername());

                    // 성공 응답 데이터 생성
                    Map<String, Object> successData = new HashMap<>();
                    successData.put("success", true);
                    successData.put("token", jwt);
                    successData.put("user", Map.of(
                            "id", user.getId(),
                            "email", user.getEmail(),
                            "name", user.getName(),
                            "provider", user.getProviderType().toString()
                    ));

                    // JSON 응답
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(new ObjectMapper().writeValueAsString(successData));
                } else {
                    response.sendRedirect("/api/auth/oauth2/failure");
                }

            } catch (Exception e) {
                response.sendRedirect("/api/auth/oauth2/failure");
            }
        } else {
            response.sendRedirect("/api/auth/oauth2/failure");
        }
    }

    /**
     * OAuth2 실패 콜백 (수정된 버전)
     */
    @GetMapping("/oauth2/failure")
    public void oAuth2LoginFailure(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String errorMessage = request.getParameter("error");
        
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("success", false);
        
        if (errorMessage != null && errorMessage.contains("You already have an account")) {
            errorData.put("error", Map.of(
                    "code", "ALREADY_REGISTERED",
                    "message", "You already have an account",
                    "details", "Please log in another way"
            ));
        } else {
            errorData.put("error", Map.of(
                    "code", "OAUTH2_LOGIN_FAILED",
                    "message", "소셜 로그인에 실패했습니다",
                    "details", "다시 시도해주세요"
            ));
        }

        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(new ObjectMapper().writeValueAsString(errorData));
    }

    /**
     * 현재 로그인된 사용자 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        
        if (token == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", Map.of(
                    "code", "MISSING_TOKEN",
                    "message", "인증 토큰이 필요합니다",
                    "details", "Authorization 헤더에 Bearer 토큰을 제공해주세요"
            ));
            return ResponseEntity.status(401).body(errorResponse);
        }

        try {
            if (jwtUtil.isTokenExpired(token)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", Map.of(
                        "code", "TOKEN_EXPIRED",
                        "message", "토큰이 만료되었습니다",
                        "details", "다시 로그인해주세요"
                ));
                return ResponseEntity.status(401).body(errorResponse);
            }

            String username = jwtUtil.extractUsername(token);
            Optional<User> userOptional = userRepository.findByUsername(username);
            
            if (userOptional.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", Map.of(
                        "code", "USER_NOT_FOUND",
                        "message", "사용자를 찾을 수 없습니다",
                        "details", "토큰에 해당하는 사용자가 존재하지 않습니다"
                ));
                return ResponseEntity.status(404).body(errorResponse);
            }

            User user = userOptional.get();
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "name", user.getName(),
                    "username", user.getUsername(),
                    "provider", user.getProviderType().toString(),
                    "profileImageUrl", user.getProfileImageUrl(),
                    "emailVerified", user.isEmailVerified()
            ));
            
            return ResponseEntity.ok(successResponse);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", Map.of(
                    "code", "INVALID_TOKEN",
                    "message", "유효하지 않은 토큰입니다",
                    "details", "토큰 검증에 실패했습니다"
            ));
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    /**
     * 토큰 검증 API
     */
    @PostMapping("/verify-token")
    public ResponseEntity<?> verifyToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        
        if (token == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("valid", false);
            errorResponse.put("error", "Missing token");
            return ResponseEntity.status(401).body(errorResponse);
        }

        try {
            boolean isValid = !jwtUtil.isTokenExpired(token);
            String username = jwtUtil.extractUsername(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("valid", isValid);
            response.put("username", username);
            response.put("expirationTime", jwtUtil.extractExpiration(token));
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("valid", false);
            errorResponse.put("error", "Invalid token");
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    /**
     * Request에서 Bearer 토큰 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}