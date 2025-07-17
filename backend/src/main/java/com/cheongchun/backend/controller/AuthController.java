package com.cheongchun.backend.controller;

import com.cheongchun.backend.dto.JwtResponse;
import com.cheongchun.backend.dto.LoginRequest;
import com.cheongchun.backend.dto.SignUpRequest;
import com.cheongchun.backend.entity.User;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")  // /api는 context-path에서 처리되므로 제거
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
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

        // TODO: 리프레시 토큰 검증 및 새 토큰 발급 로직 구현
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
     * OAuth2 성공 콜백
     */
    @GetMapping("/oauth2/success")
    public void oAuth2LoginSuccess(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            String jwt = jwtUtil.generateTokenFromUsername(username);

            // 프론트엔드로 리다이렉트 (나중에 실제 URL로 변경)
            response.sendRedirect("http://localhost:3000/auth/success?token=" + jwt);
        } else {
            response.sendRedirect("http://localhost:3000/auth/failure");
        }
    }

    /**
     * OAuth2 실패 콜백
     */
    @GetMapping("/oauth2/failure")
    public void oAuth2LoginFailure(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect("http://localhost:3000/auth/failure");
    }

    /**
     * 테스트용 엔드포인트
     */
    @GetMapping("/test")
    public ResponseEntity<?> testAuth() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "인증 컨트롤러가 정상 작동합니다");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }
}