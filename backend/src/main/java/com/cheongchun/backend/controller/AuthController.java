package com.cheongchun.backend.controller;

import com.cheongchun.backend.dto.JwtResponse;
import com.cheongchun.backend.dto.LoginRequest;
import com.cheongchun.backend.dto.SignUpRequest;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.repository.UserRepository;
import com.cheongchun.backend.service.AuthService;
import com.cheongchun.backend.service.RefreshTokenService;
import com.cheongchun.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/auth")  // /api는 context-path에서 처리되므로 제거
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, JwtUtil jwtUtil, UserRepository userRepository) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    /**
     * 이메일 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest,
                                          HttpServletRequest request) {
        try {
            JwtResponse jwtResponse = authService.registerUser(signUpRequest, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "user", Map.of(
                            "id", jwtResponse.getId(),
                            "username", jwtResponse.getUsername(),
                            "email", jwtResponse.getEmail(),
                            "name", jwtResponse.getName()
                    ),
                    "tokens", Map.of(
                            "accessToken", jwtResponse.getAccessToken(),
                            "refreshToken", jwtResponse.getRefreshToken(),
                            "tokenType", jwtResponse.getTokenType(),
                            "expiresIn", jwtResponse.getExpiresIn()
                    )
            ));
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

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                                              HttpServletRequest request) {
        try {
            JwtResponse jwtResponse = authService.authenticateUser(loginRequest, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "user", Map.of(
                            "id", jwtResponse.getId(),
                            "username", jwtResponse.getUsername(),
                            "email", jwtResponse.getEmail(),
                            "name", jwtResponse.getName()
                    ),
                    "tokens", Map.of(
                            "accessToken", jwtResponse.getAccessToken(),
                            "refreshToken", jwtResponse.getRefreshToken(),
                            "tokenType", jwtResponse.getTokenType(),
                            "expiresIn", jwtResponse.getExpiresIn()
                    )
            ));
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


            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    /**
     * 토큰 갱신
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request,
                                          HttpServletRequest httpRequest) {
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

        try {
            JwtResponse jwtResponse = authService.refreshToken(refreshToken, httpRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "user", Map.of(
                            "id", jwtResponse.getId(),
                            "username", jwtResponse.getUsername(),
                            "email", jwtResponse.getEmail(),
                            "name", jwtResponse.getName()
                    ),
                    "tokens", Map.of(
                            "accessToken", jwtResponse.getAccessToken(),
                            "refreshToken", jwtResponse.getRefreshToken(),
                            "tokenType", jwtResponse.getTokenType(),
                            "expiresIn", jwtResponse.getExpiresIn()
                    )
            ));
            response.put("message", "토큰이 성공적으로 갱신되었습니다");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", Map.of(
                    "code", "TOKEN_REFRESH_FAILED",
                    "message", e.getMessage(),
                    "details", "토큰 갱신에 실패했습니다. 다시 로그인해주세요"
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

            // User 객체에서 안전하게 데이터 추출
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", currentUser.getId());
            userData.put("username", currentUser.getUsername());
            userData.put("email", currentUser.getEmail());
            userData.put("name", currentUser.getName());
            userData.put("profileImageUrl", currentUser.getProfileImageUrl()); // null 가능
            userData.put("role", currentUser.getRole() != null ? currentUser.getRole().name() : "USER");
            userData.put("providerType", currentUser.getProviderType() != null ? currentUser.getProviderType().name() : "LOCAL");
            userData.put("emailVerified", currentUser.getEmailVerified() != null ? currentUser.getEmailVerified() : false);
            userData.put("createdAt", currentUser.getCreatedAt());
            userData.put("updatedAt", currentUser.getUpdatedAt());

            // authorities 안전하게 처리
            List<Map<String, String>> authorities = new ArrayList<>();
            if (currentUser.getAuthorities() != null) {
                for (var authority : currentUser.getAuthorities()) {
                    Map<String, String> auth = new HashMap<>();
                    auth.put("authority", authority.getAuthority());
                    authorities.add(auth);
                }
            }
            userData.put("authorities", authorities);

            userData.put("enabled", currentUser.isEnabled());
            userData.put("accountNonExpired", currentUser.isAccountNonExpired());
            userData.put("credentialsNonExpired", currentUser.isCredentialsNonExpired());
            userData.put("accountNonLocked", currentUser.isAccountNonLocked());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userData);
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
        } catch (Exception e) {
            // 추가된 예외 처리
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", Map.of(
                    "code", "INTERNAL_ERROR",
                    "message", "사용자 정보 조회 중 오류가 발생했습니다",
                    "details", e.getMessage() != null ? e.getMessage() : "Unknown error"
            ));
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    /**
     * 로그아웃 (현재 디바이스만)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) Map<String, String> request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getName())) {

            try {
                User currentUser = (User) authentication.getPrincipal();
                String refreshToken = null;

                if (request != null) {
                    refreshToken = request.get("refreshToken");
                }

                authService.logout(currentUser, refreshToken);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "로그아웃되었습니다");
                response.put("timestamp", LocalDateTime.now());

                return ResponseEntity.ok(response);
            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", Map.of(
                        "code", "LOGOUT_FAILED",
                        "message", "로그아웃 처리 중 오류가 발생했습니다",
                        "details", e.getMessage()
                ));
                errorResponse.put("timestamp", LocalDateTime.now());

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "로그아웃되었습니다");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * 모든 디바이스에서 로그아웃
     */
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutFromAllDevices() {
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
            authService.logoutFromAllDevices(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "모든 디바이스에서 로그아웃되었습니다");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", Map.of(
                    "code", "LOGOUT_ALL_FAILED",
                    "message", "전체 로그아웃 처리 중 오류가 발생했습니다",
                    "details", e.getMessage()
            ));
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    /**
     * 사용자 활성 세션 조회
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getUserSessions() {
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
            RefreshTokenService.RefreshTokenStats stats = authService.getUserTokenStats(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "activeTokens", stats.getActiveTokens(),
                    "totalTokens", stats.getTotalTokens(),
                    "maxAllowed", stats.getMaxAllowed(),
                    "isNearLimit", stats.isNearLimit()
            ));
            response.put("message", "세션 정보 조회 성공");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", Map.of(
                    "code", "SESSION_INFO_FAILED",
                    "message", "세션 정보 조회 중 오류가 발생했습니다",
                    "details", e.getMessage()
            ));
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
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