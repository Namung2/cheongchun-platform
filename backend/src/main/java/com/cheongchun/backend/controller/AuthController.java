package com.cheongchun.backend.controller;

import com.cheongchun.backend.dto.LoginRequest;
import com.cheongchun.backend.dto.SignUpRequest;
import com.cheongchun.backend.entity.RefreshToken;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.repository.UserRepository;
import com.cheongchun.backend.service.AuthService;
import com.cheongchun.backend.service.RefreshTokenService;
import com.cheongchun.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;


@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, JwtUtil jwtUtil, UserRepository userRepository, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * 이메일 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest,
                                          HttpServletRequest request, HttpServletResponse response) {
        try {
            // 사용자 등록 및 토큰 생성
            User newUser = authService.createUser(signUpRequest);
            
            // JWT 토큰 생성
            String jwt = jwtUtil.generateTokenFromUsername(newUser.getEmail());
            
            // 리프레시 토큰 생성
            String userAgent = request.getHeader("User-Agent");
            String ipAddress = getClientIpAddress(request);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(newUser, userAgent, ipAddress);
            
            // 쿠키에 토큰 설정
            setAuthCookies(response, jwt, refreshToken.getToken());

            // 사용자 정보만 응답 (토큰은 쿠키로)
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("data", Map.of(
                    "user", Map.of(
                            "id", newUser.getId(),
                            "email", newUser.getEmail(),
                            "name", newUser.getName()
                    )
            ));
            responseData.put("message", "회원가입이 완료되었습니다");
            responseData.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(responseData);
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
                                              HttpServletRequest request, HttpServletResponse response) {
        try {
            // 사용자 인증
            User authenticatedUser = authService.authenticateUserCookie(loginRequest);
            
            // JWT 토큰 생성
            String jwt = jwtUtil.generateTokenFromUsername(authenticatedUser.getEmail());
            
            // 리프레시 토큰 생성
            String userAgent = request.getHeader("User-Agent");
            String ipAddress = getClientIpAddress(request);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(authenticatedUser, userAgent, ipAddress);
            
            // 쿠키에 토큰 설정
            setAuthCookies(response, jwt, refreshToken.getToken());

            // 사용자 정보만 응답 (토큰은 쿠키로)
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("data", Map.of(
                    "user", Map.of(
                            "id", authenticatedUser.getId(),
                            "email", authenticatedUser.getEmail(),
                            "name", authenticatedUser.getName()
                    )
            ));
            responseData.put("message", "로그인 성공");
            responseData.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(responseData);
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
     * 리프레시 토큰을 사용한 토큰 갱신
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 쿠키에서 리프레시 토큰 추출
            String refreshToken = null;
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie cookie : cookies) {
                    if ("refreshToken".equals(cookie.getName())) {
                        refreshToken = cookie.getValue();
                        break;
                    }
                }
            }

            if (refreshToken == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", Map.of(
                        "code", "REFRESH_TOKEN_MISSING",
                        "message", "리프레시 토큰이 필요합니다",
                        "details", "다시 로그인해주세요"
                ));
                errorResponse.put("timestamp", LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // 리프레시 토큰 검증 및 새 토큰 발급
            Optional<RefreshToken> refreshTokenOpt = refreshTokenService.findByToken(refreshToken);
            if (refreshTokenOpt.isEmpty()) {
                // 만료된 리프레시 토큰 쿠키 삭제
                clearAuthCookies(response);
                
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", Map.of(
                        "code", "REFRESH_TOKEN_EXPIRED",
                        "message", "리프레시 토큰이 만료되었습니다",
                        "details", "다시 로그인해주세요"
                ));
                errorResponse.put("timestamp", LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // 토큰 유효성 검증
            RefreshToken validRefreshToken;
            try {
                validRefreshToken = refreshTokenService.verifyExpiration(refreshTokenOpt.get());
            } catch (RuntimeException e) {
                // 만료된 리프레시 토큰 쿠키 삭제
                clearAuthCookies(response);
                
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", Map.of(
                        "code", "REFRESH_TOKEN_EXPIRED",
                        "message", "리프레시 토큰이 만료되었습니다",
                        "details", e.getMessage()
                ));
                errorResponse.put("timestamp", LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            User user = validRefreshToken.getUser();

            // 새 JWT 토큰 생성
            String newJwt = jwtUtil.generateTokenFromUsername(user.getEmail());

            // 새 리프레시 토큰 생성
            String userAgent = request.getHeader("User-Agent");
            String ipAddress = getClientIpAddress(request);
            RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user, userAgent, ipAddress);

            // 기존 리프레시 토큰 무효화
            refreshTokenService.revokeToken(refreshToken);

            // 새 토큰들을 쿠키로 설정
            setAuthCookies(response, newJwt, newRefreshToken.getToken());

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "토큰이 갱신되었습니다");
            successResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(successResponse);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", Map.of(
                    "code", "TOKEN_REFRESH_ERROR",
                    "message", "토큰 갱신 중 오류가 발생했습니다",
                    "details", e.getMessage()
            ));
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 로그아웃 (현재 디바이스만)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getName())) {

            try {
                User currentUser = (User) authentication.getPrincipal();
                
                // 쿠키에서 리프레시 토큰 추출
                String refreshToken = null;
                jakarta.servlet.http.Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (jakarta.servlet.http.Cookie cookie : cookies) {
                        if ("refreshToken".equals(cookie.getName())) {
                            refreshToken = cookie.getValue();
                            break;
                        }
                    }
                }

                authService.logout(currentUser, refreshToken);
                
                // 쿠키 삭제
                clearAuthCookies(response);

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", true);
                responseData.put("message", "로그아웃되었습니다");
                responseData.put("timestamp", LocalDateTime.now());

                return ResponseEntity.ok(responseData);
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

        Map<String, Object> logoutResponse = new HashMap<>();
        logoutResponse.put("success", true);
        logoutResponse.put("message", "로그아웃되었습니다");
        logoutResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(logoutResponse);
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

    /**
     * OAuth2 성공 콜백 처리
     * OAuth2LoginHandler에서 리다이렉트되는 엔드포인트
     */
    @GetMapping("/oauth-success")
    public ResponseEntity<String> handleOAuthSuccess(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String provider) {

        String html = String.format("""
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>로그인 성공</title>
                    <style>
                        body { 
                            font-family: 'Noto Sans KR', Arial, sans-serif; 
                            margin: 0; 
                            padding: 40px; 
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            min-height: 100vh;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                        }
                        .container { 
                            background: white; 
                            padding: 40px; 
                            border-radius: 10px; 
                            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
                            text-align: center;
                            max-width: 400px;
                            width: 100%%;
                        }
                        h1 { color: #4caf50; margin-bottom: 20px; }
                        .info { background: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0; }
                        button { 
                            background: #4285f4; 
                            color: white; 
                            border: none; 
                            padding: 15px 30px; 
                            border-radius: 5px; 
                            cursor: pointer; 
                            font-size: 16px;
                            margin: 10px;
                        }
                        button:hover { background: #357ae8; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>🎉 로그인 성공!</h1>
                        <p>청춘 애플리케이션에 성공적으로 로그인되었습니다.</p>
                        
                        <div class="info">
                            <p><strong>이름:</strong> %s</p>
                            <p><strong>이메일:</strong> %s</p>
                            <p><strong>사용자 ID:</strong> %s</p>
                            <p><strong>로그인 방식:</strong> %s</p>
                        </div>
                        
                        <button onclick="testAPI()">API 테스트</button>
                        <button onclick="goToMain()">메인으로 이동</button>
                        
                        <div id="result" style="margin-top: 20px;"></div>
                    </div>

                    <script>
                        // React Native WebView에 토큰 정보 전달
                        const userData = {
                            userId: '%s', 
                            email: '%s',
                            name: '%s',
                            provider: '%s'
                        };
                        
                        // WebView에 postMessage로 사용자 정보 전달 (React Native)
                        if (window.ReactNativeWebView) {
                            window.ReactNativeWebView.postMessage(JSON.stringify({
                                type: 'oauth_success',
                                data: userData
                            }));
                        } else {
                            // 웹 환경에서는 쿠키에 토큰이 이미 저장됨
                            document.querySelector('.container').innerHTML += 
                                '<div style="background: #e8f5e8; padding: 15px; border-radius: 5px; margin-top: 20px;">로그인이 완료되었습니다! 쿠키에 토큰이 안전하게 저장되었습니다.</div>';
                        }
                        
                        async function testAPI() {
                            try {
                                // 쿠키에서 자동으로 토큰을 읽어오므로 별도 헤더 설정 불필요
                                const response = await fetch('/auth/me', {
                                    credentials: 'include' // 쿠키 포함하여 요청
                                });
                                
                                const result = await response.json();
                                document.getElementById('result').innerHTML = 
                                    `<div style="background: #e8f5e8; padding: 10px; border-radius: 5px;">
                                        <strong>API 테스트 성공!</strong><br>
                                        <pre>${JSON.stringify(result, null, 2)}</pre>
                                    </div>`;
                            } catch (error) {
                                document.getElementById('result').innerHTML = 
                                    `<div style="background: #ffeaa7; padding: 10px; border-radius: 5px;">
                                        <strong>API 테스트 실패:</strong> ${error.message}
                                    </div>`;
                            }
                        }
                        
                        function goToMain() {
                            if (window.ReactNativeWebView) {
                                // React Native 앱 환경
                                window.ReactNativeWebView.postMessage(JSON.stringify({
                                    type: 'navigate_to_main'
                                }));
                            } else {
                                // 웹 브라우저 환경  
                                alert('로그인이 완료되었습니다! 메인 페이지로 이동합니다.');
                                window.location.href = '/';
                            }
                        }
                    </script>
                </body>
                </html>
                """, 
                name != null ? name : "N/A",
                email != null ? email : "N/A", 
                userId != null ? userId : "N/A",
                provider != null ? provider.toUpperCase() : "UNKNOWN",
                userId != null ? userId : "",
                email != null ? email : "",
                name != null ? name : "",
                provider != null ? provider : ""
        );

        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
    }

    /**
     * OAuth2 실패 콜백 처리
     */
    @GetMapping("/oauth-error")
    public ResponseEntity<String> handleOAuthError(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String message) {

        String html = String.format("""
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>로그인 실패</title>
                    <style>
                        body { 
                            font-family: 'Noto Sans KR', Arial, sans-serif; 
                            margin: 0; 
                            padding: 40px; 
                            background: linear-gradient(135deg, #ff7675 0%%, #d63031 100%%);
                            min-height: 100vh;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                        }
                        .container { 
                            background: white; 
                            padding: 40px; 
                            border-radius: 10px; 
                            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
                            text-align: center;
                            max-width: 400px;
                            width: 100%%;
                        }
                        h1 { color: #d63031; margin-bottom: 20px; }
                        .error { background: #ffe8e8; padding: 15px; border-radius: 5px; margin: 20px 0; }
                        button { 
                            background: #4285f4; 
                            color: white; 
                            border: none; 
                            padding: 15px 30px; 
                            border-radius: 5px; 
                            cursor: pointer; 
                            font-size: 16px;
                            margin: 10px;
                        }
                        button:hover { background: #357ae8; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>❌ 로그인 실패</h1>
                        <p>로그인 중 오류가 발생했습니다.</p>
                        
                        <div class="error">
                            <p><strong>오류 코드:</strong> %s</p>
                            <p><strong>오류 메시지:</strong> %s</p>
                        </div>
                        
                        <button onclick="retryLogin()">다시 시도</button>
                    </div>

                    <script>
                        function retryLogin() {
                            window.location.href = '/oauth2/authorization/google';
                        }
                    </script>
                </body>
                </html>
                """,
                code != null ? code : "UNKNOWN_ERROR",
                message != null ? message : "알 수 없는 오류가 발생했습니다."
        );

        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
    }

    /**
     * 쿠키에 인증 토큰들을 설정하는 헬퍼 메소드
     */
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

    /**
     * 인증 쿠키들을 삭제하는 헬퍼 메소드
     */
    private void clearAuthCookies(HttpServletResponse response) {
        // accessToken 쿠키 삭제
        jakarta.servlet.http.Cookie jwtCookie = new jakarta.servlet.http.Cookie("accessToken", "");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0); // 즉시 만료
        response.addCookie(jwtCookie);
        
        // refreshToken 쿠키 삭제
        jakarta.servlet.http.Cookie refreshCookie = new jakarta.servlet.http.Cookie("refreshToken", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0); // 즉시 만료
        response.addCookie(refreshCookie);
    }

    /**
     * 클라이언트 IP 주소를 추출하는 헬퍼 메소드
     */
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